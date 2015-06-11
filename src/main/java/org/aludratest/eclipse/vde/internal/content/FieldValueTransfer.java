package org.aludratest.eclipse.vde.internal.content;

import org.eclipse.swt.dnd.ByteArrayTransfer;

public class FieldValueTransfer extends ByteArrayTransfer {

	private static final String TYPE_NAME = FieldValueTransfer.class.getName();

	private static final int TYPE_ID = registerType(TYPE_NAME);

	public static FieldValueTransfer instance = new FieldValueTransfer();

	private FieldValueTransfer() {
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { TYPE_ID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

}
