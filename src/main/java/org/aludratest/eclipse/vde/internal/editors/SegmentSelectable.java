package org.aludratest.eclipse.vde.internal.editors;

import org.aludratest.eclipse.vde.model.ITestData;

public interface SegmentSelectable {

	public ITestData getTestDataModel();

	public boolean selectSegment(String segmentName);

}
