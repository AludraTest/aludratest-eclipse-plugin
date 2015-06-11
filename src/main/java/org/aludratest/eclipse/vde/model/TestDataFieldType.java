package org.aludratest.eclipse.vde.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Locale;

public enum TestDataFieldType {

	STRING, DATE, NUMBER, BOOLEAN, OBJECT, OBJECT_LIST, STRING_LIST;

	protected Format constructFormatter(String pattern, Locale locale) {
		// a little bit ugly, but Java Format classes cannot be treated equally
		switch (this) {
			case DATE:
				return new SimpleDateFormat(pattern, locale);
			case NUMBER:
				return new DecimalFormat(pattern, new DecimalFormatSymbols(locale));
			default:
				return null;
		}
	}

	public String displayName() {
		// TODO implement NLS?

		StringBuilder sb = new StringBuilder();
		String nm = name();

		boolean nextCap = true;
		for (int i = 0; i < nm.length(); i++) {
			char ch = nm.charAt(i);
			if (ch == '_') {
				sb.append(" ");
				nextCap = true;
			}
			else if (nextCap) {
				sb.append(ch);
				nextCap = false;
			}
			else {
				sb.append(Character.toLowerCase(ch));
			}
		}

		return sb.toString();
	}

	public static String[] displayNames() {
		TestDataFieldType[] values = values();
		String[] result = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = values[i].displayName();
		}
		return result;
	}

}
