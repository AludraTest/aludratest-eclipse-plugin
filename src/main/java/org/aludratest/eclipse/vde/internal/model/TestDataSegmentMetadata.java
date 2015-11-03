package org.aludratest.eclipse.vde.internal.model;

import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.w3c.dom.Element;

public class TestDataSegmentMetadata extends AbstractModelNode implements ITestDataSegmentMetadata {

	public TestDataSegmentMetadata(AbstractModelNode parentNode, String wrapperElementName, int indexInParent) {
		super(parentNode, wrapperElementName, indexInParent);
	}

	@Override
	protected String getElementName() {
		return SEGMENT;
	}

	@Override
	public String getName() {
		return getAttributeValueOrNull(NAME);
	}

	@Override
	public String getDataClassName() {
		return getAttributeValueOrNull(DATA_CLASS_NAME);
	}

	@Override
	public ITestDataFieldMetadata[] getFields() {
		return getChildObjects(TestDataFieldMetadata.class, new ITestDataFieldMetadata[0], FIELDS);
	}

	@Override
	public void setName(String name) {
		String oldName = getName();
		if (oldName != null && oldName.equals(name)) {
			return;
		}

		// safety first: No double names
		ITestDataMetadata metadata = (ITestDataMetadata) getParentNode();
		for (ITestDataSegmentMetadata segment : metadata.getSegments()) {
			if (name.equals(segment.getName())) {
				throw new IllegalArgumentException("There already exists another segment with this name.");
			}
		}

		// update all sub-segments
		String prefix = oldName + ".";
		for (ITestDataSegmentMetadata segment : metadata.getSegments()) {
			String prevName = segment.getName();
			if (prevName.startsWith(prefix)) {
				segment.setName(name + "." + prevName.substring(prefix.length()));
			}
		}

		// update all associated configuration segments
		ITestData testData = (ITestData) getParentNode().getParentNode();

		for (ITestDataConfiguration config : testData.getConfigurations()) {
			for (ITestDataConfigurationSegment configSegment : config.getSegments()) {
				if (configSegment.getName().equals(oldName) && (configSegment instanceof TestDataConfigurationSegment)) {
					((TestDataConfigurationSegment) configSegment).setName(name);
				}
			}
		}

		setAttributeValue(NAME, name);
	}

	@Override
	public void setDataClassName(String dataClassName) {
		setAttributeValue(DATA_CLASS_NAME, dataClassName);
	}

	@Override
	public void removeField(ITestDataFieldMetadata field) {
		if (!(field instanceof AbstractModelNode)) {
			return;
		}
		removeChild(FIELDS, (AbstractModelNode) field);
	}

	@Override
	public void moveField(ITestDataFieldMetadata field, int newIndex) {
		if (!(field instanceof AbstractModelNode)) {
			return;
		}

		moveChild(FIELDS, ((AbstractModelNode) field), newIndex);
	}

	@Override
	public void addField() {
		DOMOperation<Void> op = new DOMOperation<Void>() {

			@Override
			public Void perform(Element element) {
				Element fields = getChildElement(element, FIELDS, true);
				format(TestDataFieldMetadata.create(fields, "newField"), true);
				return null;
			}

			@Override
			public boolean isEdit() {
				return true;
			}

			@Override
			public String getName() {
				return "Add field";
			}
		};

		performDOMOperation(op);
	}

	public static Element create(Element parent, String name, String dataClassName) {
		Element element = appendChildElement(parent, SEGMENT);
		element.setAttribute(NAME, name);
		element.setAttribute(DATA_CLASS_NAME, dataClassName);
		return element;
	}

}
