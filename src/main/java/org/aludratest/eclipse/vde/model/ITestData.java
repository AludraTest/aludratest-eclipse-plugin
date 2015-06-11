package org.aludratest.eclipse.vde.model;

/**
 * Interface of a test data model. An instance of this interface represents the contents of a .testdata.xml file.
 * 
 * @author falbrech
 * 
 */
public interface ITestData {

	public String getVersion();

	public ITestDataMetadata getMetaData();

	public ITestDataConfiguration[] getConfigurations();

	public void addConfiguration(String name);

	public void removeConfiguration(ITestDataConfiguration configuration);

}
