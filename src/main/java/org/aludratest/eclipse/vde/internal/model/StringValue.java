package org.aludratest.eclipse.vde.internal.model;

import org.aludratest.eclipse.vde.model.IStringValue;

public class StringValue extends AbstractModelNode implements IStringValue {

	public StringValue(AbstractModelNode parentNode) {
		super(parentNode);
	}

	public StringValue(AbstractModelNode parentNode, String wrapperElementName, int indexInParent) {
		super(parentNode, wrapperElementName, indexInParent);
	}

	@Override
	public int getValueType() {
		return TYPE_STRING;
	}

	@Override
	protected String getElementName() {
		return VALUE;
	}

	@Override
	public String getValue() {
		return getTextContentOrNull();
	}

	@Override
	public void setValue(String value) {
		setTextContent(value);
	}

}
