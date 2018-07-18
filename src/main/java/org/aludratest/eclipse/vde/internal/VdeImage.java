package org.aludratest.eclipse.vde.internal;

import org.eclipse.swt.graphics.Image;

public enum VdeImage {

	TESTDATA("icons/testdata.gif"), SEGMENT("icons/segment.gif"), FIELD("icons/field.gif"), ADD("icons/add.gif"), DELETE(
			"icons/delete.gif"), RENAME("icons/rename.gif"), FUNCTION("icons/function.gif"), LOCAL_VAR("icons/localvar.gif"), IGNORE(
			"icons/ignore.gif"), DUPLICATE("icons/duplicate.gif"), HTML_LINK("icons/htmllink.gif");

	private String resource;

	private VdeImage(String resource) {
		this.resource = resource;
	}

	public String getResource() {
		return resource;
	}

	public Image getImage() {
		return VdePlugin.getDefault().getImage(this);
	}

}
