package org.aludratest.eclipse.vde.model;


public interface IStringListValue extends IFieldValue {

	public IStringValue[] getValues();

	public void addValue(String value);

	public void removeValue(IStringValue value);

}
