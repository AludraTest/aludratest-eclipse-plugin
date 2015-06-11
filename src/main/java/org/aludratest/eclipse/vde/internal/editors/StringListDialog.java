package org.aludratest.eclipse.vde.internal.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class StringListDialog extends Dialog {

	private Text txtLines;

	private List<String> value;

	private Label lblLineCount;

	protected StringListDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));

		Label lbl = new Label(c, SWT.LEAD);
		lbl.setText("Please enter the field values, one value per line:");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		txtLines = new Text(c, SWT.BORDER | SWT.LEAD | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 260;
		txtLines.setLayoutData(gd);
		txtLines.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLineCount();
			}
		});

		lblLineCount = new Label(c, SWT.LEAD);
		setValue(this.value);
		updateLineCount();

		getShell().setText("Edit String List");

		return super.createDialogArea(parent);
	}

	private List<String> retrieveValue() {
		String txt = txtLines.getText();
		if (txt == null || "".equals(txt)) {
			return Collections.emptyList();
		}
		return (value = Arrays.asList(txt.split(txtLines.getLineDelimiter())));
	}

	public void setValue(List<String> values) {
		this.value = new ArrayList<String>(values);

		if (txtLines != null) {
			StringBuilder sb = new StringBuilder();
			for (String s : values) {
				if (sb.length() > 0) {
					sb.append(txtLines.getLineDelimiter());
				}
				sb.append(s);
			}
			txtLines.setText(sb.toString());
		}
	}

	public List<String> getValue() {
		return value;
	}

	private void updateLineCount() {
		int cnt = retrieveValue().size();
		lblLineCount.setText(cnt + " value(s) in list.");
	}

}
