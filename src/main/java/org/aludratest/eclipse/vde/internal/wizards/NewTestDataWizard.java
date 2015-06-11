package org.aludratest.eclipse.vde.internal.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.aludratest.eclipse.vde.internal.TestDataCore;
import org.aludratest.eclipse.vde.internal.VdePlugin;
import org.aludratest.eclipse.vde.internal.model.TestDataOperation;
import org.aludratest.eclipse.vde.internal.util.ArrayUtil;
import org.aludratest.eclipse.vde.internal.util.SignatureUtil;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.aludratest.testcase.data.Source;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class NewTestDataWizard extends Wizard implements INewWizard {
	private NewTestDataWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for NewTestDataWizard.
	 */
	public NewTestDataWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	@Override
	public void addPages() {
		page = new NewTestDataWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		final IType initType = page.getInitializeFromClass();

		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, initType, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */
	private void doFinish(String containerName, String fileName, IType initializationClass, IProgressMonitor monitor)
			throws CoreException {
		monitor.beginTask("Creating " + fileName, 3);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(fileName));
		try {
			String encoding = container.getDefaultCharset();
			// translate encoding into XML compatible
			if (encoding == null) {
				encoding = "UTF-8";
			}
			if ("Cp1252".equalsIgnoreCase(encoding)) {
				encoding = "ISO-8859-1";
			}
			InputStream stream = openContentStream(encoding, "\n");
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		monitor.worked(1);
		if (initializationClass != null) {
			monitor.setTaskName("Initializing contents from class " + initializationClass.getElementName() + "...");

			try {
				initializeFromClass(file, initializationClass);
			}
			catch (IOException e) {
				throw new CoreException(new Status(Status.ERROR, VdePlugin.PLUGIN_ID,
						"Could not write initial contents to test data file"));
			}
		}
		monitor.worked(1);

		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}
	
	private void initializeFromClass(IFile file, final IType initializationClass) throws CoreException, IOException {
		final Map<String, String> segmentsToCreate = new LinkedHashMap<String, String>();

		// find public non-static methods
		for (IMethod method : initializationClass.getMethods()) {
			int flags = method.getFlags();
			if (!Flags.isPublic(flags) || Flags.isAbstract(flags) || Flags.isStatic(flags)) {
				continue;
			}

			ILocalVariable[] params = method.getParameters();
			for (ILocalVariable param : params) {
				IAnnotation srcAnnot = findSourceAnnotation(param);
				if (srcAnnot != null) {
					String segmentName = extractAnnotationParamValue(srcAnnot, "segment");
					String javaType = SignatureUtil.getFullyQualifiedSignatureType(initializationClass.getTypeRoot(),
							param.getTypeSignature());
					if (segmentName != null && javaType != null) {
						segmentsToCreate.put(segmentName, javaType);
					}
				}
			}
		}

		TestDataCore.performTestDataOperation(file, new TestDataOperation<Void>() {
			@Override
			public boolean isEdit() {
				return true;
			}

			@Override
			public Void perform(ITestData testData) {
				ITestDataMetadata metadata = testData.getMetaData();

				for (Map.Entry<String, String> segment : segmentsToCreate.entrySet()) {
					metadata.addSegment(segment.getKey(), segment.getValue());
					ITestDataSegmentMetadata seg = ArrayUtil.lastElement(metadata.getSegments());

					// try to lookup data class
					try {
						IType dataClass = initializationClass.getJavaProject().findType(segment.getValue());
						if (dataClass != null) {
							syncToType(metadata, seg, dataClass);
						}
					}
					catch (JavaModelException e) {
					}
				}

				return null;
			}
		});
	}

	private void syncToType(ITestDataMetadata metadata, ITestDataSegmentMetadata segment, IType dataType)
			throws JavaModelException {
		String segmentName = segment.getName();

		for (IField field : dataType.getFields()) {
			if (TestDataCore.isProperty(field)) {
				segment.addField();
				ITestDataFieldMetadata metaField = ArrayUtil.lastElement(segment.getFields());
				metaField.setName(field.getElementName());

				TestDataCore.applyFieldType(field, metaField);
				// create a subsegment?
				String subTypeClassName = metaField.getSubTypeClassName();
				if (subTypeClassName != null) {
					String subSegmentName = segmentName + "." + metaField.getName();
					if (metaField.getType() == TestDataFieldType.OBJECT_LIST) {
						subSegmentName += "-1";
					}

					metadata.addSegment(subSegmentName, subTypeClassName);

					// try to resolve class name
					try {
						IType subDataType = dataType.getJavaProject().findType(subTypeClassName);
						if (subDataType != null) {
							ITestDataSegmentMetadata seg = ArrayUtil.lastElement(metadata.getSegments());
							syncToType(metadata, seg, subDataType);
						}
					}
					catch (JavaModelException e) {
					}
				}
			}
		}

	}

	private IAnnotation findSourceAnnotation(ILocalVariable param) throws JavaModelException {
		for (IAnnotation annot : param.getAnnotations()) {
			String nm = annot.getElementName();
			if ("Source".equals(nm) || Source.class.getName().equals(nm)) {
				return annot;
			}
		}

		return null;
	}

	private String extractAnnotationParamValue(IAnnotation annotation, String key) throws JavaModelException {
		for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
			if (pair.getValueKind() == IMemberValuePair.K_STRING && key.equals(pair.getMemberName())) {
				Object o = pair.getValue();
				return o == null ? null : o.toString();
			}
		}

		return null;
	}

	/**
	 * We will initialize file contents with a sample text.
	 */

	private InputStream openContentStream(String encoding, String newline) {
		// build namespace XML
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			XMLStreamWriter writer = factory.createXMLStreamWriter(baos, encoding);
			writer.writeStartDocument(encoding, "1.0");
			writer.writeCharacters(newline);
			writer.writeStartElement("testdata");
			writer.writeNamespace("", "http://aludratest.org/testdata");
			writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
					"http://aludratest.org/testdata http://aludratest.github.io/aludratest/schema/draft/testdata.xsd");
			writer.writeAttribute("version", "1.0");
			writer.writeCharacters(newline);
			writer.writeStartElement("metadata");
			writer.writeEndElement();
			writer.writeCharacters(newline);
			writer.writeStartElement("configurations");
			writer.writeEndElement();
			writer.writeCharacters(newline);
			writer.writeEndElement();
			writer.writeCharacters(newline);

			writer.flush();
			return new ByteArrayInputStream(baos.toByteArray());
		}
		catch (Exception e) {
			// log, and initialize with empty content
			VdePlugin.getDefault().logException("Could not create default testdata XML content", e);
			return new ByteArrayInputStream(new byte[0]);
		}
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "org.aludratest.eclipse.vde", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}