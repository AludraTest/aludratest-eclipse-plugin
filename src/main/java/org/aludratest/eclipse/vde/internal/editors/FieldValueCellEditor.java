package org.aludratest.eclipse.vde.internal.editors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aludratest.eclipse.vde.internal.util.ArrayUtil;
import org.aludratest.eclipse.vde.model.IFieldValue;
import org.aludratest.eclipse.vde.model.IStringListValue;
import org.aludratest.eclipse.vde.model.IStringValue;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

public class FieldValueCellEditor extends DialogCellEditor {

	private SegmentSelectable segmentSelector;

	private Text txtValue;

	private ITestDataFieldValue fieldValue;

	private String valueToSet;

	private Color yellowColor;

	public FieldValueCellEditor(SegmentSelectable segmentSelector, Composite parent) {
		super(parent);
		this.segmentSelector = segmentSelector;
		yellowColor = new Color(parent.getDisplay(), 255, 255, 219);
	}

	@Override
	public void dispose() {
		yellowColor.dispose();
		super.dispose();
	}

	@Override
	protected Control createContents(Composite cell) {
		txtValue = new Text(cell, SWT.LEFT);
		txtValue.setFont(cell.getFont());
		txtValue.setBackground(cell.getBackground());
		txtValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtValue.getEditable()) {
					valueToSet = txtValue.getText();
				}
			}
		});
		txtValue.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 13) {
					applyValueAndDeactivate();
				}
			}
		});
		return txtValue;
	}

	@Override
	protected void updateContents(Object value) {
		txtValue.setEditable(false);
		txtValue.setBackground(null);
		valueToSet = null;
		if (value instanceof ITestDataFieldValue) {
			fieldValue = (ITestDataFieldValue) value;

			IFieldValue val = fieldValue.getFieldValue();
			if (val.getValueType() == IFieldValue.TYPE_STRING) {
				if (!fieldValue.isScript()) {
					txtValue.setEditable(true);
				}
				else {
					txtValue.setBackground(yellowColor);
				}
				String v = ((IStringValue) val).getValue();
				txtValue.setText(v == null ? "" : v);
			}
		}
		else {
			txtValue.setText(value == null ? "" : value.toString());
			fieldValue = null;
		}
	}

	@Override
	protected void doSetFocus() {
		if (txtValue.getEditable()) {
			txtValue.selectAll();
			txtValue.setFocus();
		}
		else {
			super.doSetFocus();
		}
		txtValue.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				FieldValueCellEditor.this.focusLost();
			}
		});
	}

	@Override
	protected void focusLost() {
		applyValueAndDeactivate();
	}

	@Override
	public void deactivate() {
		super.deactivate();
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		Object value = getValue();

		// if value is reference to segment, open it - create it if necessary
		if (value instanceof SegmentReference) {
			SegmentReference sr = (SegmentReference) value;
			String segmentName = sr.getSegmentName();
			
			if (segmentSelector.selectSegment(segmentName)) {
				return null;
			}

			// no such segment - not editable
			MessageBox mb = new MessageBox(cellEditorWindow.getShell(), SWT.ICON_ERROR | SWT.OK);
			mb.setMessage("There is no segment \"" + segmentName + "\" defined in meta data. Please define this segment first.");
			mb.setText("No segment to edit");
			mb.open();
			return null;
		}

		// open dialog depending on type
		if (fieldValue != null) {
			IFieldValue val = fieldValue.getFieldValue();
			if (val.getValueType() == IFieldValue.TYPE_STRING_LIST) {
				// extract strings from value
				List<String> strings = new ArrayList<String>();
				for (IStringValue sv : ((IStringListValue) val).getValues()) {
					strings.add(sv.getValue());
				}

				StringListDialog dlg = new StringListDialog(cellEditorWindow.getShell());
				dlg.setValue(strings);
				if (dlg.open() == Dialog.OK) {
					setFieldValue(fieldValue, dlg.getValue());
					return dlg.getValue();
				}
			}
			else if (val.getValueType() == IFieldValue.TYPE_STRING) {
				ITestDataFieldMetadata meta = fieldValue.getMetadata(segmentSelector.getTestDataModel().getMetaData());

				StringEditorDialog dlg = new StringEditorDialog(cellEditorWindow.getShell(), fieldValue, meta);

				if (dlg.open() == StringEditorDialog.OK) {
					return fieldValue;
				}
			}
		}

		return null;
	}

	private void applyValueAndDeactivate() {
		if (valueToSet != null) {
			IFieldValue value = fieldValue.getFieldValue();
			if (value.getValueType() == IFieldValue.TYPE_STRING) {
				((IStringValue) value).setValue(valueToSet);
			}
		}
		fireApplyEditorValue();
		deactivate();
	}

	static void setFieldValue(ITestDataFieldValue field, Object value) {
		IFieldValue fv = field.getFieldValue();
		// MUST be != null
		if (fv != null) {
			switch (fv.getValueType()) {
				case IFieldValue.TYPE_STRING:
					((IStringValue) fv).setValue(value == null ? null : value.toString());
					break;
				case IFieldValue.TYPE_STRING_LIST:
					IStringListValue slv = (IStringListValue) fv;
					// simple code: remove all; add all new
					IStringValue[] values = slv.getValues();
					while (values != null && values.length > 0) {
						slv.removeValue(ArrayUtil.lastElement(values));
					}

					Collection<?> coll = (Collection<?>) value;
					for (Object o : coll) {
						slv.addValue(o == null ? null : o.toString());
					}
					break;
			}
		}

	}

}
