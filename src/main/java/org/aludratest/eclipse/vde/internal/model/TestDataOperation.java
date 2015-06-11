package org.aludratest.eclipse.vde.internal.model;

import org.aludratest.eclipse.vde.model.ITestData;

public interface TestDataOperation<T> {

	public boolean isEdit();

	public T perform(ITestData testData);

}
