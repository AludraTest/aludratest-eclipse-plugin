package org.aludratest.eclipse.vde.internal.refactor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aludratest.eclipse.vde.internal.TestDataCore;
import org.aludratest.eclipse.vde.internal.model.AbstractModelNode;
import org.aludratest.eclipse.vde.internal.model.TestDataOperation;
import org.aludratest.eclipse.vde.internal.model.XmlTagNames;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
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
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.search.ui.text.Match;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

public class RenameFieldParticipant extends RenameParticipant {

	private IField field;

	private String newName;

	private Change change;

	@Override
	public boolean initialize(RefactoringProcessor processor, Object element, RefactoringArguments arguments) {
		if (element instanceof IField) {
			this.field = (IField) element;
			try {
				if (!UpdateDataClassRefactoring.isDataClass(field.getDeclaringType())) {
					return false;
				}
			}
			catch (JavaModelException e) {
				// whatever
				return false;
			}
		}
		else {
			return false;
		}

		RenameArguments renameArgs = (RenameArguments) arguments;
		newName = renameArgs.getNewName();
		if (!renameArgs.getUpdateReferences()) {
			// no update of references - ok...
			return false;
		}

		return super.initialize(processor, element, arguments);
	}

	@Override
	protected boolean initialize(Object element) {
		return true;
	}

	@Override
	public String getName() {
		return "Update field name in Test Data XMLs";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {
		if (field == null || newName == null) {
			return new RefactoringStatus();
		}

		pm.beginTask("Searching for Test Data references to this field", 100);

		// find test data XML files referencing to enclosing Java type
		IType javaType = field.getDeclaringType();
		final String javaTypeName = javaType.getFullyQualifiedName();
		final String fieldName = field.getElementName();

		IJavaSearchScope searchScope = SearchEngine
				.createJavaSearchScope(new IJavaElement[] { javaType.getJavaProject() }, false);
		ElementQuerySpecification spec = new ElementQuerySpecification(javaType, JavaQueryParticipant.S_LIMIT_ALL, searchScope,
				"Project containing class");

		SubProgressMonitor spm = new SubProgressMonitor(pm, 80);
		try {
			SearchRequestor req = new SearchRequestor();
			new JavaQueryParticipant().search(req, spec, spm);

			List<Change> changes = new ArrayList<Change>();

			// take each matching file, find references to this field, create changes
			for (final IFile file : req.matches.keySet()) {
				try {
					List<Match> matches = TestDataCore.performTestDataOperation(file, new TestDataOperation<List<Match>>() {
						@Override
						public boolean isEdit() {
							return false;
						}

						@Override
						public List<Match> perform(ITestData testData) {
							List<Match> result = new ArrayList<Match>();

							Set<String> matchingSegments = new HashSet<String>();

							// find field in metadata
							for (ITestDataSegmentMetadata segment : testData.getMetaData().getSegments()) {
								if (javaTypeName.equals(segment.getDataClassName())) {
									for (ITestDataFieldMetadata fieldMeta : segment.getFields()) {
										if (fieldName.equals(fieldMeta.getName()) && (fieldMeta instanceof AbstractModelNode)) {
											result.add(new ModelNodeAttributeValueMatch(file, (AbstractModelNode) fieldMeta,
													XmlTagNames.NAME));
											matchingSegments.add(segment.getName());
										}
									}
								}
							}

							// find field in configuration segments
							for (ITestDataConfiguration config : testData.getConfigurations()) {
								for (ITestDataConfigurationSegment segment : config.getSegments()) {
									if (matchingSegments.contains(segment.getName())) {
										for (ITestDataFieldValue fieldValue : segment.getDefinedFieldValues()) {
											if (fieldName.equals(fieldValue.getFieldName())
													&& (fieldValue instanceof AbstractModelNode)) {
												result.add(new ModelNodeAttributeValueMatch(file, (AbstractModelNode) fieldValue,
														XmlTagNames.NAME));
											}
										}
									}
								}
							}

							return result;
						}
					});

					// build changes from matches for this file

					TextFileChange change = new TextFileChange("Update references in " + file.getName(), file);
					change.setEdit(new MultiTextEdit());
					for (Match match : matches) {
						ReplaceEdit edit = new ReplaceEdit(match.getOffset(), match.getLength(), newName);
						change.addEdit(edit);
					}

					if (!matches.isEmpty()) {
						changes.add(change);
					}
				}
				catch (IOException e) {
					// ignore file silently
				}
			}

			change = new CompositeChange("Update references in Test Data elements", changes.toArray(new Change[0]));
		}
		catch (CoreException e) {
			return RefactoringStatus.create(e.getStatus());
		}
		finally {
			pm.worked(20);
			pm.done();
		}

		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return change;
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
