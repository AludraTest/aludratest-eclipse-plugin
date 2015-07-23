package org.aludratest.eclipse.vde.internal.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aludratest.dict.Data;
import org.aludratest.eclipse.vde.internal.TestDataCore;
import org.aludratest.eclipse.vde.internal.TestDataModelDiff;
import org.aludratest.eclipse.vde.internal.VdeImage;
import org.aludratest.eclipse.vde.internal.contentassist.JavaTypeContentProposalProvider;
import org.aludratest.eclipse.vde.internal.util.ArrayUtil;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

class SegmentDetailsFormPart extends AbstractFormPart implements IDetailsPage {

	private SegmentsMasterDetailsBlock masterDetailsBlock;

	private TableViewer tvFields;

	private ITestDataSegmentMetadata segment;

	private Text txtSegmentName;

	private Text txtDataClass;

	private ControlDecoration cdDataClassWarning;

	public SegmentDetailsFormPart(SegmentsMasterDetailsBlock masterDetailsBlock) {
		this.masterDetailsBlock = masterDetailsBlock;
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		segment = (ITestDataSegmentMetadata) ((IStructuredSelection) selection).getFirstElement();
		tvFields.setInput(segment);

		txtSegmentName.setText(segment.getName());
		txtDataClass.setText(segment.getDataClassName() == null ? "" : segment.getDataClassName());

		checkDataClassExists();
	}

	private boolean checkDataClassExists() {
		IJavaProject project = JavaCore.create(((IFileEditorInput) getEditor().getEditorInput()).getFile().getProject());

		// optimization: Quick-check class to avoid full diff calculation
		String className = txtDataClass.getText();
		if (className != null && !"".equals(className.trim())) {
			IType tp;
			try {
				tp = project.findType(txtDataClass.getText());
			}
			catch (JavaModelException e) {
				// treat as not found
				tp = null;
			}
			if (tp == null) {
				cdDataClassWarning.show();
				return false;
			}
			else {
				cdDataClassWarning.hide();
				return true;
			}
		}
		else {
			// TODO error marker?
		}
		return false;
	}

	private TestDataEditor getEditor() {
		return ((TestDataMetadataPage) getManagedForm().getContainer()).getEditor();
	}

	@Override
	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		GridLayout gl = new GridLayout(1, false);
		gl.marginTop = -5;
		parent.setLayout(gl);

		Section section = toolkit.createSection(parent, Section.EXPANDED | Section.DESCRIPTION | Section.TITLE_BAR);
		section.setText("Segment details");
		section.setDescription("Edit here the fields of the segment.");
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite c = toolkit.createComposite(section);
		section.setClient(c);
		c.setLayout(new GridLayout(2, false));

