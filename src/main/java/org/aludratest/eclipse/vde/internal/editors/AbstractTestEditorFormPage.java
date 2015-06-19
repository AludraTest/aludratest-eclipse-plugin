package org.aludratest.eclipse.vde.internal.editors;

import org.eclipse.ui.forms.editor.FormPage;

public abstract class AbstractTestEditorFormPage extends FormPage {

	public AbstractTestEditorFormPage(TestDataEditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	public TestDataEditor getEditor() {
		return (TestDataEditor) super.getEditor();
	}

	protected abstract void refreshContents();

}
