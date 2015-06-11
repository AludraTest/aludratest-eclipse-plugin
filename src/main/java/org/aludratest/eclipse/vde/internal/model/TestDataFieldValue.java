package org.aludratest.eclipse.vde.internal.model;

import org.aludratest.eclipse.vde.model.IFieldValue;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.w3c.dom.Element;

public class TestDataFieldValue extends AbstractModelNode implements ITestDataFieldValue {

	public TestDataFieldValue(AbstractModelNode parentNode, String wrapperElementName, int indexInParent) {
		super(parentNode, wrapperElementName, indexInParent);
	}

	@Override
	protected String getElementName() {
		return FIELD_VALUE;
	}

	@Override
	public String getFieldName() {
		return getAttributeValueOrNull(NAME);
	}

	private ITestDataFieldMetadata getMetadata() {
		return getMetadata(((ITestData) getParentNode().getParentNode().getParentNode()).getMetaData());
	}

	@Override
	public ITestDataFieldMetadata getMetadata(ITestDataMetadata metadata) {
		TestDataConfigurationSegment segment = (TestDataConfigurationSegment) getParentNode();
		if (segment == null) {
			return null;
		}

		// navigate up to TestData
		ITestDataSegmentMetadata segmentMetadata = null;

		for (ITestDataSegmentMetadata segMeta : metadata.getSegments()) {
			if (segMeta.getName().equals(segment.getName())) {
				segmentMetadata = segMeta;
				break;
			}
		}

		if (segmentMetadata == null) {
			return null;
		}

		String fieldName = getFieldName();

		for (ITestDataFieldMetadata fieldMeta : segmentMetadata.getFields()) {
			if (fieldMeta.getName().equals(fieldName)) {
				return fieldMeta;
			}
		}

		return null;
	}

	private int getFieldValueType() {
		final ITestDataFieldMetadata fieldMeta = getMetadata();
		
		if (fieldMeta == null) {
			// TODO not good; missing metadata
			return IFieldValue.TYPE_STRING;
		}

		int valueType;
		switch (fieldMeta.getType()) {
			case BOOLEAN:
			case DATE:
			case NUMBER:
			case STRING:
				valueType = IFieldValue.TYPE_STRING;
				break;
			case STRING_LIST:
				valueType = IFieldValue.TYPE_STRING_LIST;
				break;
			case OBJECT_LIST:
				valueType = -1;
				break;
			case OBJECT:
				valueType = -1;
				break;
			default:
				throw new IllegalArgumentException("Unknown field type: " + fieldMeta.getType());
		}
		
		return valueType;
	}

	@Override
	public IFieldValue getFieldValue() {
		final int valueType = getFieldValueType();

		DOMOperation<IFieldValue> op = new DOMOperation<IFieldValue>() {

			@Override
			public IFieldValue perform(Element element) {
				switch (valueType) {
					case IFieldValue.TYPE_STRING:
						getChildElement(element, VALUE, true);
						return new StringValue(TestDataFieldValue.this);
					case IFieldValue.TYPE_STRING_LIST:
						getChildElement(element, STRING_VALUES, true);
						return new StringListValue(TestDataFieldValue.this);
				}
				
				return null;
			}

			@Override
			public boolean isEdit() {
				return false;
			}

			@Override
			public String getName() {
				return null;
			}
		};

		return performDOMOperation(op);
	}

	@Override
	public boolean isScript() {
		return Boolean.TRUE.toString().equals(getAttributeValueOrNull(SCRIPT));
	}

	public void setScript(boolean script) {
		setAttributeValue(SCRIPT, Boolean.toString(script));
	}

	void setNotReferencedInMetadata(final boolean notReferencedInMetadata) {
		performDOMOperation(new DOMOperation<Void>() {
			@Override
			public Void perform(Element element) {
				element.setUserData("notReferencedInMetadata", Boolean.valueOf(notReferencedInMetadata), null);
				return null;
			}

			@Override
			public boolean isEdit() {
				// false because document is not modified
				return false;
			}

			@Override
			public String getName() {
				return null;
			}
		});
	}

	public boolean isNotReferencedInMetadata() {
		return performDOMOperation(TestDataConfigurationSegment.isNotReferencedInMetadataOp).booleanValue();
	}


}
