package org.aludratest.eclipse.vde.internal.editors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class IgnoreConfigDialog extends Dialog {

	private boolean ignored;

	private String reason;

	private Button btnIgnored;

	private Label lblReason;

	private Text txtReason;

	protected IgnoreConfigDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 10;
		gl.marginHeight = 10;
		c.setLayout(gl);

		btnIgnored = new Button(c, SWT.CHECK);
		btnIgnored.setText("Ignore this Test Data Configuration");
		btnIgnored.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ignored = btnIgnored.getSelection();
				lblReason.setEnabled(ignored);
				txtReason.setEnabled(ignored);
			}
		});

		lblReason = new Label(c, SWT.LEAD | SWT.WRAP);
		lblReason.setText("Please give an optional reason why this Test Data Configuration shall be ignored:");
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.widthHint = 350;
		gd.verticalIndent = 10;
		lblReason.setLayoutData(gd);

		txtReason = new Text(c, SWT.BORDER | SWT.LEAD);
		txtReason.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		txtReason.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				reason = txtReason.getText();
			}
		});

		setIgnored(ignored);
		lblReason.setEnabled(ignored);
		txtReason.setEnabled(ignored);
		setReason(reason);

		getShell().setText("Ignore Test Data Configuration");
		return super.createDialogArea(parent);
	}

	public void setReason(String reason) {
		this.reason = reason;
		if (txtReason != null && !txtReason.isDisposed()) {
			txtReason.setText(reason == null ? "" : reason);
		}
	}

	public String getReason() {
		return "".equals(reason) ? null : reason;
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
		if (btnIgnored != null && !btnIgnored.isDisposed()) {
			btnIgnored.setSelection(ignored);
		}
	}

	public boolean isIgnored() {
		return ignored;
	}

}
