package org.aludratest.eclipse.vde.internal.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.XMLContentDescriber;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class TestDataContentDescriber extends XMLContentDescriber implements IExecutableExtension {

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		// call the basic XML describer to do basic recognition
		if (super.describe(contents, description) == INVALID) {
			return INVALID;
		}
		// super.describe will have consumed some chars, need to rewind
		contents.reset();
		// Check to see if we matched our criteria.
		return checkCriteria(new InputSource(contents));
	}

	@Override
	public int describe(Reader contents, IContentDescription description) throws IOException {
		// call the basic XML describer to do basic recognition
		if (super.describe(contents, description) == INVALID) {
			return INVALID;
		}
		// super.describe will have consumed some chars, need to rewind
		contents.reset();
		// Check to see if we matched our criteria.
		return checkCriteria(new InputSource(contents));
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
	}

	private int checkCriteria(InputSource contents) throws IOException {
		// quick and dirty check: Must have <testdata> root element.
		TestDataXmlQuickCheck quickCheck = new TestDataXmlQuickCheck();
		try {
			 SAXParserFactory spf = SAXParserFactory.newInstance();
			    spf.setNamespaceAware(true);
			    SAXParser saxParser = spf.newSAXParser();
			XMLReader reader = saxParser.getXMLReader();
			reader.setContentHandler(quickCheck);
			reader.parse(contents);
			return quickCheck.testData ? VALID : INDETERMINATE;
		}
		catch (SAXException e) {
			// could be intentional exception to save time
			return quickCheck.testData ? VALID : INDETERMINATE;
		}
		catch (ParserConfigurationException e) {
			return INDETERMINATE;
		}
	}

	private static class TestDataXmlQuickCheck extends DefaultHandler {

		private boolean testData;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if ("testdata".equals(localName) && "http://aludratest.org/testdata".equals(uri)) {
				testData = true;
			}
			else {
				throw new SAXException("Intentional parser abort");
			}
		}

	}
}
