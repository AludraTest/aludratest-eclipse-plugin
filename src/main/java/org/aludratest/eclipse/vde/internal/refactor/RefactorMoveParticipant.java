package org.aludratest.eclipse.vde.internal.refactor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

public class RefactorMoveParticipant extends MoveParticipant {

	private UpdateDataClassRefactoring refactoring = new UpdateDataClassRefactoring();

	@Override
	public boolean initialize(RefactoringProcessor processor, Object element, RefactoringArguments arguments) {
		return refactoring.initialize(processor, element, arguments);
	}

	@Override
	public String getName() {
		return refactoring.getName();
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {
		return refactoring.checkConditions(pm, context);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return refactoring.createChange(pm);
	}

	@Override
	public Change createPreChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return refactoring.createPreChange(pm);
	}

	@Override
	public TextChange getTextChange(Object element) {
		return refactoring.getTextChange(element);
	}

	@Override
	protected boolean initialize(Object element) {
		return refactoring.initialize(element);
	}

}
