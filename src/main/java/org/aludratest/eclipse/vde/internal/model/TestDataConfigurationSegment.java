package org.aludratest.eclipse.vde.internal.model;

import java.util.HashSet;
import java.util.Set;

import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.w3c.dom.Element;

public class TestDataConfigurationSegment extends AbstractModelNode implements ITestDataConfigurationSegment {

	public TestDataConfigurationSegment(AbstractModelNode parentNode, String wrapperElementName, int indexInParent) {
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

	// only for TestDataSegmentMetadata on rename there
	void setName(String name) {
		setAttributeValue(NAME, name);
	}

	@Override
	public ITestDataFieldValue[] getFieldValues() {
		return getChildObjects(TestDataFieldValue.class, new ITestDataFieldValue[0], FIELD_VALUES);
	}

	public void removeFieldValue(ITestDataFieldValue fieldValue) {
		if (fieldValue instanceof AbstractModelNode) {
			removeChild(FIELD_VALUES, (AbstractModelNode) fieldValue);
		}
	}

	public void syncToMetadata(final ITestDataSegmentMetadata segmentMetadata) {
		DOMOperation<Void> op = new DOMOperation<Void>() {
			@Override
			public Void perform(Element element) {
				Element fieldValues = getChildElement(element, FIELD_VALUES, true);
				
				// collect names of already existing fields
				Set<String> existingFields = new HashSet<String>();
				for (ITestDataFieldValue field : getFieldValues()) {
					existingFields.add(field.getFieldName());
				}
				
				// create fields not yet existing
				Set<String> segmentFields = new HashSet<String>();
				for (ITestDataFieldMetadata field : segmentMetadata.getFields()) {
					String newFieldName = field.getName();
					if (!existingFields.contains(newFieldName)) {
						// create new
						Element newField = appendChildElement(fieldValues, FIELD_VALUE);
						newField.setAttribute(NAME, newFieldName);
						format(newField, false);
					}
					segmentFields.add(newFieldName);
				}

				// do not remove existing fields not present in metadata; could be used for manual refactoring
				// but mark them as superfluous
				for (ITestDataFieldValue field : getFieldValues()) {
					if (field instanceof TestDataFieldValue) {
						((TestDataFieldValue) field).setNotReferencedInMetadata(!segmentFields.contains(field.getFieldName()));
					}
				}

				return null;
			}
			
			@Override
			public boolean isEdit() {
				return true;
			}
			
			@Override
			public String getName() {
				return "Sync configuration segment to meta data";
			}
		};
		
		performDOMOperation(op);
	}

	final static DOMOperation<Boolean> isNotReferencedInMetadataOp = new DOMOperation<Boolean>() {
		@Override
		public Boolean perform(Element element) {
			Object o = element.getUserData("notReferencedInMetadata");
			return o == null ? false : ((Boolean) o).booleanValue();
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

	public boolean isNotReferencedInMetadata(boolean includeChildren) {
		boolean self = performDOMOperation(isNotReferencedInMetadataOp);
		if (self || !includeChildren) {
			return self;
		}

		for (ITestDataFieldValue field : getFieldValues()) {
			if ((field instanceof TestDataFieldValue) && ((TestDataFieldValue) field).isNotReferencedInMetadata()) {
				return true;
			}
		}

		return false;
	}

	public static Element create(Element parent, String name) {
		Element element = appendChildElement(parent, SEGMENT);
		element.setAttribute(NAME, name);
		return format(element, false);

	}

}
