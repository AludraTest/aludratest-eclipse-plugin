package org.aludratest.eclipse.vde.internal.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.editor.FormPage;

public abstract class AbstractTestEditorFormPage extends FormPage {

	public AbstractTestEditorFormPage(TestDataEditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	public TestDataEditor getEditor() {
		return (TestDataEditor) super.getEditor();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		IActionBars actionBars = site.getActionBars();

		// hook undo; map to source editor
		actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), new HistoryActionDelegate(true));
		actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), new HistoryActionDelegate(false));
		actionBars.updateActionBars();
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	protected abstract void refreshContents();

	private class HistoryActionDelegate extends Action {

		private boolean undo;

		public HistoryActionDelegate(boolean undo) {
			this.undo = undo;
		}

		private IAction getDelegate() {
			try {
				return getEditor().getSourceEditor().getAction(undo ? ActionFactory.UNDO.getId() : ActionFactory.REDO.getId());
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
			refreshContents();
		}

		@Override
		public void runWithEvent(Event event) {
			getDelegate().runWithEvent(event);
			refreshContents();
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
