package org.aludratest.eclipse.vde.model;

public interface ITestDataConfiguration {

	public String getName();

	public ITestDataConfigurationSegment[] getSegments();

	public void setName(String name);

	public boolean isIgnored();

	public void setIgnored(boolean ignored);

}
