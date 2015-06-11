package org.aludratest.eclipse.vde.internal.model;

import org.w3c.dom.Element;

public interface DOMOperation<T> {

	public boolean isEdit();

	public String getName();

	public T perform(Element element);

}
