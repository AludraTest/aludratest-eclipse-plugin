package org.aludratest.eclipse.vde.internal.editors;

import org.aludratest.eclipse.vde.internal.TestDataModelDiff;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;

public class MissingFieldMetadata implements ITestDataFieldMetadata {

	private TestDataModelDiff diff;

	public MissingFieldMetadata(TestDataModelDiff diff) {
		this.diff = diff;
	}

	@Override
	public String getName() {
		return diff.getFieldName();
	}

	@Override
	public TestDataFieldType getType() {
		return diff.getExpectedFieldType();
	}

	@Override
	public String getSubTypeClassName() {
		return diff.getExpectedSubType();
	}

	@Override
	public String getFormatterPattern() {
		return null;
	}

	@Override
	public String getFormatterLocale() {
		return null;
	}

	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setType(TestDataFieldType fieldType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSubTypeClassName(String subTypeClassName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFormatterPattern(String pattern) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFormatterLocale(String locale) {
		throw new UnsupportedOperationException();
	}

}
