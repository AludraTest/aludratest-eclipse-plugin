package org.aludratest.eclipse.vde.internal.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.databene.commons.Filter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.xml.core.internal.cleanup.ElementNodeCleanupHandler;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Base class for all Test Data model implementation classes in this package. This implementation uses JAXB annotations to marshal
 * and unmarshal to/from XML. Also, the corresponding IDOMElement node of the XML document is stored per model object. Changes in
 * the model are automatically reflected in the associated XML document.
 * 
 * @author falbrech
 * 
 */
@SuppressWarnings("restriction")
public abstract class AbstractModelNode implements XmlTagNames {

	// not set in root element
	private AbstractModelNode parentNode;

	// only set in root element
	protected DOMDocumentProvider documentProvider;

	// set for list elements
	private String wrapperElementName;

	// set for list elements
	private int indexInParent;

	protected AbstractModelNode(AbstractModelNode parentNode) {
		this.parentNode = parentNode;
	}

	protected AbstractModelNode(AbstractModelNode parentNode, String wrapperElementName, int indexInParent) {
		this.parentNode = parentNode;
		this.wrapperElementName = wrapperElementName;
		this.indexInParent = indexInParent;
	}

	public AbstractModelNode(DOMDocumentProvider documentProvider) {
		this.documentProvider = documentProvider;
	}

	protected abstract String getElementName();

	protected final String getAttributeValueOrNull(String attributeName) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForRead();

