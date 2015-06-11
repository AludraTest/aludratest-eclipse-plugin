package org.aludratest.eclipse.vde.model;

public interface ITestDataSegmentMetadata {

	public String getName();

	public String getDataClassName();

	public ITestDataFieldMetadata[] getFields();

	public void setName(String name);

	public void setDataClassName(String dataClassName);

	public void addField();

	public void removeField(ITestDataFieldMetadata field);

	public void moveField(ITestDataFieldMetadata field, int newIndex);

}
