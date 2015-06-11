package org.aludratest.eclipse.vde.internal.editors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.aludratest.eclipse.vde.internal.VdeImage;
import org.aludratest.eclipse.vde.internal.VdePlugin;
import org.aludratest.eclipse.vde.internal.script.FunctionLibrary;
import org.aludratest.eclipse.vde.internal.script.ScriptCategory;
import org.aludratest.eclipse.vde.internal.script.ScriptFunction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

public class ScriptFunctionDialog extends Dialog {

	private TreeViewer tvFunctions;

	private Label lblDescription;

	private FunctionLibrary functionLibrary;

	private ScriptFunction selectedFunction;

	public ScriptFunctionDialog(Shell parentShell) {
		super(parentShell);
		
		// load the function library for current Locale; use _en if not found
		Locale lc = VdePlugin.getDefault().getCurrentLocale();
		try {
			functionLibrary = FunctionLibrary.load(lc);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		c.setLayout(new GridLayout(1, false));

		Tree tree = new Tree(c, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 300;
		gd.widthHint = 200;
		tree.setLayoutData(gd);

		tvFunctions = new TreeViewer(tree);
		tvFunctions.setContentProvider(new ScriptFunctionTreeContentProvider());
		tvFunctions.setLabelProvider(new ScriptFunctionTreeLabelProvider(tree.getFont()));
		tvFunctions.setInput(functionLibrary);
		tvFunctions.expandAll();

		Composite cpoDesc = new Composite(c, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.heightHint = 80;
		cpoDesc.setLayoutData(gd);
		FillLayout fl = new FillLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		cpoDesc.setLayout(fl);

		lblDescription = new Label(cpoDesc, SWT.LEFT | SWT.WRAP);

		tvFunctions.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object sel = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (sel instanceof ScriptFunction) {
					lblDescription.setText(((ScriptFunction) sel).getDescription());
				}
				else {
					lblDescription.setText("");
				}

			}
		});
		tvFunctions.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object sel = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (sel instanceof ScriptFunction) {
					selectedFunction = (ScriptFunction) sel;
					close();
				}
			}
		});

		getShell().setText("Select Script Function");

		return super.createDialogArea(parent);
	}

	@Override
	protected void okPressed() {
		Object sel = ((IStructuredSelection) tvFunctions.getSelection()).getFirstElement();
		if (sel instanceof ScriptFunction) {
			selectedFunction = (ScriptFunction) sel;
		}

		super.okPressed();
	}

	public ScriptFunction getSelectedFunction() {
		return selectedFunction;
	}

	private static class ScriptFunctionTreeContentProvider implements ITreeContentProvider {

		private Map<ScriptFunction, ScriptCategory> functionToCategory = new HashMap<ScriptFunction, ScriptCategory>();

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			functionToCategory.clear();
			if (newInput != null) {
				FunctionLibrary lib = (FunctionLibrary) newInput;
				for (ScriptCategory category : lib.getCategories()) {
					for (ScriptFunction fn : category.getFunctions()) {
						functionToCategory.put(fn, category);
					}
				}
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			FunctionLibrary library = (FunctionLibrary) inputElement;
			return library.getCategories().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ScriptCategory) {
				return ((ScriptCategory) parentElement).getFunctions().toArray();
			}

			return null;
		}

		@Override
		public Object getParent(Object element) {
			return functionToCategory.get(element);
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof ScriptCategory;
		}

	}

	private static class ScriptFunctionTreeLabelProvider extends LabelProvider implements IFontProvider {

		private Font defaultFont;

		private Font boldFont;

		public ScriptFunctionTreeLabelProvider(Font defaultFont) {
			this.defaultFont = defaultFont;
			boldFont = new Font(defaultFont.getDevice(), defaultFont.getFontData()[0].getName(),
					defaultFont.getFontData()[0].getHeight(), defaultFont.getFontData()[0].getStyle() | SWT.BOLD);
		}

		@Override
		public void dispose() {
			boldFont.dispose();
			super.dispose();
		}

		@Override
		public Font getFont(Object element) {
			return (element instanceof ScriptCategory) ? boldFont : defaultFont;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof ScriptCategory) {
				return ((ScriptCategory) element).getName();
			}
			else if (element instanceof ScriptFunction) {
				return ((ScriptFunction) element).getName();
			}

			return super.getText(element);
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof ScriptFunction) {
				return VdeImage.FUNCTION.getImage();
			}

			return super.getImage(element);
		}
	}

}
