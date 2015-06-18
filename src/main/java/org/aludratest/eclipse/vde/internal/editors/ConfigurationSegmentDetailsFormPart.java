package org.aludratest.eclipse.vde.internal.editors;

import org.aludratest.eclipse.vde.internal.VdeImage;
import org.aludratest.eclipse.vde.internal.model.TestDataConfigurationSegment;
import org.aludratest.eclipse.vde.internal.model.TestDataFieldValue;
import org.aludratest.eclipse.vde.model.IFieldValue;
import org.aludratest.eclipse.vde.model.IStringListValue;
import org.aludratest.eclipse.vde.model.IStringValue;
import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ConfigurationSegmentDetailsFormPart extends AbstractFormPart implements IDetailsPage {

	private Section section;

	private TableViewer tvFields;

	private ConfigurationSegmentsMasterDetailsBlock masterBlock;

	private ITestDataConfigurationSegment segment;

	public ConfigurationSegmentDetailsFormPart(ConfigurationSegmentsMasterDetailsBlock masterBlock) {
		this.masterBlock = masterBlock;
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection sel = (IStructuredSelection) selection;
		Object input = sel.getFirstElement();
		tvFields.setInput(input);

		segment = (ITestDataConfigurationSegment) input;

		boolean allRed = false;
		if (input instanceof TestDataConfigurationSegment) {
			allRed = ((TestDataConfigurationSegment) input).isNotReferencedInMetadata(false);
		}

		tvFields.setLabelProvider(new SegmentFieldLabelProvider(getEditor().getTestDataModel().getMetaData(), allRed));
	}

	@Override
	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		GridLayout gl = new GridLayout(1, false);
		gl.marginTop = -5;
		gl.marginBottom = -5;
		parent.setLayout(gl);

		section = toolkit.createSection(parent, Section.EXPANDED | Section.DESCRIPTION | Section.TITLE_BAR);
		section.setText("Segment contents");
		section.setDescription("Main Segment Fields");
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite c = toolkit.createComposite(section);
		section.setClient(c);
		c.setLayout(new GridLayout(1, false));

		Table tbl = new Table(c, SWT.BORDER | SWT.FULL_SELECTION);
		tbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tbl.setHeaderVisible(true);
		// TODO resize listener to adjust table height to available client size; avoid scrolling of details part

		// create columns
		TableColumn tc = new TableColumn(tbl, SWT.LEAD);
		tc.setText("Field");
		tc.setWidth(120);
		tc = new TableColumn(tbl, SWT.LEAD);
		tc.setText("Data type");
		tc.setWidth(90);
		tc = new TableColumn(tbl, SWT.LEAD);
		tc.setText("Data subtype");
		tc.setWidth(140);
		tc = new TableColumn(tbl, SWT.LEAD);
		tc.setText("Value");
		tc.setWidth(200);

		tvFields = new TableViewer(tbl);
		tvFields.setContentProvider(new SegmentFieldsContentProvider());
		tvFields.setLabelProvider(new SegmentFieldLabelProvider(getEditor().getTestDataModel().getMetaData(), false));

		Menu contextMenu = new Menu(tbl);
		final MenuItem mnuDelete = new MenuItem(contextMenu, SWT.PUSH);
		mnuDelete.setText("Remove");
		mnuDelete.setImage(VdeImage.DELETE.getImage());
		contextMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				// get selected field; only enable if field is red
				ITestDataFieldValue field = (ITestDataFieldValue) ((IStructuredSelection) tvFields.getSelection())
						.getFirstElement();
				if (field != null && (field instanceof TestDataFieldValue)
						&& ((TestDataFieldValue) field).isNotReferencedInMetadata()) {
					mnuDelete.setEnabled(true);
				}
				else {
					mnuDelete.setEnabled(false);
				}
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		mnuDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemoveField();
			}
		});
		tbl.setMenu(contextMenu);

		// cell editors
		tvFields.setCellEditors(new CellEditor[] { null, null, null, new FieldValueCellEditor(masterBlock, tvFields.getTable()) });

		tvFields.setColumnProperties(new String[] { "name", "type", "subtype", "value" });
		tvFields.setCellModifier(new ICellModifier() {
			@Override
			public void modify(Object element, String property, Object value) {
				if (element instanceof Widget) {
					element = ((Widget) element).getData();
				}
				// nothing to do - all done by FieldValueCellEditor
				// only update
				tvFields.update(element, null);
			}

			@Override
			public Object getValue(Object element, String property) {
				ITestDataFieldValue field = (ITestDataFieldValue) element;
				if ("value".equals(property)) {
					return getFieldValue(field);
				}
				return null;
			}

			@Override
			public boolean canModify(Object element, String property) {
				return "value".equals(property);
			}
		});
	}

	private TestDataEditor getEditor() {
		return ((VisualDataEditorPage) getManagedForm().getContainer()).getEditor();
	}

	private Object getFieldValue(ITestDataFieldValue field) {
		IFieldValue fv = field.getFieldValue();
		if (fv == null) {
			ITestDataFieldMetadata meta = field.getMetadata(getEditor().getTestDataModel().getMetaData());
			// OK, calculate referenced segment name
			String segmentName = segment.getName();
			boolean isList = meta.getType() == TestDataFieldType.OBJECT_LIST;
			String refSegName = segmentName + "." + field.getFieldName() + (isList ? "-1" : "");
			return new SegmentReference(refSegName);
		}

		return field;
	}

	private void handleRemoveField() {
		ITestDataFieldValue field = (ITestDataFieldValue) ((IStructuredSelection) tvFields.getSelection())
				.getFirstElement();
		if (field != null && (segment instanceof TestDataConfigurationSegment)) {
			((TestDataConfigurationSegment) segment).removeFieldValue(field);
		}
		tvFields.refresh();
		masterBlock.refreshSegmentsList();
	}

	private static class SegmentFieldsContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((ITestDataConfigurationSegment) inputElement).getFieldValues();
		}

	}

	private class SegmentFieldLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private Color redColor;

		private Color yellowColor;

		private ITestDataMetadata metadata;

		private boolean allRed;

		public SegmentFieldLabelProvider(ITestDataMetadata metadata, boolean allRed) {
			this.metadata = metadata;
			this.allRed = allRed;
			redColor = new Color(PlatformUI.getWorkbench().getDisplay(), 255, 0, 0);
			yellowColor = new Color(PlatformUI.getWorkbench().getDisplay(), 255, 255, 219);
		}

		@Override
		public void dispose() {
			redColor.dispose();
			yellowColor.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				return VdeImage.FIELD.getImage();
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			ITestDataFieldValue field = (ITestDataFieldValue) element;

			switch (columnIndex) {
				case 0:
					return field.getFieldName();
				case 1:
					ITestDataFieldMetadata meta = field.getMetadata(metadata);
					return meta == null ? "unknown" : meta.getType().displayName();
				case 2:
					meta = field.getMetadata(metadata);
					// TODO perhaps simple name only
					return meta == null ? "unknown" : meta.getSubTypeClassName();
				case 3:
					IFieldValue fv = field.getFieldValue();
					if (fv == null) {
						return null;
					}

					if (fv.getValueType() == IFieldValue.TYPE_STRING) {
						return ((IStringValue) fv).getValue();
					}
					if (fv.getValueType() == IFieldValue.TYPE_STRING_LIST) {
						return toString(((IStringListValue) fv).getValues());
					}

					return null;
			}
			return null;
		}

		private String toString(IStringValue[] values) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (IStringValue sv : values) {
				if (sb.length() > 1) {
					sb.append(", ");
				}
				sb.append(sv.getValue());
			}
			sb.append("]");
			return sb.toString();
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (allRed || (element instanceof TestDataFieldValue && ((TestDataFieldValue) element).isNotReferencedInMetadata())) {
				return redColor;
			}

			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			if (columnIndex != 3) {
				return null;
			}

			ITestDataFieldValue field = (ITestDataFieldValue) element;
			if (field.isScript()) {
				return yellowColor;
			}

			return null;
		}

	}
}
