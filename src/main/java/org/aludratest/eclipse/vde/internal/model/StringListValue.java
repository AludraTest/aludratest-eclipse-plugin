package org.aludratest.eclipse.vde.internal.model;

import org.aludratest.eclipse.vde.internal.util.ArrayUtil;
import org.aludratest.eclipse.vde.model.IStringListValue;
import org.aludratest.eclipse.vde.model.IStringValue;

public class StringListValue extends AbstractModelNode implements IStringListValue {

	public StringListValue(AbstractModelNode parentNode) {
		super(parentNode);
	}

	@Override
	protected String getElementName() {
		return STRING_VALUES;
	}

	@Override
	public int getValueType() {
		return TYPE_STRING_LIST;
	}

	@Override
	public IStringValue[] getValues() {
		return getParentNode().getChildObjects(StringValue.class, new IStringValue[0], getElementName());
	}

	@Override
	public void removeValue(IStringValue value) {
		if (value instanceof AbstractModelNode) {
			getParentNode().removeChild(getElementName(), (AbstractModelNode) value);
		}
	}

	@Override
	public void addValue(String value) {
		appendChildElement(VALUE);
		ArrayUtil.lastElement(getValues()).setValue(value);
		IStringValue[] values = getValues();
		values[values.length - 1].setValue(value);
	}

}