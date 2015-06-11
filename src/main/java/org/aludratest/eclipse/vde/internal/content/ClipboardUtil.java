package org.aludratest.eclipse.vde.internal.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aludratest.eclipse.vde.model.IFieldValue;
import org.aludratest.eclipse.vde.model.IStringValue;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.databene.commons.Converter;
import org.databene.formats.DataContainer;
import org.databene.formats.csv.CSVLineIterator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Rectangle;

public class ClipboardUtil {

	public static void copyToClipboard(Clipboard clipboard, Collection<ILayerCell> selectedCells,
			Converter<ILayerCell, ITestDataFieldValue> cellValueExtractor) {

		ClipboardFormat cf = new ClipboardFormat();
		
		Rectangle rc = getBoundingRect(selectedCells);
		if (rc == null) {
			return;
		}

		// calculate region
		for (ILayerCell cell : selectedCells) {
			ClipboardFormatCell cfCell = new ClipboardFormatCell();
			cfCell.columnIndex = cell.getColumnIndex();
			cfCell.rowIndex = cell.getRowIndex();
			
			ITestDataFieldValue fv = cellValueExtractor.convert(cell);
			IFieldValue value = fv.getFieldValue();
			if (value != null && value.getValueType() == IFieldValue.TYPE_STRING) {
				cfCell.contents = ((IStringValue) value).getValue();
				cfCell.script = fv.isScript();
			}

			cf.cellContents.add(cfCell);
		}

		cf.numColumns = rc.width;
		cf.numRows = rc.height;
		
		// offset coordinates to match relative region
		for (ClipboardFormatCell cell : cf.cellContents) {
			cell.columnIndex -= rc.x;
			cell.rowIndex -= rc.y;
		}
		
		clipboard.clearContents();
		clipboard.setContents(new Object[] { cf.toByteArray(), cf.toCsv() }, new Transfer[] { FieldValueTransfer.instance,
				CsvTransfer.instance });
	}

	private static TransferData getSupportedTransferData(Clipboard clipboard, Transfer transfer) {
		TransferData[] datas = clipboard.getAvailableTypes();
		for (TransferData td : datas) {
			if (transfer.isSupportedType(td)) {
				return td;
			}
		}

		return null;
	}

	public static boolean canPasteFromClipboard(Clipboard clipboard) {
		// Clipboard must contain one of these formats:
		// - our very own
		// - CSV
		// - CF_TEXT
		
		return getSupportedTransferData(clipboard, FieldValueTransfer.instance) != null
				|| getSupportedTransferData(clipboard, CsvTransfer.instance) != null
				|| getSupportedTransferData(clipboard, TextTransfer.getInstance()) != null;
	}

	public static void pasteFromClipboard(Clipboard clipboard, SelectionLayer selectionLayer,
			PasteStringValueAcceptor valueAcceptor) throws ClipboardRegionDoesNotMatchException {
		Collection<ILayerCell> selection = selectionLayer.getSelectedCells();
		if (selection.isEmpty() || !canPasteFromClipboard(clipboard)) {
			return;
		}
		
		ClipboardFormat cf = null;

		Object o = clipboard.getContents(FieldValueTransfer.instance);
		if (o instanceof byte[]) {
			try {
				cf = ClipboardFormat.fromByteArray((byte[]) o);
			}
			catch (IOException e) {
				// ignore; cancel paste
				return;
			}
		}

		// try CSV now
		if (cf == null) {
			o = clipboard.getContents(CsvTransfer.instance);
			if (o instanceof String) {
				try {
					cf = ClipboardFormat.fromCsv(o.toString());
				}
				catch (IOException e) {
					// ignore; cancel paste
					return;
				}
			}
		}

		// try text now
		if (cf == null) {
			o = clipboard.getContents(TextTransfer.getInstance());
			if (o instanceof String) {
				cf = ClipboardFormat.fromSingleString(o.toString());
			}
			else {
				// no format matches :-(
				return;
			}
		}
		
		if (selection.size() == 1) {
			// simple case: only one cell selected
			ILayerCell cell = selection.iterator().next();
			int firstColumn = cell.getColumnIndex();
			int firstRow = cell.getRowIndex();

			for (ClipboardFormatCell cfCell : cf.cellContents) {
				valueAcceptor
						.accept(firstColumn + cfCell.columnIndex, firstRow + cfCell.rowIndex, cfCell.contents, cfCell.script);
			}
		}
		else if (cf.cellContents.size() == 1) {
			// second simple case: Clipboard contains only one value; copy to all selected cells
			ClipboardFormatCell cfCell = cf.cellContents.get(0);
			for (ILayerCell cell : selection) {
				valueAcceptor.accept(cell.getColumnIndex(), cell.getRowIndex(), cfCell.contents, cfCell.script);
			}
		}
		else if (regionMatches(selection, cf)) {
			Rectangle rc = getBoundingRect(selection);
			
			for (ClipboardFormatCell cfCell : cf.cellContents) {
				valueAcceptor.accept(cfCell.columnIndex + rc.x, cfCell.rowIndex + rc.y, cfCell.contents, cfCell.script);
			}
		}
		else {
			if (cf.isOneRow()) {
				// Complex case #1: All clipboard content is in one row; selection matches this row * N
				List<Integer> rowIndices = new ArrayList<Integer>();
				if (isRowMultiple(selection, cf, rowIndices)) {
					Rectangle rc = getBoundingRect(selection);
					for (Integer row : rowIndices) {
						for (ClipboardFormatCell cell : cf.cellContents) {
							valueAcceptor.accept(rc.x + cell.columnIndex, row.intValue(), cell.contents, cell.script);
						}
					}
				}
				else {
					throw new ClipboardRegionDoesNotMatchException("The clipboard contents do not fit into the selection.");
				}
			}
			else if (cf.isOneColumn()) {
				// Complex case #2: All clipboard content is in one column; selection matches this column * N
				List<Integer> columnIndices = new ArrayList<Integer>();
				if (isColumnMultiple(selection, cf, columnIndices)) {
					Rectangle rc = getBoundingRect(selection);
					for (Integer col : columnIndices) {
						for (ClipboardFormatCell cell : cf.cellContents) {
							valueAcceptor.accept(col.intValue(), rc.y + cell.rowIndex, cell.contents, cell.script);
						}
					}
				}
				else {
					throw new ClipboardRegionDoesNotMatchException("The clipboard contents do not fit into the selection.");
				}
			}
			else {
				throw new ClipboardRegionDoesNotMatchException("The clipboard contents do not fit into the selection.");
			}
		}

	}

