package org.aludratest.eclipse.vde.internal.editors;

import org.aludratest.eclipse.vde.model.ITestData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class TestDataMetadataPage extends AbstractTestEditorFormPage {

	private static final String ID = "meta";

	private SegmentsMasterDetailsBlock masterDetailsBlock;

	public TestDataMetadataPage(TestDataEditor editor) {
		super(editor, ID, "Test Data Definition");
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText("Test Data Definition");
		form.getBody().setLayout(new GridLayout(1, false));

		masterDetailsBlock = new SegmentsMasterDetailsBlock();
		masterDetailsBlock.createContent(managedForm);
		masterDetailsBlock.setInput(getEditor().getTestDataModel());
	}

	@Override
	protected void refreshContents() {
		ITestData testData = getEditor().getTestDataModel();
		if (masterDetailsBlock != null) {
			masterDetailsBlock.setInput(testData);
		}
	}

}
