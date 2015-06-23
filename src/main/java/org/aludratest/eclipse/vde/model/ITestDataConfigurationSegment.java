package org.aludratest.eclipse.vde.model;

public interface ITestDataConfigurationSegment {

	public String getName();

	public ITestDataFieldValue getFieldValue(String fieldName, boolean create);

	public ITestDataFieldValue[] getDefinedFieldValues();

}
