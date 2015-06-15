package org.aludratest.eclipse.vde.internal.editors;

import org.eclipse.nebula.widgets.nattable.edit.editor.AbstractCellEditor;
import org.eclipse.nebula.widgets.nattable.edit.editor.IEditErrorHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class GridFieldValueCellEditor extends AbstractCellEditor {

	private FieldValueCellEditor editorDelegate;

	private SegmentSelectable segmentSelectable;

	public GridFieldValueCellEditor(SegmentSelectable segmentSelectable) {
		this.segmentSelectable = segmentSelectable;
	}

	@Override
	public Object getEditorValue() {
		return editorDelegate.getValue();
	}

	@Override
	public void setEditorValue(Object value) {
		if (value instanceof GridEditorPage.CellInfo) {
			editorDelegate.setValue(((GridEditorPage.CellInfo) value).getFieldValue());
		}
	}

	@Override
	public Control getEditorControl() {
		return editorDelegate.getControl();
	}

	@Override
	public Control createEditorControl(Composite parent) {
		editorDelegate = new FieldValueCellEditor(segmentSelectable, parent);
		return editorDelegate.getControl();
	}

	@Override
	protected Control activateCell(Composite parent, Object originalCanonicalValue) {
		Control c = createEditorControl(parent);
		setEditorValue(originalCanonicalValue);
		editorDelegate.getControl().setVisible(true);
		editorDelegate.activate();
		editorDelegate.setFocus();
		return c;
	}

	@Override
	protected Object handleConversion(Object displayValue, IEditErrorHandler conversionErrorHandler) {
		return displayValue;
	}

	public void paste() {
		editorDelegate.performPaste();
	}

	public void copy() {
		editorDelegate.performCopy();
	}

}
