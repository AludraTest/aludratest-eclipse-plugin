package org.aludratest.eclipse.vde.internal;

import org.aludratest.eclipse.vde.model.TestDataFieldType;

public class TestDataModelDiff {

	private String segmentName;

	private String fieldName;

	private String expectedSubType;

	private TestDataFieldType expectedFieldType;

	private DiffType diffType;

	public static enum DiffType {
		MISSING_IN_MODEL, MISSING_IN_CLASS, DIFFERS, CLASS_NOT_FOUND
	}

	public TestDataModelDiff(String segmentName, DiffType diffType) {
		this.segmentName = segmentName;
		this.diffType = diffType;
	}

	public TestDataModelDiff(String segmentName, String fieldName, TestDataFieldType expectedFieldType, String expectedSubType,
			DiffType diffType) {
		this(segmentName, diffType);
		this.fieldName = fieldName;
		this.expectedFieldType = expectedFieldType;
		this.expectedSubType = expectedSubType;
	}

	public String getSegmentName() {
		return segmentName;
	}

	public DiffType getDiffType() {
		return diffType;
	}

	public String getFieldName() {
		return fieldName;
	}

	public TestDataFieldType getExpectedFieldType() {
		return expectedFieldType;
	}

	public String getExpectedSubType() {
		return expectedSubType;
	}

}
