package org.aludratest.eclipse.vde.internal.util;

public final class ArrayUtil {

	private ArrayUtil() {
	}

	public static <T> T lastElement(T[] arr) {
		return arr[arr.length - 1];
	}

}
