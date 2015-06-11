package org.aludratest.eclipse.vde.model;

public interface ITestDataFieldMetadata {

	public String getName();

	public TestDataFieldType getType();

	public String getSubTypeClassName();

	public String getFormatterPattern();

	public String getFormatterLocale();

	public void setName(String name);

	public void setType(TestDataFieldType fieldType);

	public void setSubTypeClassName(String subTypeClassName);

	public void setFormatterPattern(String pattern);

	public void setFormatterLocale(String locale);

}
