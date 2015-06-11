package org.aludratest.eclipse.vde.internal.model;

import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.w3c.dom.Element;

public class TestDataFieldMetadata extends AbstractModelNode implements ITestDataFieldMetadata {

	public TestDataFieldMetadata(AbstractModelNode parentNode, String wrapperElementName, int indexInParent) {
		super(parentNode, wrapperElementName, indexInParent);
	}

	@Override
	protected String getElementName() {
		return FIELD;
	}

	@Override
	public String getName() {
		return getAttributeValueOrNull(NAME);
	}

	@Override
	public void setName(String name) {
		setAttributeValue(NAME, name);
	}

	@Override
	public TestDataFieldType getType() {
		String tp = getAttributeValueOrNull(FIELD_TYPE);
		if (tp == null || EMPTY_STRING.equals(tp)) {
			return TestDataFieldType.STRING;
		}

		try {
			return TestDataFieldType.valueOf(tp);
		}
		catch (IllegalArgumentException e) {
			return TestDataFieldType.STRING;
		}
	}

	@Override
	public void setType(TestDataFieldType fieldType) {
		setAttributeValue(FIELD_TYPE, fieldType.name());
		if (fieldType != TestDataFieldType.OBJECT_LIST && fieldType != TestDataFieldType.OBJECT) {
			setSubTypeClassName(null);
		}
	}

	@Override
	public String getSubTypeClassName() {
		return getAttributeValueOrNull(SUB_TYPE_CLASS_NAME);
	}

	@Override
	public void setSubTypeClassName(String subTypeClassName) {
		setAttributeValue(SUB_TYPE_CLASS_NAME, subTypeClassName);
	}

	@Override
	public String getFormatterPattern() {
		return getAttributeValueOrNull(FORMATTER_PATTERN);
	}

	@Override
	public void setFormatterPattern(String pattern) {
		setAttributeValue(FORMATTER_PATTERN, pattern);
	}

	@Override
	public String getFormatterLocale() {
		return getAttributeValueOrNull(FORMATTER_LOCALE);
	}

	@Override
	public void setFormatterLocale(String locale) {
		setAttributeValue(FORMATTER_LOCALE, locale);
	}

	public static Element create(Element parent, String name) {
		Element element = appendChildElement(parent, FIELD);
		element.setAttribute(NAME, name);
		return element;
	}

}
