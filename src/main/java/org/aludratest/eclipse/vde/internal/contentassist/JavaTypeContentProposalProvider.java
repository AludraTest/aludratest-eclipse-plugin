package org.aludratest.eclipse.vde.internal.contentassist;

import java.util.ArrayList;
import java.util.List;

import org.aludratest.eclipse.vde.internal.TestDataCore;
import org.aludratest.eclipse.vde.internal.VdePlugin;
import org.aludratest.eclipse.vde.internal.editors.TestDataEditor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

public class JavaTypeContentProposalProvider extends LabelProvider implements IContentProposalProvider {

	private IJavaProject javaProject;

	private IType baseType;

	public JavaTypeContentProposalProvider(IResource projectResource, String baseTypeName) {
		javaProject = JavaCore.create(projectResource.getProject());
		baseType = TestDataCore.findClass(projectResource, baseTypeName);
	}

	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		if (baseType == null) {
			return new IContentProposal[] { new ContentProposal("",
					"No AludraTest found on classpath. Please check your project configuration.", null) };
		}

		final List<IContentProposal> proposals = new ArrayList<IContentProposal>();
		TypeNameMatchRequestor requestor = new TypeNameMatchRequestor() {
			@Override
			public void acceptTypeNameMatch(TypeNameMatch match) {
				IType javaType = match.getType();
				try {
					if (!Flags.isAbstract(javaType.getFlags())) {
						String pkgName = javaType.getPackageFragment().getElementName();
						proposals.add(new ContentProposal(javaType.getFullyQualifiedName(), javaType.getElementName(), pkgName));
					}
				}
				catch (JavaModelException e) {
					VdePlugin.getDefault().logException("Could not query flags of class " + javaType.getFullyQualifiedName(), e);
				}
			}
		};

		try {
			IJavaSearchScope searchScope = SearchEngine.createHierarchyScope(baseType);
			new SearchEngine().searchAllTypeNames(null, 0, contents.toCharArray(), SearchPattern.R_CAMELCASE_MATCH,
					IJavaSearchConstants.CLASS, searchScope, requestor, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
					new NullProgressMonitor());
		}
		catch (JavaModelException e) {
			VdePlugin.getDefault().logException("Could not determine proposals for segment Data class", e);
			return new IContentProposal[0];
		}
		
		return proposals.toArray(new IContentProposal[0]);
	}

	@Override
	public Image getImage(Object element) {
		try {
			if ((element instanceof ContentProposal) && baseType != null) {
				IType javaType = javaProject.findType(((ContentProposal) element).getContent());
				if (javaType != null) {
					return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
				}
			}
			return super.getImage(element);
		}
		catch (JavaModelException e) {
			return super.getImage(element);
		}
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ContentProposal) {
			ContentProposal cp = (ContentProposal) element;
			return cp.getLabel() + (cp.getDescription() != null ? " - " + cp.getDescription() : "");
		}

		return super.getText(element);
	}

	public static void attachToComponents(final Text txtClass, Button btnBrowse, final IResource projectResource,
			final String browseMessagePrompt, final String baseClassName, final String defaultFilter) {
		// dispose previous, if any
		ContentProposalAdapter previous = (ContentProposalAdapter) txtClass.getData(JavaTypeContentProposalProvider.class.getName());
		if (previous != null) {
		}
		
		FieldDecoration fdec = FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);

		ControlDecoration cdec = new ControlDecoration(txtClass, SWT.TOP | SWT.LEFT);
		cdec.setImage(fdec.getImage());
		cdec.setDescriptionText(fdec.getDescription());
		cdec.setShowOnlyOnFocus(true);

		try {
			JavaTypeContentProposalProvider provider = new JavaTypeContentProposalProvider(projectResource, baseClassName);
			ContentProposalAdapter adapter = new ContentProposalAdapter(txtClass, new TextContentAdapter() {
				@Override
				public void insertControlContents(Control control, String text, int cursorPosition) {
					((Text) control).setText(text);
				}
			}, provider, KeyStroke.getInstance("Ctrl+Space"), null);
			// decorate every entry with the Java Class symbol
			adapter.setLabelProvider(provider);
			adapter.setPopupSize(new Point(450, 200));
		}
		catch (ParseException pe) {
			throw new RuntimeException(pe);
		}
		
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String initialValue = txtClass.getText();
				if (initialValue == null || "".equals(initialValue)) {
					initialValue = defaultFilter;
				}
				String newClass = browseForClass(txtClass.getShell(), PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
						projectResource, initialValue, browseMessagePrompt, baseClassName);
				if (newClass != null) {
					txtClass.setText(newClass);
				}
			}
		});
	}
	
	public static void attachToComponents(final Text txtClass, Button btnBrowse, final TestDataEditor editor,
			final String browseMessagePrompt, String baseClassName) {
		attachToComponents(txtClass, btnBrowse, ((IFileEditorInput) editor.getEditorInput()).getFile(), browseMessagePrompt,
				baseClassName, "*Data");
	}

	public static String browseForClass(Shell shell, IRunnableContext runnableContext, IResource projectResource,
			String initialFilter, String messagePrompt, String baseClassName) {
		if (initialFilter == null || "".equals(initialFilter)) {
			initialFilter = "*Data";
		}
		else {
			if (initialFilter.contains(".")) {
				initialFilter = initialFilter.substring(initialFilter.lastIndexOf('.') + 1);
			}
		}

		try {
			IType dataClass = TestDataCore.findClass(projectResource, baseClassName);
			IJavaSearchScope searchScope = SearchEngine.createHierarchyScope(dataClass);
			SelectionDialog dlg = JavaUI.createTypeDialog(shell, runnableContext, searchScope,
					IJavaElementSearchConstants.CONSIDER_CLASSES, false, initialFilter);
			dlg.setTitle("Select class");
			dlg.setMessage(messagePrompt);
			if (dlg.open() == SelectionDialog.OK) {
				Object[] result = dlg.getResult();
				if (result != null && result.length > 0) {
					IType tp = (IType) result[0];
					// additional check as sometimes the Dialog shows ALL classes...
					if (searchScope.encloses(tp)) {
						return tp.getFullyQualifiedName();
					}
				}
			}
		}
		catch (JavaModelException e) {
			VdePlugin.getDefault().logException("Could not open type selection dialog", e);
		}

		return null;
	}

}
