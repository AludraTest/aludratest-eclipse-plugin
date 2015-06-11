package org.aludratest.eclipse.vde.internal.editors;

import java.util.ArrayList;
import java.util.List;

import org.aludratest.eclipse.vde.internal.TestDataCore;
import org.aludratest.eclipse.vde.internal.VdeImage;
import org.aludratest.eclipse.vde.internal.VdePlugin;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.Section;

public class SegmentsMasterDetailsBlock extends MasterDetailsBlock {

	private TreeViewer tvSegments;

	private SashForm sashForm;

	private IManagedForm managedForm;

	private SectionPart sectionPart;

	public void setInput(Object input) {
		tvSegments.setInput(input);

		// fire selection change in case of preserved selection, to update detail fields
		if (!tvSegments.getSelection().isEmpty()) {
			tvSegments.setSelection(tvSegments.getSelection());
		}
	}

	void selectSegment(ITestDataSegmentMetadata segment) {
		tvSegments.setSelection(new StructuredSelection(segment));
	}

	@Override
	protected void createMasterPart(final IManagedForm managedForm, Composite parent) {
		this.managedForm = managedForm;

		Section section = managedForm.getToolkit().createSection(parent,
				Section.EXPANDED | Section.DESCRIPTION | Section.TITLE_BAR);
		section.setText("All segments");
		section.setDescription("Select a segment to edit, or create a new segment.");
		sectionPart = new SectionPart(section);
		managedForm.addPart(sectionPart);

		Composite c = managedForm.getToolkit().createComposite(section);

		GridLayout gl = new GridLayout(2, false);
		gl.verticalSpacing = 4;
		c.setLayout(gl);

		// the sections treeviewer
		Tree tree = managedForm.getToolkit().createTree(c, SWT.BORDER | SWT.SINGLE);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tvSegments = new TreeViewer(tree);

		tvSegments.setContentProvider(new TestDataSegmentsContentProvider());
		tvSegments.setLabelProvider(new TestDataSegmentsLabelProvider());

		Composite cpoButtons = managedForm.getToolkit().createComposite(c);
		cpoButtons.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, true));

		gl = new GridLayout(1, false);
		gl.verticalSpacing = 8;
		cpoButtons.setLayout(gl);

		Button btnAdd = managedForm.getToolkit().createButton(cpoButtons, "Add...", SWT.PUSH);
		final Button btnRemove = managedForm.getToolkit().createButton(cpoButtons, "Delete", SWT.PUSH);
		
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddSegment();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemoveSegment();
			}
		});
		btnRemove.setEnabled(false);

		tvSegments.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				managedForm.fireSelectionChanged(sectionPart, event.getSelection());
				btnRemove.setEnabled(!event.getSelection().isEmpty());
			}
		});

		section.setClient(c);
	}

	@Override
	protected void applyLayoutData(SashForm sashForm) {
		super.applyLayoutData(sashForm);
		this.sashForm = sashForm;
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		final IDetailsPage pg = new SegmentDetailsFormPart(this);

		detailsPart.setPageProvider(new IDetailsPageProvider() {
			@Override
			public Object getPageKey(Object object) {
				return (object instanceof ITestDataSegmentMetadata ? "segment" : null);
			}

			@Override
			public IDetailsPage getPage(Object key) {
				return "segment".equals(key) ? pg : null;
			}
		});
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		// safe to be called here because children have been added
		sashForm.setWeights(new int[] { 33, 67 });
	}

	private TestDataEditor getEditor() {
		return ((TestDataMetadataPage) managedForm.getContainer()).getEditor();
	}

	private void handleAddSegment() {
		// TODO when "list" segment selected, propose new list entry (mysegment-(N+1))

		ITestDataMetadata metadata = ((ITestData) tvSegments.getInput()).getMetaData();
		NewSegmentDialog dlg = new NewSegmentDialog(getEditor(), metadata);
		if (dlg.open() == NewSegmentDialog.OK) {
			ITestDataSegmentMetadata segment = createSegment(metadata, dlg.getSegmentName(), dlg.getClassName(),
					dlg.getDataClassType());
			tvSegments.setSelection(new StructuredSelection(segment));
		}
	}

	private ITestDataSegmentMetadata createSegment(ITestDataMetadata metadata, String segmentName, String className,
			IType dataType) {
		metadata.addSegment(segmentName, className);
		tvSegments.refresh();
		ITestDataSegmentMetadata[] segments = metadata.getSegments();
		ITestDataSegmentMetadata segment = segments[segments.length - 1];

		if (dataType != null) {
			try {
				for (IField field : dataType.getFields()) {
					if (TestDataCore.isProperty(field)) {
						segment.addField();
						ITestDataFieldMetadata[] metaFields = segment.getFields();
						ITestDataFieldMetadata metaField = metaFields[metaFields.length - 1];
						metaField.setName(field.getElementName());

						TestDataCore.applyFieldType(field, metaField);
						// create a subsegment?
						if (metaField.getSubTypeClassName() != null) {
							String subSegmentName = segmentName + "." + metaField.getName();
							if (metaField.getType() == TestDataFieldType.OBJECT_LIST) {
								subSegmentName += "-1";
							}

							createSegment(metadata, subSegmentName, metaField.getSubTypeClassName(),
									tryFindType(metaField.getSubTypeClassName()));
						}
					}
				}

			}
			catch (JavaModelException e) {
				VdePlugin.getDefault().logException("Could not enumerate data class fields", e);
			}
		}
		return segment;
	}

	private IType tryFindType(String className) {
		IFileEditorInput fileInput = (IFileEditorInput) getEditor().getEditorInput();
		IFile file = fileInput.getFile();
		IJavaProject project = JavaCore.create(file.getProject());
		try {
			return project.findType(className);
		}
		catch (JavaModelException e) {
			return null;
		}
	}

	private void handleRemoveSegment() {
		IStructuredSelection selection = (IStructuredSelection) tvSegments.getSelection();
		if (!selection.isEmpty()) {
			ITestDataSegmentMetadata segment = (ITestDataSegmentMetadata) selection.getFirstElement();
			ITestDataMetadata meta = ((ITestData) tvSegments.getInput()).getMetaData();

			// remove ALL segments starting with the segment's name and a dot, or equalling the name
			String subNameStart = segment.getName() + ".";
			boolean found;
			do {
				found = false;
				for (ITestDataSegmentMetadata seg : meta.getSegments()) {
					if (seg.getName().startsWith(subNameStart)) {
						meta.removeSegment(seg);
						found = true;
						break;
					}
				}
			}
			while (found);
			meta.removeSegment(segment);

			tvSegments.refresh();
		}
	}


	private static class TestDataSegmentsContentProvider implements ITreeContentProvider {

		private ITestData testData;

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.testData = (ITestData) newInput;

			// TODO perhaps build internal tree structure here
		}

		@Override
		public Object[] getElements(Object inputElement) {
			ITestData data = (ITestData) inputElement;

			// collect only "root" segments
			List<ITestDataSegmentMetadata> segments = new ArrayList<ITestDataSegmentMetadata>();
			for (ITestDataSegmentMetadata segment : data.getMetaData().getSegments()) {
				if (!segment.getName().contains(".")) {
					segments.add(segment);
				}
			}

			return segments.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof ITestDataSegmentMetadata)) {
				return null;
			}

			String expectedStart = ((ITestDataSegmentMetadata) parentElement).getName() + ".";
			List<ITestDataSegmentMetadata> segments = new ArrayList<ITestDataSegmentMetadata>();

			for (ITestDataSegmentMetadata segment : testData.getMetaData().getSegments()) {
				if (segment.getName().startsWith(expectedStart)
						&& !segment.getName().substring(expectedStart.length()).contains(".")) {
					segments.add(segment);
				}
			}

			return segments.toArray();
		}

		@Override
		public Object getParent(Object element) {
			if (!(element instanceof ITestDataSegmentMetadata)) {
				return null;
			}

			ITestDataSegmentMetadata segment = (ITestDataSegmentMetadata) element;
			String name = segment.getName();

			if (name == null || !name.contains(".")) {
				return null;
			}

			String searchName = name.substring(0, name.indexOf('.'));

			for (ITestDataSegmentMetadata s : testData.getMetaData().getSegments()) {
				if (s.getName().equals(searchName)) {
					return s;
				}
			}

			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			// unoptimized
			Object[] objs = getChildren(element);
			return objs != null && objs.length > 0;
		}
	}

	private static class TestDataSegmentsLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return VdeImage.SEGMENT.getImage();
		}

		@Override
		public String getText(Object element) {
			ITestDataSegmentMetadata meta = (ITestDataSegmentMetadata) element;

			String name = meta.getName();
			if (name.contains(".")) {
				name = name.substring(name.lastIndexOf('.') + 1);
			}

			// TODO perhaps include class name somehow? Tooltip?
			return name;
		}

	}

}
