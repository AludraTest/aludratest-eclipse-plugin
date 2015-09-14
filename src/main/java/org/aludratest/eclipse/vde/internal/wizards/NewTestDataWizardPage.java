package org.aludratest.eclipse.vde.internal.wizards;

import org.aludratest.eclipse.vde.internal.TestDataCore;
import org.aludratest.eclipse.vde.internal.contentassist.JavaTypeContentProposalProvider;
import org.aludratest.testcase.AludraTestCase;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (testdata.xml).
 */

public class NewTestDataWizardPage extends WizardPage {
	private Text containerText;

	private Text fileText;

	private ISelection selection;

	private Button btnFromTestClass;

	private Label lblTestClass;

	private Text txtTestClass;

	private Button btnBrowseTestClass;

	private IResource projectResource;

	private IType testClass;

	public NewTestDataWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("Create Test Data File");
		setDescription("This wizard creates a new test data file for use with AludraTest, either based on an existing test case class, or from scratch.");
		this.selection = selection;

		// try to extract a project resource from selection
		if (selection instanceof IStructuredSelection) {
			Object sel = ((IStructuredSelection) selection).getFirstElement();
			if (sel instanceof IResource) {
				projectResource = (IResource) sel;
				// try to extract test class
				IJavaElement elem = JavaCore.create(projectResource);
				if (elem instanceof IType) {
					testClass = (IType) elem;
				}
				else if (elem instanceof ICompilationUnit) {
					testClass = ((ICompilationUnit) elem).findPrimaryType();
				}
			}
			else if (sel instanceof IJavaElement) {
				projectResource = ((IJavaElement) sel).getResource();
				if (sel instanceof IType) {
					testClass = (IType) sel;
				}
				else if (sel instanceof ITypeRoot) {
					testClass = ((ITypeRoot) sel).findPrimaryType();
				}
				else {
					testClass = (IType) ((IJavaElement) sel).getAncestor(IJavaElement.TYPE);
				}
			}
		}
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		Label label = new Label(container, SWT.NULL);
		label.setText("&Path:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		label = new Label(container, SWT.NULL);
		label.setText("&File name:");

		fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileText.setLayoutData(gd);
		fileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		btnFromTestClass = new Button(container, SWT.CHECK);
		btnFromTestClass.setText("Initialize from existing Test Class");
		btnFromTestClass.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
				updateControlsEnabled();
			}
		});

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		btnFromTestClass.setLayoutData(gd);

		lblTestClass = new Label(container, SWT.LEAD);
		lblTestClass.setText("Test class:");

		txtTestClass = new Text(container, SWT.BORDER | SWT.LEAD);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = FieldDecorationRegistry.getDefault().getMaximumDecorationWidth();
		txtTestClass.setLayoutData(gd);
		if (testClass != null) {
			txtTestClass.setText(testClass.getFullyQualifiedName());
		}
		txtTestClass.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		btnBrowseTestClass = new Button(container, SWT.PUSH);
		btnBrowseTestClass.setText("Browse...");
		if (projectResource != null) {
			JavaTypeContentProposalProvider.attachToComponents(txtTestClass, btnBrowseTestClass, projectResource,
					"Select Test Case class", AludraTestCase.class.getName(), "*Test");
		}
		else {
			btnBrowseTestClass.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					handleBrowseForTestClass();
				}
			});
		}

		initialize();
		dialogChanged();
		updateControlsEnabled();
		setControl(container);
	}

	private void updateControlsEnabled() {
		boolean useTestClass = btnFromTestClass.getSelection();

		lblTestClass.setEnabled(useTestClass);
		txtTestClass.setEnabled(useTestClass);
		btnBrowseTestClass.setEnabled(useTestClass && projectResource != null);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	private void initialize() {
		if (projectResource != null) {
			IContainer container = projectResource instanceof IContainer ? (IContainer) projectResource : projectResource
					.getParent();
			containerText.setText(container.getFullPath().toString());
		}
		else if (selection != null && selection.isEmpty() == false
				&& selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
				containerText.setText(container.getFullPath().toString());
			}
		}

		fileText.setText("new.testdata.xml");
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select new file container");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	private void handleBrowseForTestClass() {
		// TODO implement
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
		String fileName = getFileName();

		if (getContainerName().length() == 0) {
			updateStatus("File path must be specified");
			return;
		}
		if (container == null
				|| (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("File path must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}
		if (fileName.contains(".") && !fileName.endsWith(".testdata.xml")) {
			updateStatus("File extension must be \"testdata.xml\"");
			return;
		}
		// file must not exist
		IContainer iContainer = (IContainer) container;
		if (iContainer.findMember(fileName) != null) {
			updateStatus("A file with this name already exists.");
			return;
		}

		if (iContainer != projectResource) {
			projectResource = iContainer;
			updateControlsEnabled();
		}

		// test class must exist
		if (btnFromTestClass.getSelection()) {
			IType tp = TestDataCore.findClass(projectResource, txtTestClass.getText());
			if (tp == null) {
				updateStatus("The test class could not be found.");
				return;
			}
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getFileName() {
		return fileText.getText();
	}

	public IType getInitializeFromClass() {
		if (!btnFromTestClass.getSelection() || projectResource == null) {
			return null;
		}

		return TestDataCore.findClass(projectResource, txtTestClass.getText());
	}
}