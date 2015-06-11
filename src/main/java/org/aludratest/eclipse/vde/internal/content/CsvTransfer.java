package org.aludratest.eclipse.vde.internal.content;

import java.io.UnsupportedEncodingException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class CsvTransfer extends ByteArrayTransfer {

	private static final String CSV = "CSV";

	private static final int CSV_ID = registerType(CSV);

	public static CsvTransfer instance = new CsvTransfer();

	private CsvTransfer() {
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { CSV_ID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { CSV };
	}

	@Override
	protected void javaToNative(Object object, TransferData transferData) {
		if (object instanceof String) {
			try {
				object = object.toString().getBytes("UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		super.javaToNative(object, transferData);
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
		Object barr = super.nativeToJava(transferData);
		if (barr instanceof byte[]) {
			try {
				return new String((byte[]) barr, "UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return barr;
	}

}
