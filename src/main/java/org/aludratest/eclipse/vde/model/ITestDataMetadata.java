package org.aludratest.eclipse.vde.model;

public interface ITestDataMetadata {

	public ITestDataSegmentMetadata[] getSegments();

	public void addSegment(String name, String dataClassName);

	public void removeSegment(ITestDataSegmentMetadata segment);

	public void moveSegment(ITestDataSegmentMetadata segment, int newIndex);

}