		Composite cpoBaseData = toolkit.createComposite(c);
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalSpan = 2;
		cpoBaseData.setLayoutData(gd);
		cpoBaseData.setLayout(new GridLayout(3, false));
		toolkit.createLabel(cpoBaseData, "Name:", SWT.LEAD);
		txtSegmentName = toolkit.createText(cpoBaseData, "");
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalIndent = FieldDecorationRegistry.getDefault().getMaximumDecorationWidth();
		txtSegmentName.setLayoutData(gd);
		// TODO set to disabled when name contains "."
		txtSegmentName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				// TODO prohibit "." in name
				if (segment != null) {
					segment.setName(((Text) e.widget).getText());
					// update Master view
					masterDetailsBlock.updateMasterTreeView();
				}
			}
		});
		// placeholder for third column
		toolkit.createLabel(cpoBaseData, "");
		
		Link link = new Link(cpoBaseData, SWT.NONE);
		link.setText("<a>Data class:</a>");
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleOpenDataClass();
			}
		});

		txtDataClass = toolkit.createText(cpoBaseData, "");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalIndent = FieldDecorationRegistry.getDefault().getMaximumDecorationWidth();
		txtDataClass.setLayoutData(gd);
		txtDataClass.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (segment != null) {
					segment.setDataClassName(((Text) e.widget).getText());
				}
				if (checkDataClassExists()) {
					tvFields.refresh();
				}
			}
		});

		FieldDecoration fdec = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING);
		cdDataClassWarning = new ControlDecoration(txtDataClass, SWT.BOTTOM | SWT.LEFT);
		cdDataClassWarning.setImage(fdec.getImage());
		cdDataClassWarning.setDescriptionText("The data class could not be found on the current project's classpath");
		cdDataClassWarning.hide();

		Button btnBrowseClass = toolkit.createButton(cpoBaseData, "Browse...", SWT.PUSH);
		btnBrowseClass.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		JavaTypeContentProposalProvider.attachToComponents(txtDataClass, btnBrowseClass, getEditor(),
				"Please select a Data type to use for this segment.", Data.class.getName());

		Table tbl = new Table(c, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		tbl.setHeaderVisible(true);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		tbl.setLayoutData(gd);

		// create columns
		TableColumn tc = new TableColumn(tbl, SWT.LEAD);
		tc.setText("Field");
		tc.setWidth(120);
		tc = new TableColumn(tbl, SWT.LEAD);
		tc.setText("Data type");
		tc.setWidth(90);
		tc = new TableColumn(tbl, SWT.LEAD);
		tc.setText("Format pattern");
		tc.setWidth(140);
		tc = new TableColumn(tbl, SWT.LEAD);
		tc.setText("Data subtype");
		tc.setWidth(140);

		tvFields = new TableViewer(tbl);
		tvFields.setContentProvider(new SegmentFieldsContentProvider());
		tvFields.setLabelProvider(new SegmentFieldLabelProvider(getEditor().getSite().getWorkbenchWindow().getWorkbench()));

		// cell editors
		tvFields.setCellEditors(new CellEditor[] { new TextCellEditor(tvFields.getTable()),
				new ComboBoxCellEditor(tvFields.getTable(), TestDataFieldType.displayNames(), SWT.READ_ONLY),
				new TextCellEditor(tvFields.getTable()), // TODO format pattern editor dialog
				new DataClassCellEditor(getEditor(), tvFields.getTable())
		});
		tvFields.setColumnProperties(new String[] { "name", "type", "pattern", "subtype" });
		tvFields.setCellModifier(new ICellModifier() {
			@Override
			public void modify(Object element, String property, Object value) {
				if (element instanceof TableItem) {
					element = ((TableItem) element).getData();
				}

				Object prevValue = getValue(element, property);
				if (prevValue == null ? value == null : prevValue.equals(value)) {
					return;
				}

				SegmentFieldTableEntry entry = (SegmentFieldTableEntry) element;
				ITestDataFieldMetadata meta = entry.getField();
				if ("name".equals(property)) {
					meta.setName(value == null ? null : value.toString());
				}
				if ("type".equals(property)) {
					meta.setType(TestDataFieldType.values()[((Number) value).intValue()]);
					if (!canModify(entry, "pattern")) {
						meta.setFormatterPattern(null);
					}
				}
				if ("pattern".equals(property)) {
					meta.setFormatterPattern((value == null || "".equals(value)) ? null : value.toString());
				}
				if ("subtype".equals(property)) {
					meta.setSubTypeClassName((value == null || "".equals(value)) ? null : value.toString());
				}

				((SegmentFieldsContentProvider) tvFields.getContentProvider()).checkFieldCurrentness(segment, entry);
				tvFields.update(element, null);
			}

			@Override
			public Object getValue(Object element, String property) {
				SegmentFieldTableEntry entry = (SegmentFieldTableEntry) element;
				ITestDataFieldMetadata meta = entry.getField();
				if ("name".equals(property)) {
					return meta.getName();
				}
				if ("type".equals(property)) {
					return Integer.valueOf(Arrays.asList(TestDataFieldType.values()).indexOf(meta.getType()));
				}
				if ("pattern".equals(property)) {
					String pattern = meta.getFormatterPattern();
					return pattern == null ? "" : pattern;
				}
				if ("subtype".equals(property)) {
					return meta.getSubTypeClassName();
				}
				return null;
			}

			@Override
			public boolean canModify(Object element, String property) {
				SegmentFieldTableEntry entry = (SegmentFieldTableEntry) element;
				if (entry.getField() instanceof MissingFieldMetadata) {
					return false;
				}
				ITestDataFieldMetadata meta = entry.getField();

				if ("subtype".equals(property)) {
					return meta.getType() == TestDataFieldType.OBJECT || meta.getType() == TestDataFieldType.OBJECT_LIST;
				}
				if ("pattern".equals(property)) {
					return meta.getType() == TestDataFieldType.DATE || meta.getType() == TestDataFieldType.NUMBER;
				}
				return true;
			}
		});
		
		// tooltip for field warnings
		tvFields.getTable().addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				TableItem ti = tvFields.getTable().getItem(new Point(e.x, e.y));
				if (ti != null) {
					String text = ((SegmentFieldLabelProvider) tvFields.getLabelProvider()).getTooltipText(ti.getData());
					if (text == null) {
						tvFields.getTable().setToolTipText("");
					}
					else if (!text.equals(tvFields.getTable().getToolTipText())) {
						tvFields.getTable().setToolTipText(text);
					}
				}
				else {
					tvFields.getTable().setToolTipText("");
				}
			}
		});

		tvFields.getTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (e.button == 1) {
					IStructuredSelection sel = (IStructuredSelection) tvFields.getSelection();
					if (!sel.isEmpty()) {
						SegmentFieldTableEntry entry = (SegmentFieldTableEntry) sel.getFirstElement();
						if (entry.isVirtual()) {
							// add real field instead; update viewer
							segment.addField();
							ITestDataFieldMetadata field = ArrayUtil.lastElement(segment.getFields());
							field.setName(entry.getField().getName());
							field.setType(entry.getField().getType());
							field.setSubTypeClassName(entry.getField().getSubTypeClassName());
							String stcn = field.getSubTypeClassName();
							if (stcn != null && !"".equals(stcn) && !String.class.getName().equals(stcn)) {
								openSubSegment(field);
							}
							tvFields.refresh();
						}
						else if (entry.getWarningMessage() != null) {
							// TODO for diff fields, also implement (update to data class)
						}
						else {
							// jump to sub segment, if subtype; create if necessary
							ITestDataFieldMetadata field = entry.getField();
							String stcn = field.getSubTypeClassName();
							if (stcn != null && !"".equals(stcn) && !String.class.getName().equals(stcn)) {
								openSubSegment(field);
							}
						}
					}
				}
			}
		});

		Composite cpoButtons = toolkit.createComposite(c);
		gl = new GridLayout(1, false);
		gl.verticalSpacing = 8;
		cpoButtons.setLayout(gl);
		cpoButtons.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		Button btnAdd = toolkit.createButton(cpoButtons, "Add", SWT.PUSH);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		final Button btnRemove = toolkit.createButton(cpoButtons, "Delete", SWT.PUSH);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setEnabled(false);
		final Button btnUp = toolkit.createButton(cpoButtons, "Up", SWT.PUSH);
		btnUp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		final Button btnDown = toolkit.createButton(cpoButtons, "Down", SWT.PUSH);
		btnDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddField();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemoveField();
			}
		});
		btnUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleFieldUp();
			}
		});
		btnDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleFieldDown();
			}
		});

		tvFields.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnRemove.setEnabled(!event.getSelection().isEmpty());
				btnUp.setEnabled(!event.getSelection().isEmpty());
				btnDown.setEnabled(!event.getSelection().isEmpty());
				if (!event.getSelection().isEmpty()) {
					SegmentFieldTableEntry entry = (SegmentFieldTableEntry) ((IStructuredSelection) event.getSelection())
							.getFirstElement();
					btnUp.setEnabled(!entry.isVirtual());
					btnDown.setEnabled(!entry.isVirtual());
				}
			}
		});
	}

	private void openSubSegment(ITestDataFieldMetadata field) {
		String expectedSegmentName = segment.getName() + "." + field.getName();
		ITestData data = getEditor().getTestDataModel();
		ITestDataMetadata meta = data.getMetaData();

		for (ITestDataSegmentMetadata segment : meta.getSegments()) {
			if (expectedSegmentName.equals(segment.getName())) {
				masterDetailsBlock.selectSegment(segment);
				return;
			}
		}

		// create segment
		meta.addSegment(expectedSegmentName, field.getSubTypeClassName());
		// refresh parent
		masterDetailsBlock.setInput(data);
		masterDetailsBlock.selectSegment(ArrayUtil.lastElement(meta.getSegments()));
	}

	private void handleAddField() {
		segment.addField();
		tvFields.refresh();
	}

	private void handleRemoveField() {
		IStructuredSelection selection = (IStructuredSelection) tvFields.getSelection();
		if (!selection.isEmpty()) {
			SegmentFieldTableEntry entry = (SegmentFieldTableEntry) selection.getFirstElement();
			if (entry.isVirtual()) {
				return;
			}
			segment.removeField(entry.getField());
			tvFields.refresh();
		}
	}

	private void handleFieldUp() {
		IStructuredSelection selection = (IStructuredSelection) tvFields.getSelection();
		if (!selection.isEmpty()) {
			SegmentFieldTableEntry entry = (SegmentFieldTableEntry) selection.getFirstElement();
			ITestDataFieldMetadata field = entry.getField();
			ITestDataFieldMetadata[] allFields = segment.getFields();
			int index = Arrays.asList(allFields).indexOf(field);
			if (index < 1) {
				return; // no up possible
			}
			segment.moveField(field, index - 1);
			tvFields.refresh();
			tvFields.getTable().select(index - 1);
		}
	}

	private void handleFieldDown() {
		IStructuredSelection selection = (IStructuredSelection) tvFields.getSelection();
		if (!selection.isEmpty()) {
			SegmentFieldTableEntry entry = (SegmentFieldTableEntry) selection.getFirstElement();
			ITestDataFieldMetadata field = entry.getField();
			ITestDataFieldMetadata[] allFields = segment.getFields();
			int index = Arrays.asList(allFields).indexOf(field);
			if (index >= allFields.length - 1) {
				return; // no down possible
			}
			field = allFields[index + 1];
			segment.moveField(field, index);
			tvFields.refresh();
			tvFields.getTable().select(index + 1);
		}
	}
	
	private void handleOpenDataClass() {
		String typeName = txtDataClass.getText();
		
		if (typeName != null && !"".equals(typeName.trim())) {
			IJavaProject project = JavaCore.create(((IFileEditorInput) getEditor().getEditorInput()).getFile().getProject());
			IType tp;
			try {
				tp = project.findType(typeName);
				JavaUI.openInEditor(tp);
			}
			catch (JavaModelException e) {
				return;
			}
			catch (PartInitException e) {
				return;
			}
		}
	}

	private static class SegmentFieldLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private Image iWarningField;

		private Color grayColor;

		public SegmentFieldLabelProvider(IWorkbench workbench) {
			// calculate warning image here
			Image iField = VdeImage.FIELD.getImage();
			ImageDescriptor idWarning = workbench.getSharedImages().getImageDescriptor(ISharedImages.IMG_DEC_FIELD_WARNING);

			iWarningField = new DecorationOverlayIcon(iField, idWarning, IDecoration.BOTTOM_RIGHT).createImage();
			grayColor = new Color(workbench.getDisplay(), 128, 128, 128);
		}

		@Override
		public void dispose() {
			iWarningField.dispose();
			grayColor.dispose();
			super.dispose();
		}

		public String getTooltipText(Object element) {
			SegmentFieldTableEntry entry = (SegmentFieldTableEntry) element;
			return entry.getWarningMessage();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0) {
				return null;
			}

			SegmentFieldTableEntry entry = (SegmentFieldTableEntry) element;
			return entry.getWarningMessage() != null ? iWarningField : VdeImage.FIELD.getImage();
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			SegmentFieldTableEntry entry = (SegmentFieldTableEntry) element;
			ITestDataFieldMetadata field = entry.getField();

			switch (columnIndex) {
				case 0:
					return field.getName();
				case 1:
					return field.getType().displayName();
				case 2:
					return field.getFormatterPattern();
				case 3:
					String className = field.getSubTypeClassName();
					return (className == null || !className.contains(".")) ? className : className.substring(className
							.lastIndexOf('.') + 1);
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			SegmentFieldTableEntry entry = (SegmentFieldTableEntry) element;
			return entry.isVirtual() ? grayColor : null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

	}

	private class SegmentFieldsContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			ITestDataSegmentMetadata segment = (ITestDataSegmentMetadata) inputElement;
			return checkModelCurrentness(segment).toArray();
		}

		public void checkFieldCurrentness(ITestDataSegmentMetadata segment, SegmentFieldTableEntry field) {
			List<SegmentFieldTableEntry> allInfo = checkModelCurrentness(segment);
			for (SegmentFieldTableEntry entry : allInfo) {
				if (entry.equals(field)) {
					field.warningMessage = entry.warningMessage;
				}
			}
		}

		private List<SegmentFieldTableEntry> checkModelCurrentness(ITestDataSegmentMetadata segment) {
			List<SegmentFieldTableEntry> result = new ArrayList<SegmentFieldTableEntry>();

			IJavaProject project = JavaCore.create(((IFileEditorInput) getEditor().getEditorInput()).getFile().getProject());
			TestDataModelDiff[] diffs = TestDataCore.checkModelCurrentness(segment, project);

			for (ITestDataFieldMetadata field : segment.getFields()) {
				String fieldName = field.getName();
				// check if there is a field warning
				boolean added = false;
				for (TestDataModelDiff diff : diffs) {
					if (diff.getDiffType() == TestDataModelDiff.DiffType.MISSING_IN_CLASS
							&& fieldName.equals(diff.getFieldName())) {
						SegmentFieldTableEntry entry = new SegmentFieldTableEntry(field,
								"This field does not exist in the data class");
						result.add(entry);
						added = true;
						break;
					}
					if (diff.getDiffType() == TestDataModelDiff.DiffType.DIFFERS && fieldName.equals(diff.getFieldName())) {
						SegmentFieldTableEntry entry = new SegmentFieldTableEntry(field,
								"The field type does not reflect the data class field type");
						result.add(entry);
						added = true;
						break;
					}
				}
				if (!added) {
					result.add(new SegmentFieldTableEntry(field, null));
				}
			}

			// add virtual entries for non-existing fields
			for (TestDataModelDiff diff : diffs) {
				if (diff.getDiffType() == TestDataModelDiff.DiffType.MISSING_IN_MODEL) {
					result.add(new SegmentFieldTableEntry(new MissingFieldMetadata(diff),
							"This field is declared in the data class, but not yet configured in the Segment metadata. Double-click to add."));
				}
			}

			return result;
		}

	}

	private static class SegmentFieldTableEntry {

		private ITestDataFieldMetadata metaField;

		private String warningMessage;

		public SegmentFieldTableEntry(ITestDataFieldMetadata metaField, String warningMessage) {
			this.metaField = metaField;
			this.warningMessage = warningMessage;
		}

		public boolean isVirtual() {
			return metaField instanceof MissingFieldMetadata;
		}

		public ITestDataFieldMetadata getField() {
			return metaField;
		}

		public String getWarningMessage() {
			return warningMessage;
		}

		@Override
		public int hashCode() {
			return metaField.getName().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (obj.getClass() != getClass()) {
				return false;
			}

			SegmentFieldTableEntry entry = (SegmentFieldTableEntry) obj;
			if (entry.isVirtual() != isVirtual()) {
				return false;
			}
			String fn1 = entry.getField().getName();
			String fn2 = getField().getName();

			return fn1 == null ? false : fn1.equals(fn2);
		}

	}

}