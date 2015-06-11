package org.aludratest.eclipse.vde.internal.editors;

import org.aludratest.dict.Data;
import org.aludratest.eclipse.vde.internal.contentassist.JavaTypeContentProposalProvider;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IFileEditorInput;

public class DataClassCellEditor extends DialogCellEditor {

	private TestDataEditor editor;

	public DataClassCellEditor(TestDataEditor editor, Composite parent) {
		super(parent);
		this.editor = editor;
	}

	@Override
	protected Control createContents(Composite cell) {
		Composite c = new Composite(cell, SWT.NONE);
		FillLayout fl = new FillLayout();
		fl.marginWidth = 6;
		c.setLayout(fl);
		super.createContents(c);
		return c;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		String className = (String) getValue();
		if (className != null && className.contains(".")) {
			className = className.substring(className.lastIndexOf('.') + 1);
		}

		return JavaTypeContentProposalProvider.browseForClass(editor.getSite().getShell(), editor.getSite()
				.getWorkbenchWindow(), ((IFileEditorInput) editor.getEditorInput()).getFile(), className,
				"Please select the Data class to use as subtype for this field.", Data.class.getName());
	}


}
