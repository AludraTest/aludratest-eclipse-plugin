package org.aludratest.eclipse.vde.internal.editors;

import org.aludratest.eclipse.vde.internal.VdeImage;
import org.aludratest.eclipse.vde.internal.model.TestDataConfiguration;
import org.aludratest.eclipse.vde.internal.util.ArrayUtil;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
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

		Section section = toolkit.createSection(form.getBody(), Section.DESCRIPTION
				| Section.EXPANDED | Section.TITLE_BAR);
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
			}
		});

		Composite cpo = toolkit.createComposite(form.getBody());
		cpo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		configurationSegments = new ConfigurationSegmentsMasterDetailsBlock();
		configurationSegments.createContent(managedForm, cpo);

		section.setClient(c);
	}

	@Override
	public void refreshContents() {
		if (cvConfig != null) {
			// try to maintain selection
			String selectedConfigName = null;
			if (!cvConfig.getSelection().isEmpty()) {
				selectedConfigName = ((ITestDataConfiguration) ((IStructuredSelection)cvConfig.getSelection()).getFirstElement()).getName();
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

	private final static IInputValidator CONFIG_NAME_VALIDATOR = new IInputValidator() {
		@Override
		public String isValid(String newText) {
			if (newText == null || newText.length() == 0) {
				return "Please enter a value.";
			}
			if (newText.trim().length() != newText.length()) {
				return "Please do not begin or end with a space.";
			}

			// TODO handle existing names; handle differently for edit and add

			return null;
		}
	};

	private void handleAddConfiguration() {
		InputDialog dlg = new InputDialog(getEditorSite().getShell(), "Add Test Data Configuration",
				"Please enter the name for the new Test Data Configuration", "newConfiguration", CONFIG_NAME_VALIDATOR);
		
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
					"Please enter the name for the Test Data Configuration", config.getName(), CONFIG_NAME_VALIDATOR);

			if (dlg.open() != InputDialog.OK) {
				return;
			}

			config.setName(dlg.getValue());
			cvConfig.refresh();
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

	private static class TestDataConfigurationLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof ITestDataConfiguration) {
				return ((ITestDataConfiguration) element).getName();
			}
			return super.getText(element);
		}
	}

}