		IDOMElement element = getElement(document);
		if (element == null) {
			return null;
		}
		String attributeValue = (element != null && element.getAttributeNode(attributeName) == null) ? null : element
				.getAttribute(attributeName);
		documentProvider.releaseFromRead();
		return attributeValue;
	}

	protected final String getTextContentOrNull() {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForRead();

		IDOMElement element = getElement(document);
		String content = element.getTextContent();
		String result = "".equals(content) ? null : content;

		documentProvider.releaseFromRead();
		return result;
	}

	protected final void setAttributeValue(String attributeName, String attributeValue) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForEdit();

		IDOMElement element = getElement(document);
		String prevValue = element.getAttributeNode(attributeName) == null ? null : element.getAttribute(attributeName);
		if (attributeValue == null ? prevValue == null : attributeValue.equals(prevValue)) {
			documentProvider.releaseFromEdit();
			return;
		}

		document.getModel().beginRecording(this, "Modify Attribute");

		if (attributeValue == null) {
			element.removeAttribute(attributeName);
		}
		else {
			element.setAttribute(attributeName, attributeValue);
		}

		document.getModel().endRecording(this);
		documentProvider.releaseFromEdit();
	}

	protected final void setTextContent(String textContent) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForEdit();

		IDOMElement element = getElement(document);
		while (element.getChildNodes().getLength() > 0) {
			element.removeChild(element.getChildNodes().item(0));
		}
		element.appendChild(document.createTextNode(textContent));

		documentProvider.releaseFromEdit();
	}

	protected final void removeChild(String wrapperElementName, AbstractModelNode node) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForEdit();

		try {
			IDOMElement element = getElement(document);
			if (wrapperElementName != null && !"".equals(wrapperElementName)) {
				element = (IDOMElement) getChildElement(element, wrapperElementName, true);
			}

			document.getModel().beginRecording(this, "Remove element");
			removeChildElement(element, node.getElement(document));
			document.getModel().endRecording(this);
		}
		finally {
			documentProvider.releaseFromEdit();
		}
	}

	protected final void moveChild(String wrapperElementName, AbstractModelNode node, int indexTo) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForEdit();

		try {
			IDOMElement element = getElement(document);
			if (wrapperElementName != null && !"".equals(wrapperElementName)) {
				element = (IDOMElement) getChildElement(element, wrapperElementName, true);
			}
			document.getModel().beginRecording(this, "Move element");
			moveChildElement(element, node.getElement(document), indexTo);
			document.getModel().endRecording(this);
		}
		finally {
			documentProvider.releaseFromEdit();
		}
	}

	protected final <T> T performDOMOperation(DOMOperation<T> operation) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = operation.isEdit() ? documentProvider.getDOMDocumentForEdit()
				: documentProvider
				.getDOMDocumentForRead();

		Element element = getElement(document);
		if (operation.isEdit()) {
			document.getModel().beginRecording(this, operation.getName());
		}
		try {
			return operation.perform(element);
		}
		finally {
			if (operation.isEdit()) {
				document.getModel().endRecording(this);
				documentProvider.releaseFromEdit();
			}
			else {
				documentProvider.releaseFromRead();
			}
		}
	}

	public final IRegion getAttributeRegion(String attributeName) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForRead();

		try {
			IDOMElement element = getElement(document);
			if (element == null || element.getAttributeNode(attributeName) == null) {
				return null;
			}
			IDOMAttr node = (IDOMAttr) element.getAttributeNode(attributeName);
			return new Region(node.getStartOffset(), node.getLength());
		}
		finally {
			documentProvider.releaseFromRead();
		}
	}

	public final IRegion getAttributeValueRegion(String attributeName) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForRead();

		try {
			IDOMElement element = getElement(document);
			if (element == null || element.getAttributeNode(attributeName) == null) {
				return null;
			}
			IDOMAttr node = (IDOMAttr) element.getAttributeNode(attributeName);
			// offset returns offset to quotes, so increment by 1
			return new Region(node.getValueRegionStartOffset() + 1, node.getValue().length());
		}
		finally {
			documentProvider.releaseFromRead();
		}
	}

	public final IRegion getElementRegion() {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForRead();

		try {
			IDOMElement element = getElement(document);
			if (element == null) {
				return null;
			}
			return new Region(element.getStartOffset(), element.getLength());
		}
		finally {
			documentProvider.releaseFromRead();
		}

	}

	public final AbstractModelNode getParentNode() {
		return parentNode;
	}

	private DOMDocumentProvider getDocumentProvider() {
		if (documentProvider != null) {
			return documentProvider;
		}
		return parentNode.getDocumentProvider();
	}

	private IDOMElement getElement(IDOMDocument document) {
		if (documentProvider != null) {
			// root element
			return (IDOMElement) document.getDocumentElement();
		}

		Element parentElement = parentNode.getElement(document);
		if (parentElement == null) {
			// most probably, stale element
			return null;
		}

		if (wrapperElementName != null) {
			parentElement = getChildElement(parentElement, wrapperElementName, false);
			if (parentElement == null) {
				throw new IllegalStateException("Could not find child element " + wrapperElementName + " at location "
						+ parentNode.buildLocationString());
			}
		}

		List<Element> children = getChildElementsByTagName(parentElement, getElementName());
		if (children.size() <= indexInParent) {
			return null;
		}

		return (IDOMElement) children.get(indexInParent);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}

		if (obj.getClass() != getClass()) {
			return false;
		}

		final AbstractModelNode node = (AbstractModelNode) obj;

		DOMOperation<Boolean> op = new DOMOperation<Boolean>() {
			@Override
			public Boolean perform(Element element) {
				if (element == null) {
					return Boolean.FALSE;
				}
				Element element2 = node.getElement((IDOMDocument) element.getOwnerDocument());
				return Boolean.valueOf(element.equals(element2));
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public boolean isEdit() {
				return false;
			}
		};

		return performDOMOperation(op).booleanValue();
	}

	@Override
	public int hashCode() {
		return (parentNode == null ? 0 : parentNode.hashCode()) + indexInParent;
	}

	protected final <A, T extends AbstractModelNode> A[] getChildObjects(Class<T> objectClass, A[] emptyArray,
			String wrapperElementName) {
		return getChildObjects(objectClass, emptyArray, wrapperElementName, null);
	}

	protected final <A, T extends AbstractModelNode> A[] getChildObjects(Class<T> objectClass, A[] emptyArray,
			String wrapperElementName, Filter<Element> elementFilter) {
		// create a prototype to get element name
		Constructor<T> cstr;
		T prototype;
		try {
			cstr = objectClass.getConstructor(AbstractModelNode.class, String.class, int.class);
			prototype = cstr.newInstance(this, wrapperElementName, 0);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof IllegalStateException) {
				return emptyArray;
			}
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForRead();

		try {
			IDOMElement element = getElement(document);
			if (element == null) {
				return emptyArray;
			}
			if (wrapperElementName != null) {
				element = (IDOMElement) getChildElement(element, wrapperElementName, true);
			}

			String elementName = prototype.getElementName();
			List<Element> nl = getChildElementsByTagName(element, elementName);

			List<T> result = new ArrayList<T>(nl.size());
			for (int i = 0; i < nl.size(); i++) {
				try {
					if (elementFilter == null || elementFilter.accept(nl.get(i))) {
						result.add(cstr.newInstance(this, wrapperElementName, i));
					}
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			return result.toArray(emptyArray);
		}
		finally {
			documentProvider.releaseFromRead();
		}
	}

	protected final <T extends AbstractModelNode> T getChildObject(Class<T> objectClass, boolean required) {
		try {
			Constructor<T> cstr = objectClass.getConstructor(AbstractModelNode.class);
			return cstr.newInstance(this);
		}
		catch (IllegalStateException e) {
			// no element found
			return null;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected static final Element getChildElement(Element parent, String name, boolean create) {
		List<Element> nl = getChildElementsByTagName(parent, name);
		if (nl.isEmpty()) {
			if (create) {
				Element e = parent.getOwnerDocument().createElement(name);
				parent.appendChild(e);
				return format(e, false);
			}
			return null;
		}
		return nl.get(0);
	}

	protected static List<Element> getChildElementsByTagName(Node element, String tagName) {
		List<Element> result = new ArrayList<Element>();

		NodeList nl = element.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE && tagName.equals(n.getLocalName())) {
				result.add((Element) n);
			}
		}

		return result;
	}

	protected static final Element appendChildElement(Element parent, String elementName) {
		Element element = parent.getOwnerDocument().createElement(elementName);
		parent.appendChild(element);
		return element;
	}

	protected final int appendChildElement(String elementName, String... attributesAndValues) {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForEdit();
		try {
			Element element = appendChildElement(getElement(document), elementName);
			for (int i = 0; i < attributesAndValues.length - 1; i += 2) {
				String attrName = attributesAndValues[i];
				String value = attributesAndValues[i + 1];
				element.setAttribute(attrName, value);
			}

			return getIndexOfElementInParent(format(element, false));
		}
		finally {
			documentProvider.releaseFromEdit();
		}
	}

	protected static Element format(Element newNode, boolean closeEmptyTag) {
		// build an XPath in the form /elem/elem/elem/elemName[elemIndex] to retrieve the (possibly replaced) element after format
		String xpath = getElementXPath(newNode);

		if (newNode.getParentNode() != null && newNode.equals(newNode.getParentNode().getLastChild())) {
			// add a new line to get the newly generated content correctly formatted.
			newNode.getParentNode().appendChild(newNode.getParentNode().getOwnerDocument().createTextNode("\n")); //$NON-NLS-1$
		}

		Document doc = newNode.getOwnerDocument();

		FormatProcessorXML formatProcessor = new FormatProcessorXML();
		// ignore any line width settings, causes wrong formatting of <foo>bar</foo>
		formatProcessor.getFormatPreferences().setLineWidth(2000);
		formatProcessor.formatNode(newNode);

		// auto-close empty elements
		if (closeEmptyTag) {
			ElementNodeCleanupHandler cleanup = new ElementNodeCleanupHandler();
			cleanup.getCleanupPreferences().setCompressEmptyElementTags(true);
			cleanup.getCleanupPreferences().setFormatSource(false);
			cleanup.getCleanupPreferences().setConvertEOLCodes(false);
			cleanup.cleanup(newNode);
		}

		// now eval XPath on new XML to retrieve new node
		return (Element) getNodeByXPath(doc, xpath);
	}

	public String getElementXPath() {
		DOMDocumentProvider documentProvider = getDocumentProvider();
		IDOMDocument document = documentProvider.getDOMDocumentForRead();
		try {
			return getElementXPath(getElement(document));
		}
		finally {
			documentProvider.releaseFromRead();
		}
	}

	private static String getElementXPath(Element element) {
		StringBuilder xpath = new StringBuilder();

		// get index within parent, of elements with same name
		int indexInParent = getIndexOfElementInParent(element);
		xpath.append(element.getLocalName()).append("[").append(indexInParent + 1).append("]");
		Node n = element.getParentNode();
		while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				indexInParent = getIndexOfElementInParent((Element) n);
				if (indexInParent > -1) {
					xpath.insert(0, n.getLocalName() + "[" + (indexInParent + 1) + "]/");
				}
				else {
					// root node
					xpath.insert(0, n.getLocalName() + "/");
				}
			}
			n = n.getParentNode();
		}

		return xpath.toString();
	}

	/**
	 * Clean removal of child element; updates adjacent text nodes for clean XML code.
	 * 
	 * @param parent
	 *            Parent element containing child.
	 * @param child
	 *            Child element to remove from parent.
	 */
	protected static void removeChildElement(Element parent, Element child) {
		// Code taken from M2E
		if (child != null) {
			Node prev = child.getPreviousSibling();
			if (prev instanceof Text) {
				Text txt = (Text) prev;
				int lastnewline = getLastEolIndex(txt.getData());
				if (lastnewline >= 0) {
					txt.setData(txt.getData().substring(0, lastnewline));
				}
			}
			parent.removeChild(child);
		}
	}

	private static void moveChildElement(Element parent, Element moveChild, int newIndex) {
		List<Node> nodesToMove = new ArrayList<Node>();

		nodesToMove.add(moveChild);
		Node n = moveChild.getNextSibling();
		while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
			nodesToMove.add(n);
			n = n.getNextSibling();
		}

		List<Element> nl = getChildElementsByTagName(parent, moveChild.getLocalName());
		if (newIndex >= nl.size()) {
			for (Node node : nodesToMove) {
				parent.appendChild(node);
			}
			return;
		}

		Node refNode = nl.get(newIndex);
		for (Node node : nodesToMove) {
			parent.insertBefore(node, refNode);
		}
		format((Element) refNode, false);
	}

	private static int getLastEolIndex(String s) {
		if (s == null || s.length() == 0) {
			return -1;
		}
		for (int i = s.length() - 1; i >= 0; i--) {
			char c = s.charAt(i);
			if (c == '\r') {
				return i;
			}
			if (c == '\n') {
				if (i > 0 && s.charAt(i - 1) == '\r') {
					return i - 1;
				}
				return i;
			}
		}
		return -1;
	}

	private static Node getNodeByXPath(Document doc, String xpath) {
		XPathFactory xPathfactory = XPathFactory.newInstance();
		xpath = xpath.replaceAll("(^|/)([a-z])", "$1at:$2");
		XPath xp = xPathfactory.newXPath();
		xp.setNamespaceContext(new AludraTestNamespaceContext());
		try {
			XPathExpression expr = xp.compile(xpath);
			return (Node) expr.evaluate(doc, XPathConstants.NODE);
		}
		catch (XPathException e) {
			return null;
		}
	}

	protected static int getIndexOfElementInParent(Element e) {
		if (e.getParentNode() == null) {
			return -1;
		}

		String elemName = e.getLocalName();
		int indexInParent = 0;
		List<Element> nl = getChildElementsByTagName(e.getParentNode(), elemName);

		for (int i = 0; i < nl.size() && nl.get(i) != e; i++) {
			indexInParent++;
		}
		return indexInParent;
	}

	protected final String buildLocationString() {
		if (parentNode == null) {
			return "root";
		}

		return internalBuildLocationString();
	}

	private String internalBuildLocationString() {
		if (parentNode == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(parentNode.internalBuildLocationString());
		sb.append("/");
		sb.append(getElementName());
		return sb.toString();
	}

	private static class AludraTestNamespaceContext implements NamespaceContext {

		@Override
		public String getNamespaceURI(String prefix) {
			if ("at".equals(prefix)) {
				return "http://aludratest.org/testdata";
			}

			return XMLConstants.NULL_NS_URI;
		}

		@Override
		public String getPrefix(String namespaceURI) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Iterator getPrefixes(String namespaceURI) {
			throw new UnsupportedOperationException();
		}

	}


}
