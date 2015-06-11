package org.aludratest.eclipse.vde.internal.editors;

import org.aludratest.dict.Data;
import org.aludratest.eclipse.vde.internal.contentassist.JavaTypeContentProposalProvider;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IFileEditorInput;

public class NewSegmentDialog extends TitleAreaDialog {

	private Text txtName;

	private Text txtClass;

	private TestDataEditor editor;

	private String segmentName;

	private String className;

	private ITestDataMetadata metadata;

	private IType dataClassType;

	public NewSegmentDialog(TestDataEditor editor, ITestDataMetadata metadata) {
		super(editor.getSite().getShell());
		this.editor = editor;
		this.metadata = metadata;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control c = super.createContents(parent);
		validate();
		return c;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite client = (Composite) super.createDialogArea(parent);
		Composite c = new Composite(client, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridLayout gl = new GridLayout(3, false);
		gl.horizontalSpacing = 4;
		gl.marginWidth = 5;
		c.setLayout(gl);

		ModifyListener validationListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};

		new Label(c, SWT.LEAD).setText("Segment name:");
		txtName = new Text(c, SWT.LEAD | SWT.BORDER);
		txtName.setText("newSegment");
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalIndent = FieldDecorationRegistry.getDefault().getMaximumDecorationWidth();
		txtName.setLayoutData(gd);
		txtName.addModifyListener(validationListener);

		// placeholder for 3-column layout
		new Label(c, SWT.LEAD).setText(" ");

		new Label(c, SWT.LEAD).setText("Data class:");
		txtClass = new Text(c, SWT.LEAD | SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalIndent = FieldDecorationRegistry.getDefault().getMaximumDecorationWidth();
		txtClass.setLayoutData(gd);
		txtClass.addModifyListener(validationListener);

		Button btnBrowseClass = new Button(c, SWT.PUSH);
		btnBrowseClass.setText("Browse...");
		btnBrowseClass.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		JavaTypeContentProposalProvider.attachToComponents(txtClass, btnBrowseClass, editor,
				"Please select a Data type to use for this segment.", Data.class.getName());

		setTitle("Add Segment");
		getShell().setText("Add Segment");

		return c;
	}

	@Override
	protected void okPressed() {
		this.segmentName = txtName.getText();
		this.className = txtClass.getText();
		super.okPressed();
	}

	public String getSegmentName() {
		return segmentName;
	}

	public String getClassName() {
		return className;
	}

	public IType getDataClassType() {
		return dataClassType;
	}

	private void validate() {
		String name = txtName.getText();
		String className = txtClass.getText();

		boolean error = true;

		if (name == null || "".equals(name)) {
			setMessage("A name for the new segment must be entered.", IMessageProvider.ERROR);
		}
		else if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
			setMessage("The segment name is invalid.", IMessageProvider.ERROR);
		}
		else if (nameExists(name)) {
			setMessage("A segment with this name already exists.", IMessageProvider.ERROR);
		}
		else if (className == null || "".equals(className)) {
			setMessage("A Data class name must be entered or selected.", IMessageProvider.ERROR);
		}
		else if (checkType(className) == null) {
			setMessage("The Data class could not be found on current classpath.", IMessageProvider.WARNING);
			error = false;
		}
		else {
			setMessage("Please enter a segment name and select a Data class.", IMessageProvider.INFORMATION);
			error = false;
		}

		Button button = getButton(IDialogConstants.OK_ID);
		if (button != null) {
			button.setEnabled(!error);
		}
	}

	private IType checkType(String className) {
		IJavaProject project = JavaCore.create(((IFileEditorInput) editor.getEditorInput()).getFile().getProject());
		try {
			return (dataClassType = project.findType(className));
		}
		catch (JavaModelException e) {
			return (dataClassType = null);
		}
	}

	private boolean nameExists(String name) {
		for (ITestDataSegmentMetadata segment : metadata.getSegments()) {
			if (name.equals(segment.getName())) {
				return true;
			}
		}
		return false;
	}
}
