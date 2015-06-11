package org.aludratest.eclipse.vde.internal.refactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aludratest.dict.Data;
import org.aludratest.eclipse.vde.internal.TestDataCore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.search.ui.text.Match;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

public class UpdateDataClassRefactoring extends RefactoringParticipant {

	private IType javaType;

	private String newName;

	private Change change;

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IType) {
			this.javaType = (IType) element;
		}
		return true;
	}

	@Override
	protected void initialize(RefactoringArguments arguments) {
		// all done in 3-param method
	}

	@Override
	public boolean initialize(RefactoringProcessor processor, Object element, RefactoringArguments arguments) {
		if (arguments instanceof RenameArguments) {
			RenameArguments renameArgs = (RenameArguments) arguments;
			newName = renameArgs.getUpdateReferences() ? renameArgs.getNewName() : null;

			// also add package name
			if (newName != null && (element instanceof IType)) {
				newName = ((IType) element).getPackageFragment().getElementName() + "." + newName;
			}
		}
		else if (arguments instanceof MoveArguments) {
			MoveArguments moveArgs = (MoveArguments) arguments;
			if (moveArgs.getUpdateReferences()) {
				Object target = moveArgs.getDestination();
				if ((target instanceof IPackageFragment) && (element instanceof IType)) {
					newName = ((IPackageFragment) target).getElementName() + "." + ((IType) element).getElementName();
				}
			}
		}

		return super.initialize(processor, element, arguments);
	}

	@Override
	public String getName() {
		return "Update Data Class reference in XML Test Data";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {
		if (javaType == null || newName == null) {
			return new RefactoringStatus();
		}

		// check class for being a Data class
		try {
			if (!isDataClass(javaType)) {
				return new RefactoringStatus();
			}
		}
		catch (JavaModelException e) {
			return new RefactoringStatus(); // cannot check class for being Data class for whatever reason
		}

		pm.beginTask("Searching for Test Data references to this class", 100);
		// find test data XML files referencing to this Java type

		IJavaSearchScope searchScope = SearchEngine
				.createJavaSearchScope(new IJavaElement[] { javaType.getJavaProject() }, false);
		ElementQuerySpecification spec = new ElementQuerySpecification(javaType, JavaQueryParticipant.S_LIMIT_ALL, searchScope,
				"Project containing class");

		SubProgressMonitor spm = new SubProgressMonitor(pm, 95);
		try {
			SearchRequestor req = new SearchRequestor();
			new JavaQueryParticipant().search(req, spec, spm);

			// build refactorings from matches
			List<Change> changes = new ArrayList<Change>();

			for (Map.Entry<IFile, List<Match>> entry : req.matches.entrySet()) {
				IFile file = entry.getKey();
				TextFileChange change = new TextFileChange("Update references in " + file.getName(), file);
				change.setEdit(new MultiTextEdit());
				for (Match match : entry.getValue()) {
					if (match instanceof ModelNodeAttributeValueMatch) {
						// still, only replace its match by the new class name (no XML operations)
						ReplaceEdit edit = new ReplaceEdit(match.getOffset(), match.getLength(), newName);
						change.addEdit(edit);
					}
				}
				if (change.getEdit().hasChildren()) {
					changes.add(change);
				}
			}

			change = new CompositeChange("Update references in Test Data elements", changes.toArray(new Change[0]));
		}
		catch (CoreException e) {
			return RefactoringStatus.create(e.getStatus());
		}
		finally {
			pm.worked(5);
			pm.done();
		}

		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return change;
	}

	static boolean isDataClass(IType javaType) throws JavaModelException {
		if (javaType.getFullyQualifiedName().equals(Data.class.getName())) {
			return true;
		}

		IJavaProject project = javaType.getJavaProject();
		IJavaSearchScope scope = SearchEngine.createHierarchyScope(TestDataCore.findDataClass(project.getResource()));
		return scope.encloses(javaType);
	}

	private static class SearchRequestor implements ISearchRequestor {

		private Map<IFile, List<Match>> matches = new HashMap<IFile, List<Match>>();

		@Override
		public void reportMatch(Match match) {
			Object element = match.getElement();
			if (element instanceof IFile) {
				List<Match> ls = matches.get(element);
				if (ls == null) {
					matches.put((IFile) element, ls = new ArrayList<Match>());
				}
				ls.add(match);
			}
		}
	}
}
