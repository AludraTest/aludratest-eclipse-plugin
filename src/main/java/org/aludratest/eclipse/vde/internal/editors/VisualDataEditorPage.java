package org.aludratest.eclipse.vde.internal.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aludratest.eclipse.vde.internal.VdeImage;
import org.aludratest.eclipse.vde.internal.model.TestDataConfiguration;
import org.aludratest.eclipse.vde.internal.util.ArrayUtil;
import org.aludratest.eclipse.vde.model.IFieldValue;
import org.aludratest.eclipse.vde.model.IStringListValue;
import org.aludratest.eclipse.vde.model.IStringValue;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

public class VisualDataEditorPage extends AbstractTestEditorFormPage {

	private static final String ID = "vde";

	private ComboViewer cvConfig;

	private ConfigurationSegmentsMasterDetailsBlock configurationSegments;

	public VisualDataEditorPage(TestDataEditor editor) {
		super(editor, ID, "Visual Data Editor");
	}

	@Override
	public TestDataEditor getEditor() {
		return super.getEditor();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Visual Data Editor");
		form.getBody().setLayout(new GridLayout(1, false));

		Section section = toolkit.createSection(form.getBody(), Section.DESCRIPTION | Section.EXPANDED | Section.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		section.setText("Configurations");
		section.setDescription("Select the test data configuration to edit.");

		Composite c = toolkit.createComposite(section, SWT.WRAP);
		GridLayout layout = new GridLayout(3, false);
		c.setLayout(layout);

		toolkit.createLabel(c, "Configuration:", SWT.LEAD);

		CCombo cbo = new CCombo(c, SWT.LEAD | SWT.BORDER);
		cbo.setEditable(false);
		toolkit.adapt(cbo);
		cvConfig = new ComboViewer(cbo);
		cvConfig.setContentProvider(new ArrayContentProvider());
		cvConfig.setLabelProvider(new TestDataConfigurationLabelProvider());
		cvConfig.setInput(getEditor().getTestDataModel().getConfigurations());

		ToolBar toolbar = new ToolBar(c, SWT.FLAT);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		toolkit.adapt(toolbar);

		ToolItem ti = new ToolItem(toolbar, SWT.PUSH);
		ti.setImage(VdeImage.ADD.getImage());
		ti.setToolTipText("Add Configuration");
		ti.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddConfiguration();
			}
		});
		final ToolItem tiDelete = new ToolItem(toolbar, SWT.PUSH);
		tiDelete.setImage(VdeImage.DELETE.getImage());
		tiDelete.setToolTipText("Delete Configuration");
		tiDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemoveConfiguration();
			}
		});
		tiDelete.setEnabled(false);

		final ToolItem tiEdit = new ToolItem(toolbar, SWT.PUSH);
		tiEdit.setImage(VdeImage.RENAME.getImage());
		tiEdit.setToolTipText("Rename Configuration");
		tiEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRenameConfiguration();
			}
		});
		tiEdit.setEnabled(false);

		final ToolItem tiDuplicate = new ToolItem(toolbar, SWT.PUSH);
		tiDuplicate.setImage(VdeImage.DUPLICATE.getImage());
		tiDuplicate.setToolTipText("Duplicate Configuration");
		tiDuplicate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDuplicateConfiguration();
			}
		});
		tiDuplicate.setEnabled(false);

		final ToolItem tiIgnore = new ToolItem(toolbar, SWT.CHECK);
		tiIgnore.setImage(VdeImage.IGNORE.getImage());
		tiIgnore.setToolTipText("Ignore (skip) Configuration");
		tiIgnore.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tiIgnore.setSelection(handleIgnoreConfiguration());
			}
		});
		tiIgnore.setEnabled(false);

		cvConfig.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				Object obj = sel.getFirstElement();
				if (obj instanceof TestDataConfiguration) {
					((TestDataConfiguration) obj).syncToMetadata(getEditor().getTestDataModel().getMetaData());
				}

				configurationSegments.setInput(sel.isEmpty() ? null : sel.getFirstElement());
				tiDelete.setEnabled(!sel.isEmpty());
				tiEdit.setEnabled(!sel.isEmpty());
				tiIgnore.setEnabled(!sel.isEmpty());
				boolean ignore = !sel.isEmpty() && (obj instanceof TestDataConfiguration)
						&& ((TestDataConfiguration) obj).isIgnored();
				tiIgnore.setSelection(ignore);
				tiDuplicate.setEnabled(!sel.isEmpty());
			}
		});

		Composite cpo = toolkit.createComposite(form.getBody());
		cpo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		configurationSegments = new ConfigurationSegmentsMasterDetailsBlock();
		configurationSegments.createContent(managedForm, cpo);

		ITestDataConfiguration[] configs = (ITestDataConfiguration[]) cvConfig.getInput();
		if (configs != null && configs.length > 0) {
			cvConfig.setSelection(new StructuredSelection(configs[0]));
		}

		section.setClient(c);
	}

	@Override
	public void refreshContents() {
		if (cvConfig != null) {
			// try to maintain selection
			String selectedConfigName = null;
			if (!cvConfig.getSelection().isEmpty()) {
				selectedConfigName = ((ITestDataConfiguration) ((IStructuredSelection) cvConfig.getSelection()).getFirstElement())
						.getName();
			}
			ITestDataConfiguration[] configs = getEditor().getTestDataModel().getConfigurations();
			cvConfig.setInput(configs);

			if (selectedConfigName != null) {
				for (ITestDataConfiguration config : configs) {
					if (selectedConfigName.equals(config.getName())) {
						cvConfig.setSelection(new StructuredSelection(config));
						break;
					}
				}
			}
			else {
				cvConfig.setSelection(new StructuredSelection());
			}
		}

		if (configurationSegments != null) {
			configurationSegments.refreshSegmentsList();
		}
	}

	private void handleAddConfiguration() {
		InputDialog dlg = new InputDialog(getEditorSite().getShell(), "Add Test Data Configuration",
				"Please enter the name for the new Test Data Configuration", "newConfiguration", createConfigNameValidator(false));

		if (dlg.open() != InputDialog.OK) {
			return;
		}

		String configName = dlg.getValue();
		ITestData testData = getEditor().getTestDataModel();
		testData.addConfiguration(configName);
		ITestDataConfiguration config = ArrayUtil.lastElement(testData.getConfigurations());
		if (config instanceof TestDataConfiguration) {
			((TestDataConfiguration) config).syncToMetadata(testData.getMetaData());
		}

		cvConfig.setInput(testData.getConfigurations());
		cvConfig.getControl().getParent().getParent().layout();
		cvConfig.setSelection(new StructuredSelection(config));
	}

	private void handleRemoveConfiguration() {
		ITestDataConfiguration config = (ITestDataConfiguration) ((IStructuredSelection) cvConfig.getSelection())
				.getFirstElement();
		if (config != null) {
			MessageBox box = new MessageBox(getSite().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			box.setMessage("Delete test data configuration \"" + config.getName() + "\"?");
			box.setText("Confirm delete");
			if (box.open() == SWT.YES) {
				ITestData testData = getEditor().getTestDataModel();
				testData.removeConfiguration(config);
				cvConfig.setInput(testData.getConfigurations());
				// to refresh enabled state etc
				cvConfig.setSelection(new StructuredSelection());
			}
		}
	}

	private void handleRenameConfiguration() {
		ITestDataConfiguration config = (ITestDataConfiguration) ((IStructuredSelection) cvConfig.getSelection())
				.getFirstElement();
		if (config != null) {
			InputDialog dlg = new InputDialog(getEditorSite().getShell(), "Rename Test Data Configuration",
					"Please enter the name for the Test Data Configuration", config.getName(), createConfigNameValidator(true));

			if (dlg.open() != InputDialog.OK) {
				return;
			}

			config.setName(dlg.getValue());
			cvConfig.refresh();
		}
	}

	private void handleDuplicateConfiguration() {
		ITestDataConfiguration config = (ITestDataConfiguration) ((IStructuredSelection) cvConfig.getSelection())
				.getFirstElement();
		if (config != null) {
			// first of all, query name for new config
			InputDialog dlg = new InputDialog(getEditorSite().getShell(), "Duplicate Test Data Configuration",
					"Please enter the name for the new Test Data Configuration", config.getName(),
					createConfigNameValidator(false));

			if (dlg.open() != InputDialog.OK) {
				return;
			}

			// create new configuration
			ITestData testData = getEditor().getTestDataModel();
			testData.addConfiguration(dlg.getValue());
			ITestDataConfiguration copyConfig = ArrayUtil.lastElement(testData.getConfigurations());
			if (copyConfig instanceof TestDataConfiguration) {
				((TestDataConfiguration) copyConfig).syncToMetadata(testData.getMetaData());

				// build map for faster access
				Map<String, ITestDataConfigurationSegment> copySegments = new HashMap<String, ITestDataConfigurationSegment>();
				for (ITestDataConfigurationSegment segment : copyConfig.getSegments()) {
					copySegments.put(segment.getName(), segment);
				}

				// copy data
				for (ITestDataConfigurationSegment segment : config.getSegments()) {
					ITestDataConfigurationSegment copySegment = copySegments.get(segment.getName());
					if (copySegment != null) {
						for (ITestDataFieldValue fv : segment.getDefinedFieldValues()) {
							ITestDataFieldValue copyValue = copySegment.getFieldValue(fv.getFieldName(), true);
							TestDataFieldType fieldType = fv.getMetadata(testData.getMetaData()).getType();
							if (fieldType != TestDataFieldType.OBJECT && fieldType != TestDataFieldType.OBJECT_LIST) {
								IFieldValue ofv = fv.getFieldValue();
								IFieldValue cfv = copyValue.getFieldValue();
								if (cfv.getValueType() == IFieldValue.TYPE_STRING
										&& ofv.getValueType() == IFieldValue.TYPE_STRING) {
									((IStringValue) cfv).setValue(((IStringValue) ofv).getValue());
								}
								else {
									for (IStringValue s : (((IStringListValue) ofv).getValues())) {
										((IStringListValue) cfv).addValue(s.getValue());
									}
								}
								copyValue.setScript(fv.isScript());
							}
						}
					}
				}
			}

			// update control
			cvConfig.setInput(testData.getConfigurations());
			cvConfig.getControl().getParent().getParent().layout();
			cvConfig.setSelection(new StructuredSelection(copyConfig));
		}
	}

	private boolean handleIgnoreConfiguration() {
		ITestDataConfiguration config = (ITestDataConfiguration) ((IStructuredSelection) cvConfig.getSelection())
				.getFirstElement();
		if (config != null) {
			IgnoreConfigDialog dlg = new IgnoreConfigDialog(getEditorSite().getShell());
			dlg.setIgnored(config.isIgnored());
			dlg.setReason(config.getIgnoredReason());
			if (dlg.open() == IgnoreConfigDialog.OK) {
				config.setIgnored(dlg.isIgnored());
				config.setIgnoredReason(dlg.isIgnored() ? dlg.getReason() : null);
			}

			return config.isIgnored();
		}

		return false;
	}

	private IInputValidator createConfigNameValidator(boolean edit) {
		List<String> disallowedNames = new ArrayList<String>();
		ITestDataConfiguration[] configs = getEditor().getTestDataModel().getConfigurations();

		ITestDataConfiguration selConfig = (ITestDataConfiguration) ((IStructuredSelection) cvConfig.getSelection())
				.getFirstElement();

		for (ITestDataConfiguration config : configs) {
			if (!edit || !config.equals(selConfig)) {
				disallowedNames.add(config.getName());
			}
		}

		return new ConfigNameValidator(disallowedNames);
	}

	private static class TestDataConfigurationLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof ITestDataConfiguration) {
				return ((ITestDataConfiguration) element).getName();
			}
			return super.getText(element);
		}
	}

	static class ConfigNameValidator implements IInputValidator {

		private List<String> disallowedNames;

		public ConfigNameValidator(List<String> disallowedNames) {
			// no copy constructor as this is all internal and under our control
			this.disallowedNames = disallowedNames;
		}

		@Override
		public String isValid(String newText) {
			if (newText == null || newText.length() == 0) {
				return "Please enter a value.";
			}
			if (newText.trim().length() != newText.length()) {
				return "Please do not begin or end with a space.";
			}

			if (disallowedNames.contains(newText)) {
				return "There already exists a configuration with this name";
			}

			return null;
		}
	};

}
