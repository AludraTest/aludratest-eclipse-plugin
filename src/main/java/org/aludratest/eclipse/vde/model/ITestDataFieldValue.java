package org.aludratest.eclipse.vde.model;

public interface ITestDataFieldValue {

	public String getFieldName();

	public IFieldValue getFieldValue();

	public boolean isScript();

	public ITestDataFieldMetadata getMetadata(ITestDataMetadata metadata);

	public void setScript(boolean script);

}
