package org.aludratest.eclipse.vde.internal.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Manages the installation/deinstallation of global actions for multi-page editors. Responsible for the redirection of global
 * actions to the active editor. Multi-page contributor replaces the contributors for the individual editors in the multi-page
 * editor.
 */
public class TestDataEditorContributor extends MultiPageEditorActionBarContributor {
	private Action sampleAction;

	private TestDataEditor editor;

	/**
	 * Creates a multi-page contributor.
	 */
	public TestDataEditorContributor() {
		super();
		createActions();
	}

	/**
	 * Returns the action registed with the given text editor.
	 * 
	 * @return IAction or null if editor is null.
	 */
	protected IAction getAction(ITextEditor editor, String actionID) {
		return (editor == null ? null : editor.getAction(actionID));
	}

	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		this.editor = (TestDataEditor) part;
		setActivePage(editor == null ? null : editor.getActiveEditor());
	}

	/*
	 * (non-JavaDoc) Method declared in AbstractMultiPageEditorActionBarContributor.
	 */
	@Override
	public void setActivePage(IEditorPart part) {
		IActionBars actionBars = getActionBars();
		if (actionBars != null && (part instanceof ITextEditor)) {
			ITextEditor editor = (ITextEditor) part;

			actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), getAction(editor, ITextEditorActionConstants.DELETE));
			actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), getAction(editor, ITextEditorActionConstants.UNDO));
			actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), getAction(editor, ITextEditorActionConstants.REDO));
			actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), getAction(editor, ITextEditorActionConstants.CUT));
			actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), getAction(editor, ITextEditorActionConstants.COPY));
			actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), getAction(editor, ITextEditorActionConstants.PASTE));
			actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
					getAction(editor, ITextEditorActionConstants.SELECT_ALL));
			actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), getAction(editor, ITextEditorActionConstants.FIND));
			actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(),
					getAction(editor, IDEActionFactory.BOOKMARK.getId()));
			actionBars.updateActionBars();
		}
		else if (actionBars != null && editor != null) {
			AbstractTestEditorFormPage pg = (AbstractTestEditorFormPage) editor.getActivePageInstance();
			actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), new HistoryActionDelegate(pg, true));
			actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), new HistoryActionDelegate(pg, false));
			actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), null);
			actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), null);
			actionBars.updateActionBars();
		}
	}

	private void createActions() {
		sampleAction = new Action() {
			@Override
			public void run() {
				MessageDialog.openInformation(null, "AludraTest Visual Data Editor", "Sample Action Executed");
			}
		};
		sampleAction.setText("Sample Action");
		sampleAction.setToolTipText("Sample Action tool tip");
		sampleAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(IDE.SharedImages.IMG_OBJS_TASK_TSK));
	}

	@Override
	public void contributeToMenu(IMenuManager manager) {
		IMenuManager menu = new MenuManager("Editor &Menu");
		manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
		menu.add(sampleAction);
	}

	private class HistoryActionDelegate extends Action {

		private boolean undo;

		private AbstractTestEditorFormPage formPage;

		public HistoryActionDelegate(AbstractTestEditorFormPage formPage, boolean undo) {
			this.formPage = formPage;
			this.undo = undo;
		}

		private IAction getDelegate() {
			try {
				return formPage.getEditor().getSourceEditor()
						.getAction(undo ? ActionFactory.UNDO.getId() : ActionFactory.REDO.getId());
			}
			catch (Throwable t) {
				return new Action() {
				}; // dummy action e.g. during dispose
			}
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return getDelegate().getImageDescriptor();
		}

		@Override
		public String getActionDefinitionId() {
			return getDelegate().getActionDefinitionId();
		}

		@Override
		public String getDescription() {
			return getDelegate().getDescription();
		}

		@Override
		public int getAccelerator() {
			return getDelegate().getAccelerator();
		}

		@Override
		public ImageDescriptor getDisabledImageDescriptor() {
			return getDelegate().getDisabledImageDescriptor();
		}

		@Override
		public ImageDescriptor getHoverImageDescriptor() {
			return getDelegate().getHoverImageDescriptor();
		}

		@Override
		public String getId() {
			return getDelegate().getId();
		}

		@Override
		public String getText() {
			return getDelegate().getText();
		}

		@Override
		public String getToolTipText() {
			return getDelegate().getToolTipText();
		}

		@Override
		public boolean isEnabled() {
			return getDelegate().isEnabled();
		}

		@Override
		public void run() {
			getDelegate().run();
			formPage.refreshContents();
		}

		@Override
		public void runWithEvent(Event event) {
			getDelegate().runWithEvent(event);
			formPage.refreshContents();
		}

		@Override
		public void addPropertyChangeListener(IPropertyChangeListener listener) {
			getDelegate().addPropertyChangeListener(listener);
		}

		@Override
		public void removePropertyChangeListener(IPropertyChangeListener listener) {
			getDelegate().removePropertyChangeListener(listener);
		}
	}

}
