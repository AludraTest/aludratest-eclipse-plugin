package org.aludratest.eclipse.vde.internal.editors;

import java.util.ArrayList;
import java.util.List;

import org.aludratest.eclipse.vde.internal.VdeImage;
import org.aludratest.eclipse.vde.internal.model.TestDataConfiguration;
import org.aludratest.eclipse.vde.internal.model.TestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.Section;

public class ConfigurationSegmentsMasterDetailsBlock extends MasterDetailsBlock implements SegmentSelectable {

	private TableViewer tvSegments;

	private SashForm sashForm;

	private IManagedForm managedForm;

	private SectionPart sectionPart;

	@Override
	protected void createMasterPart(final IManagedForm managedForm, Composite parent) {
		this.managedForm = managedForm;

		Section section = managedForm.getToolkit().createSection(parent,
				Section.EXPANDED | Section.DESCRIPTION | Section.TITLE_BAR);
		section.setText("All segments");
		section.setDescription("Select a test data segment to edit.");
		sectionPart = new SectionPart(section);
		managedForm.addPart(sectionPart);

		Composite c = managedForm.getToolkit().createComposite(section);
		c.setLayout(new GridLayout(1, false));

		// the sections tableviewer
		Table table = managedForm.getToolkit().createTable(c, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		table.setLayoutData(gd);

		Menu contextMenu = new Menu(table);
		final MenuItem mnuDelete = new MenuItem(contextMenu, SWT.PUSH);
		mnuDelete.setText("Remove Segment");
		mnuDelete.setImage(VdeImage.DELETE.getImage());
		contextMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				// get selected segment; only enable if segment is red
				ITestDataConfigurationSegment segment = (ITestDataConfigurationSegment) ((IStructuredSelection) tvSegments
						.getSelection()).getFirstElement();
				if (segment != null && (segment instanceof TestDataConfigurationSegment)
						&& ((TestDataConfigurationSegment) segment).isNotReferencedInMetadata(false)) {
					mnuDelete.setEnabled(true);
				}
				else {
					mnuDelete.setEnabled(false);
				}
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		mnuDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemoveSegment();
			}
		});
		table.setMenu(contextMenu);

		tvSegments = new TableViewer(table);

		tvSegments.setContentProvider(new TestDataConfigSegmentContentProvider());
		tvSegments.setLabelProvider(new TestDataConfigSegmentLabelProvider());
		tvSegments.setComparator(new SegmentsViewerComparator());

		tvSegments.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				managedForm.fireSelectionChanged(sectionPart, event.getSelection());
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
		final IDetailsPage pg = new ConfigurationSegmentDetailsFormPart(this);

		detailsPart.setPageProvider(new IDetailsPageProvider() {
			@Override
			public Object getPageKey(Object object) {
				return (object instanceof ITestDataConfigurationSegment ? "segment" : null);
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

	public void setInput(Object input) {
		tvSegments.setInput(input);

		// fire selection change in case of preserved selection, to update detail fields
		if (!tvSegments.getSelection().isEmpty()) {
			tvSegments.setSelection(tvSegments.getSelection());
		}
	}

	ITestDataConfiguration getTestDataConfiguration() {
		return (ITestDataConfiguration) tvSegments.getInput();
	}

	@Override
	public boolean selectSegment(String segmentName) {
		for (ITestDataConfigurationSegment segment : getTestDataConfiguration().getSegments()) {
			if (segment.getName().equals(segmentName)) {
				tvSegments.setSelection(new StructuredSelection(segment));
				return true;
			}
		}
		return false;
	}

	@Override
	public ITestData getTestDataModel() {
		return getEditor().getTestDataModel();
	}

	void refreshSegmentsList() {
		tvSegments.refresh();
	}

	private TestDataEditor getEditor() {
		return ((VisualDataEditorPage) managedForm.getContainer()).getEditor();
	}

	private void handleRemoveSegment() {
		ITestDataConfigurationSegment segment = (ITestDataConfigurationSegment) ((IStructuredSelection) tvSegments.getSelection())
				.getFirstElement();
		if (segment != null && (segment instanceof TestDataConfigurationSegment)
				&& ((TestDataConfigurationSegment) segment).isNotReferencedInMetadata(false)) {
			((TestDataConfiguration) getTestDataConfiguration()).removeSegment(segment);
			tvSegments.refresh();
			tvSegments.setSelection(new StructuredSelection());
		}
	}

	private static class TestDataConfigSegmentContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			ITestDataConfiguration config = (ITestDataConfiguration) inputElement;
			return config.getSegments();
		}

	}

	private static class TestDataConfigSegmentLabelProvider extends LabelProvider implements ITableLabelProvider,
			ITableColorProvider {

		private Color redColor;

		public TestDataConfigSegmentLabelProvider() {
			redColor = new Color(PlatformUI.getWorkbench().getDisplay(), 255, 0, 0);
		}

		@Override
		public void dispose() {
			redColor.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				return VdeImage.SEGMENT.getImage();
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0) {
				ITestDataConfigurationSegment segment = (ITestDataConfigurationSegment) element;
				return segment.getName();
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if ((element instanceof TestDataConfigurationSegment)
					&& ((TestDataConfigurationSegment) element).isNotReferencedInMetadata(true)) {
				return redColor;
			}

			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

	}

	private class SegmentsViewerComparator extends ViewerComparator {

		private List<String> metadataSegmentOrder = new ArrayList<String>();

		@Override
		public void sort(Viewer viewer, Object[] elements) {
			ITestDataMetadata metadata = getTestDataModel().getMetaData();
			metadataSegmentOrder.clear();

			for (ITestDataSegmentMetadata seg : metadata.getSegments()) {
				metadataSegmentOrder.add(seg.getName());
			}

			super.sort(viewer, elements);
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if ((e1 instanceof ITestDataConfigurationSegment) && (e2 instanceof ITestDataConfigurationSegment)) {
				String s1 = ((ITestDataConfigurationSegment) e1).getName();
				String s2 = ((ITestDataConfigurationSegment) e2).getName();

				return metadataSegmentOrder.indexOf(s1) - metadataSegmentOrder.indexOf(s2);
			}

			return super.compare(viewer, e1, e2);
		}
	}

}
