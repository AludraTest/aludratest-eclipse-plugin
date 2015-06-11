package org.aludratest.eclipse.vde.internal.editors;

public final class SegmentReference {

	private String segmentName;

	public SegmentReference(String segmentName) {
		this.segmentName = segmentName;
	}

	public String getSegmentName() {
		return segmentName;
	}

	@Override
	public String toString() {
		return "(Segment " + segmentName + ")";
	}

}
