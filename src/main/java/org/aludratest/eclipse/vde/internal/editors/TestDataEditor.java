package org.aludratest.eclipse.vde.internal.editors;

import org.aludratest.eclipse.vde.internal.model.DOMDocumentProvider;
import org.aludratest.eclipse.vde.internal.model.TestData;
import org.aludratest.eclipse.vde.internal.model.TestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

@SuppressWarnings("restriction")
public class TestDataEditor extends FormEditor implements IResourceChangeListener, IGotoMarker {

	/** The text editor used as last page. */
	private StructuredTextEditor sourceEditor;

	private TestDataMetadataPage metadataPage;

	private VisualDataEditorPage visualEditPage;

	private GridEditorPage gridPage;

	/** The page index of the source editor. */
	private int sourceEditorIndex;

	/** The model reflecting the current editor contents. */
	private ITestData testData;

	private boolean sourceDirty;

	private int oldPageIndex = -1;

	private DOMDocumentProvider documentProvider = new TestDataEditorDOMDocumentProvider();

	/**
	 * Creates a multi-page editor example.
	 */
	public TestDataEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	@Override
	protected void addPages() {
		try {
			addPage(metadataPage = new TestDataMetadataPage(this));
			addPage(visualEditPage = new VisualDataEditorPage(this));
			addPage(gridPage = new GridEditorPage(this));
			sourceEditorIndex = addPage(sourceEditor = new StructuredTextEditor(), getEditorInput());
			getDocument().addDocumentListener(new IDocumentListener() {
				@Override
				public void documentChanged(DocumentEvent event) {
					sourceDirty = true;
				}

				@Override
				public void documentAboutToBeChanged(DocumentEvent event) {
				}
			});
			setPageText(sourceEditorIndex, sourceEditor.getTitle());

			// sync document internals
			ITestData model = getTestDataModel();
			ITestDataMetadata metadata = model.getMetaData();
			for (ITestDataConfiguration config : model.getConfigurations()) {
				if (config instanceof TestDataConfiguration) {
					((TestDataConfiguration) config).syncToMetadata(metadata);
				}
			}
		}
		catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating Visual Data Editor", null, e.getStatus());
		}
	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		getEditor(sourceEditorIndex).doSave(monitor);
	}

	@Override
	public String getTitle() {
		IFile file = ResourceUtil.getFile(getEditorInput());
		return file != null ? file.getName() : super.getTitle();
	}

	@Override
	public String getTitleToolTip() {
		IFile file = ResourceUtil.getFile(getEditorInput());
		return file != null ? file.getFullPath().toString() : super.getTitleToolTip();
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the text for page 0's tab, and updates this multi-page
	 * editor's input to correspond to the nested editor's.
	 */
	@Override
	public void doSaveAs() {
		IEditorPart editor = getEditor(sourceEditorIndex);
		editor.doSaveAs();
		setPageText(sourceEditorIndex, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	@Override
	public boolean isDirty() {
		// TODO replace with clean code
		try {
			return super.isDirty();
		}
		catch (Exception e) {
			return false;
		}
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart
	 */
	@Override
	public void gotoMarker(IMarker marker) {
		setActivePage(sourceEditorIndex);
		IDE.gotoMarker(getEditor(sourceEditorIndex), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method checks that the input is an instance of
	 * <code>IFileEditorInput</code>.
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);
		setPartName(getTitle());
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart.
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	protected void pageChange(int newPageIndex) {
		// check for update from the source code
		if (testData == null || (oldPageIndex == sourceEditorIndex && sourceDirty)) {
			if (newPageIndex == 0) { // TODO store index from addPage()
				metadataPage.refreshContents();
			}
			if (newPageIndex == 1) {
				visualEditPage.refreshContents();
			}
			if (newPageIndex == 2) {
				gridPage.refreshContents();
			}
		}
		
		oldPageIndex = newPageIndex;
		super.pageChange(newPageIndex);

		IFormPage pg = getActivePageInstance();
		if (pg != null) {
			IActionBarsPopulator populator = (IActionBarsPopulator) pg.getAdapter(IActionBarsPopulator.class);
			if (populator != null) {
				IActionBars bars = getEditorSite().getActionBars();
				populator.contributeToActionBars(bars);
				bars.updateActionBars();
			}
		}
	}

	/**
	 * Closes all project files on project close.
	 */
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i < pages.length; i++) {
						if (((FileEditorInput) sourceEditor.getEditorInput()).getFile().getProject().equals(event.getResource())) {
							IEditorPart editorPart = pages[i].findEditor(sourceEditor.getEditorInput());
							pages[i].closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

	protected ITestData getTestDataModel() {
		if (testData == null) {
			testData = new TestData(documentProvider);
		}
		return testData;
	}

	private IDocument getDocument() {
		final IDocumentProvider provider = sourceEditor.getDocumentProvider();
		return provider.getDocument(getEditorInput());
	}

	StructuredTextEditor getSourceEditor() {
		return sourceEditor;
	}

	private class TestDataEditorDOMDocumentProvider implements DOMDocumentProvider {

		private IDOMModel domModelForRead;

		private IDOMModel domModelForWrite;

		private int readCounter;

		private int writeCounter;

		@Override
		public IDOMDocument getDOMDocumentForRead() {
			if (domModelForRead == null) {
				domModelForRead = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(
						(IStructuredDocument) getDocument());
			}
			readCounter++;

			return domModelForRead.getDocument();
		}

		@Override
		public IDOMDocument getDOMDocumentForEdit() {
			if (domModelForWrite == null) {
				domModelForWrite = (IDOMModel) StructuredModelManager.getModelManager().getModelForEdit(
						(IStructuredDocument) getDocument());
			}
			writeCounter++;

			return domModelForWrite.getDocument();
		}

		@Override
		public void releaseFromRead() {
			readCounter--;
			if (readCounter == 0) {
				domModelForRead.releaseFromRead();
				domModelForRead = null;
			}
		}

		@Override
		public void releaseFromEdit() {
			writeCounter--;
			if (writeCounter == 0) {
				domModelForWrite.releaseFromEdit();
				domModelForWrite = null;
			}
		}

	}
}
