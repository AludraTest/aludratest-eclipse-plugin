package org.aludratest.eclipse.vde.internal.refactor;

import java.io.IOException;
import java.util.regex.Pattern;

import org.aludratest.eclipse.vde.internal.TestDataCore;
import org.aludratest.eclipse.vde.internal.model.AbstractModelNode;
import org.aludratest.eclipse.vde.internal.model.TestDataOperation;
import org.aludratest.eclipse.vde.internal.model.XmlTagNames;
import org.aludratest.eclipse.vde.internal.util.PatternConstructor;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.jface.text.IRegion;
import org.eclipse.search.ui.text.Match;

/**
 * Searches references to Data classes in .testdata.xml files
 * 
 * @author falbrech
 * 
 */
public class JavaQueryParticipant implements IQueryParticipant {

	// the following are from JavaSearchPage (radio button indexes)
	static final int S_LIMIT_REF = 2;
	static final int S_LIMIT_ALL = 3;
	private static final int S_FOR_TYPES = 0;
	private static final int S_FOR_PACKAGES = 2;

	private ISearchRequestor fSearchRequestor;
	private Pattern fSearchPattern;
	private int fSearchFor = -1; // set since S_FOR_TYPES = 0;

	@Override
	public void search(ISearchRequestor requestor, QuerySpecification querySpecification, IProgressMonitor monitor)
			throws CoreException {
		if (querySpecification.getLimitTo() != S_LIMIT_REF && querySpecification.getLimitTo() != S_LIMIT_ALL) {
			return;
		}
		
		String search;
		if (querySpecification instanceof ElementQuerySpecification) {
			IJavaElement element = ((ElementQuerySpecification) querySpecification).getElement();
			if (element instanceof IType) {
				search = ((IType) element).getFullyQualifiedName('$');
			}
			else {
				search = element.getElementName();
			}
			int type = element.getElementType();
			if (type == IJavaElement.TYPE) {
				fSearchFor = S_FOR_TYPES;
			}
			else if (type == IJavaElement.PACKAGE_FRAGMENT || type == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
				fSearchFor = S_FOR_PACKAGES;
			}
		}
		else {
			fSearchFor = ((PatternQuerySpecification) querySpecification).getSearchFor();
			search = ((PatternQuerySpecification) querySpecification).getPattern();
		}
		if (fSearchFor != S_FOR_TYPES && fSearchFor != S_FOR_PACKAGES) {
			return;
		}
		fSearchPattern = PatternConstructor.createPattern(search, true);
		fSearchRequestor = requestor;

		IPath[] enclosingPaths = querySpecification.getScope().enclosingProjectsAndJars();
		monitor.beginTask("Searching for types and packages in Test Data files", enclosingPaths.length);

		for (IPath path : enclosingPaths) {
			if (monitor.isCanceled()) {
				return;
			}

			// a path in the workspace? Anything else is currently ignored (e.g. XMLs in JARs)
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			IResource res = ws.getRoot().findMember(path);
			if (res != null && (res.getType() == IResource.FOLDER || res.getType() == IResource.PROJECT)) {
				searchWorkspaceFolder((IContainer) res, monitor);
			}
			else {
				monitor.worked(1);
			}
		}
		monitor.done();
	}


	private void searchWorkspaceFolder(IContainer folder, IProgressMonitor monitor) {
		try {
			IResource[] children = folder.members();
			int ticks = children.length + 1;
			SubProgressMonitor spm = new SubProgressMonitor(monitor, 1);
			spm.beginTask("Searching folder " + folder.getName(), ticks);

			for (IResource res : children) {
				if (res.getType() == IResource.FOLDER) {
					searchWorkspaceFolder((IFolder) res, monitor);
				}
				else if (res.getType() == IResource.FILE) {
					// check file extension
					// TODO better: check only for XML, and then check XML content type
					IFile file = (IFile) res;
					if (file.getName().endsWith(".testdata.xml")) {
						searchTestdataFile(file);
					}
					spm.worked(1);
				}
				else {
					spm.worked(1);
				}
			}
			spm.worked(1); // the folder itself

			spm.done();
		}
		catch (CoreException e) {
			// ignore access error, e.g. stale path
			monitor.worked(1);
		}
	}

	private void searchTestdataFile(final IFile file) {
		try {
			// refresh file
			file.refreshLocal(1, new NullProgressMonitor());

			TestDataOperation<Void> op = new TestDataOperation<Void>() {
				@Override
				public Void perform(ITestData testData) {
					searchTestdata(file, testData);
					return null;
				}

				@Override
				public boolean isEdit() {
					return false;
				}
			};
			TestDataCore.performTestDataOperation(file, op);
		}
		catch (IOException e) {
			// ignore; silently return
			return;
		}
		catch (CoreException e) {
			// ignore; silently return
			return;
		}
	}

	private void searchTestdata(IFile file, ITestData testData) {
		for (ITestDataSegmentMetadata segment : testData.getMetaData().getSegments()) {
			if (matches(segment.getDataClassName()) && (segment instanceof AbstractModelNode)) {
				Match match = createMatch(file, (AbstractModelNode) segment, XmlTagNames.DATA_CLASS_NAME);
				fSearchRequestor.reportMatch(match);
			}

			for (ITestDataFieldMetadata field : segment.getFields()) {
				String subType = field.getSubTypeClassName();
				if (subType != null && matches(subType) && (field instanceof AbstractModelNode)) {
					Match match = createMatch(file, (AbstractModelNode) field, XmlTagNames.SUB_TYPE_CLASS_NAME);
					fSearchRequestor.reportMatch(match);
				}
			}
		}
	}

	private boolean matches(String fqClassName) {
		if (fSearchFor == S_FOR_PACKAGES) {
			String pkgName = fqClassName.substring(0, fqClassName.lastIndexOf('.'));
			return fSearchPattern.matcher(pkgName).matches();
		}
		else {
			return fSearchPattern.matcher(fqClassName).matches();
		}
	}

	private Match createMatch(Object element, AbstractModelNode node, String attributeName) {
		IRegion attrRegion = node.getAttributeValueRegion(attributeName);
		if (attrRegion != null) {
			return new ModelNodeAttributeValueMatch(element, node, attributeName);
		}

		return new Match(element, 1, 1);
	}

	@Override
	public int estimateTicks(QuerySpecification specification) {
		return 100; // same as PDE MANIFEST.MF search, which is similar
	}

	@Override
	public IMatchPresentation getUIParticipant() {
		return null;
	}

}