	private static boolean regionMatches(Collection<ILayerCell> selection, ClipboardFormat cf) {
		Rectangle rc = getBoundingRect(selection);

		for (ILayerCell cell : selection) {
			int col = cell.getColumnIndex() - rc.x;
			int row = cell.getRowIndex() - rc.y;

			boolean found = false;
			for (ClipboardFormatCell cfCell : cf.cellContents) {
				if (cfCell.columnIndex == col && cfCell.rowIndex == row) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}

		return true;
	}

	private static boolean isRowMultiple(Collection<ILayerCell> selection, ClipboardFormat cf, List<Integer> matchingRows) {
		Rectangle rc = getBoundingRect(selection);

		// collect column indices
		List<Integer> columnIndices = new ArrayList<Integer>();

		for (ClipboardFormatCell cfCell : cf.cellContents) {
			columnIndices.add(Integer.valueOf(cfCell.columnIndex));

		}

		// for each row contained in selection, row must contain all column indices and not contain more than these
		for (int r = rc.y; r < rc.y + rc.height; r++) {
			List<Integer> foundColumns = new ArrayList<Integer>();
			for (ILayerCell cell : selection) {
				if (cell.getRowIndex() == r) {
					foundColumns.add(Integer.valueOf(cell.getColumnIndex() - rc.x));
				}
			}

			if (!foundColumns.isEmpty()) {
				if (foundColumns.size() != columnIndices.size()) {
					return false;
				}

				foundColumns.removeAll(columnIndices);
				if (!foundColumns.isEmpty()) {
					return false;
				}

				matchingRows.add(Integer.valueOf(r));
			}
		}

		return true;
	}

	private static boolean isColumnMultiple(Collection<ILayerCell> selection, ClipboardFormat cf, List<Integer> matchingColumns) {
		Rectangle rc = getBoundingRect(selection);

		// collect column indices
		List<Integer> rowIndices = new ArrayList<Integer>();

		for (ClipboardFormatCell cfCell : cf.cellContents) {
			rowIndices.add(Integer.valueOf(cfCell.columnIndex));

		}

		// for each column contained in selection, column must contain all row indices and not contain more than these
		for (int c = rc.x; c < rc.x + rc.width; c++) {
			List<Integer> foundRows = new ArrayList<Integer>();
			for (ILayerCell cell : selection) {
				if (cell.getColumnIndex() == c) {
					foundRows.add(Integer.valueOf(cell.getRowIndex() - rc.y));
				}
			}

			if (!foundRows.isEmpty()) {
				if (foundRows.size() != rowIndices.size()) {
					return false;
				}

				foundRows.removeAll(rowIndices);
				if (!foundRows.isEmpty()) {
					return false;
				}

				matchingColumns.add(Integer.valueOf(c));
			}
		}

		return true;
	}

	private static Rectangle getBoundingRect(Collection<ILayerCell> selection) {
		if (selection.isEmpty()) {
			return null;
		}

		Integer minColumn = null, minRow = null, maxColumn = null, maxRow = null;

		for (ILayerCell cell : selection) {
			if (minColumn == null || cell.getColumnIndex() < minColumn.intValue()) {
				minColumn = Integer.valueOf(cell.getColumnIndex());
			}
			if (maxColumn == null || cell.getColumnIndex() > maxColumn.intValue()) {
				maxColumn = Integer.valueOf(cell.getColumnIndex());
			}
			if (minRow == null || cell.getRowIndex() < minRow.intValue()) {
				minRow = Integer.valueOf(cell.getRowIndex());
			}
			if (maxRow == null || cell.getRowIndex() > maxRow.intValue()) {
				maxRow = Integer.valueOf(cell.getRowIndex());
			}
		}

		return new Rectangle(minColumn, minRow, maxColumn - minColumn + 1, maxRow - minRow + 1);
	}

	private static class ClipboardFormat implements Serializable {

		private static final long serialVersionUID = -4325806603332668335L;

		private int numColumns;

		private int numRows;

		private List<ClipboardFormatCell> cellContents = new ArrayList<ClipboardFormatCell>();

		private byte[] toByteArray() {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(this);
				oos.flush();
				return baos.toByteArray();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		// converts the contents to an Excel-compatible CSV format
		private String toCsv() {
			StringBuilder sb = new StringBuilder();
			for (int r = 0; r < numRows; r++) {
				for (int c = 0; c < numColumns; c++) {
					if (c > 0) {
						sb.append(";");
					}
					String value = getCsvValue(c, r);
					if (value != null) {
						boolean withQuotes = value.contains("\"") || value.contains(";") || value.contains("\n")
								|| value.contains("\r");
						if (withQuotes) {
							sb.append("\"");
						}
						sb.append(value.replace("\"", "\"\""));
						if (withQuotes) {
							sb.append("\"");
						}
					}
				}
				sb.append("\r\n");
			}

			return sb.toString();
		}

		private String getCsvValue(int columnIndex, int rowIndex) {
			for (ClipboardFormatCell cell : cellContents) {
				if (cell.columnIndex == columnIndex && cell.rowIndex == rowIndex) {
					return cell.contents;
				}
			}
			return null;
		}

		private boolean isOneRow() {
			for (ClipboardFormatCell cell : cellContents) {
				if (cell.rowIndex > 0) {
					return false;
				}
			}
			return true;
		}

		private boolean isOneColumn() {
			for (ClipboardFormatCell cell : cellContents) {
				if (cell.columnIndex > 0) {
					return false;
				}
			}
			return true;
		}

		private static ClipboardFormat fromByteArray(byte[] data) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			try {
				ObjectInputStream ois = new ObjectInputStream(bais);
				return (ClipboardFormat) ois.readObject();
			}
			catch (ClassCastException e) {
				throw new IOException("Unexpected object from input data", e);
			}
			catch (ClassNotFoundException e) {
				throw new IOException("Unexpected object from input data", e);
			}
		}

		private static ClipboardFormat fromCsv(String csvData) throws IOException {
			CSVLineIterator iter = new CSVLineIterator(new StringReader(csvData), ';');
			DataContainer<String[]> data = new DataContainer<String[]>();

			ClipboardFormat cf = new ClipboardFormat();
			cf.numColumns = 0;
			cf.numRows = 0;

			while ((data = iter.next(data)) != null) {
				String[] values = data.getData();
				cf.numColumns = Math.max(cf.numColumns, values.length);

				for (int c = 0; c < values.length; c++) {
					ClipboardFormatCell cell = new ClipboardFormatCell();
					cell.columnIndex = c;
					cell.rowIndex = cf.numRows;
					cell.contents = values[c];
					cf.cellContents.add(cell);
				}

				cf.numRows++;
			}

			return cf;
		}

		private static ClipboardFormat fromSingleString(String text) {
			ClipboardFormat cf = new ClipboardFormat();
			cf.numColumns = 1;
			cf.numRows = 1;
			ClipboardFormatCell cell = new ClipboardFormatCell();
			cell.columnIndex = 0;
			cell.rowIndex = 0;
			cell.contents = text;
			cf.cellContents.add(cell);
			return cf;
		}
	}

	private static class ClipboardFormatCell implements Serializable {

		private static final long serialVersionUID = 568625115059923111L;

		private int columnIndex;

		private int rowIndex;

		private boolean script;

		private String contents;

	}

}
