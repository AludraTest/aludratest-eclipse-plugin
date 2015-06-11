package org.aludratest.eclipse.vde.internal.model;

import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;

@SuppressWarnings("restriction")
public interface DOMDocumentProvider {

	public IDOMDocument getDOMDocumentForRead();

	public IDOMDocument getDOMDocumentForEdit();

	public void releaseFromRead();

	public void releaseFromEdit();

}
