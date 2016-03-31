package org.aludratest.eclipse.vde.model;

public interface ITestDataConfiguration {

	public String getName();

	public ITestDataConfigurationSegment[] getSegments();

	public ITestDataConfigurationSegment getSegment(String name);

	public void setName(String name);

	public boolean isIgnored();

	public void setIgnored(boolean ignored);

	public String getIgnoredReason();

	public void setIgnoredReason(String reason);

}
