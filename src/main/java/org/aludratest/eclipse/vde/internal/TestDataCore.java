package org.aludratest.eclipse.vde.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aludratest.dict.Data;
import org.aludratest.eclipse.vde.internal.TestDataModelDiff.DiffType;
import org.aludratest.eclipse.vde.internal.model.AbstractModelNode;
import org.aludratest.eclipse.vde.internal.model.DOMDocumentProvider;
import org.aludratest.eclipse.vde.internal.model.TestData;
import org.aludratest.eclipse.vde.internal.model.TestDataOperation;
import org.aludratest.eclipse.vde.internal.util.SignatureUtil;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

@SuppressWarnings("restriction")
public final class TestDataCore {

	private TestDataCore() {
	}

	public static void validateTestData(TestData data, IAnnotationModel annotationModel) throws CoreException {
		ITestDataMetadata meta = data.getMetaData();

		// first pass: Config names must be unique
		Set<String> configNames = new HashSet<String>();
		for (ITestDataConfiguration config : data.getConfigurations()) {
			if (configNames.contains(config.getName())) {
				int offset = -1;
				if (config instanceof AbstractModelNode) {
					offset = ((AbstractModelNode) config).getElementRegion().getOffset();
				}

				createValidationError(annotationModel, offset, "The configuration " + config.getName()
						+ " is already present in this file.", IMarker.SEVERITY_ERROR);
			}
			configNames.add(config.getName());
		}

		// TODO more validations; configs must match segments

	}

	public static boolean isProperty(IField field) {
		// there must be a Getter and a Setter
		try {
			IMethod getter = GetterSetterUtil.getGetter(field);
			IMethod setter = GetterSetterUtil.getSetter(field);
			return (getter != null && setter != null);
		}
		catch (JavaModelException e) {
			return false;
		}
	}

	public static <T> T performTestDataOperation(IFile file, TestDataOperation<T> operation) throws IOException, CoreException {
		final IDOMModel domModel;
		if (operation.isEdit()) {
			domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForEdit(file);
		}
		else {
			domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(file);
		}
		DOMDocumentProvider provider = new DOMDocumentProvider() {
			@Override
			public IDOMDocument getDOMDocumentForRead() {
				return domModel.getDocument();
			}

			@Override
			public IDOMDocument getDOMDocumentForEdit() {
				return domModel.getDocument();
			}

			@Override
			public void releaseFromRead() {
				// done outside
			}

			@Override
			public void releaseFromEdit() {
				// done outside
			}
		};

		try {
			return operation.perform(new TestData(provider));
		}
		finally {
			if (operation.isEdit()) {
				domModel.save();
				domModel.releaseFromEdit();
			}
			else {
				domModel.releaseFromRead();
			}
		}
	}

	public static TestDataModelDiff[] checkModelCurrentness(ITestDataSegmentMetadata segment, IJavaProject javaProject) {
		List<TestDataModelDiff> result = new ArrayList<TestDataModelDiff>();

		String className = segment.getDataClassName();

		// build a map name -> field for faster access
		Map<String, ITestDataFieldMetadata> fieldMap = new HashMap<String, ITestDataFieldMetadata>();
		for (ITestDataFieldMetadata field : segment.getFields()) {
			fieldMap.put(field.getName(), field);
		}

		try {
			IType tp = javaProject.findType(className);
			if (tp == null) {
				return new TestDataModelDiff[] { new TestDataModelDiff(segment.getName(), DiffType.CLASS_NOT_FOUND) };
			}

			// check for fields in Data class, but not in metadata
			for (IField field : tp.getFields()) {
				if (isProperty(field)) {
					// check for type difference
					StringBuilder sb = new StringBuilder();
					TestDataFieldType expectedFieldType = getFieldTypeAndSubType(field, sb);
					String expectedSubType = sb.length() == 0 ? null : sb.toString();

					if (!fieldMap.containsKey(field.getElementName())) {
						result.add(new TestDataModelDiff(segment.getName(), field.getElementName(), expectedFieldType,
								expectedSubType, DiffType.MISSING_IN_MODEL));
					}
					else {
						ITestDataFieldMetadata metaField = fieldMap.get(field.getElementName());

						// difference only possible if expected type is not STRING, or if present type is complex
						TestDataFieldType metaFieldType = metaField.getType();
						if (((metaFieldType == TestDataFieldType.OBJECT_LIST || metaFieldType == TestDataFieldType.OBJECT || metaFieldType == TestDataFieldType.STRING_LIST) && expectedFieldType == TestDataFieldType.STRING)
								|| (expectedFieldType != TestDataFieldType.STRING && !expectedFieldType.equals(metaFieldType))) {
							result.add(new TestDataModelDiff(segment.getName(), field.getElementName(), expectedFieldType,
									expectedSubType, DiffType.DIFFERS));
						}
					}
				}
				else {
					// otherwise it is a difference IF the field exists in metadata! (Write to Data bean would fail)
					if (fieldMap.containsKey(field.getElementName())) {
						result.add(new TestDataModelDiff(segment.getName(), field.getElementName(), null, null,
								DiffType.MISSING_IN_CLASS));
					}
				}
			}

			// check for fields in metadata, but not in Data class
			for (ITestDataFieldMetadata field : segment.getFields()) {
				IField javaField = tp.getField(field.getName());
				if (javaField == null || !javaField.exists()) {
					result.add(new TestDataModelDiff(segment.getName(), field.getName(), null, null, DiffType.MISSING_IN_CLASS));
				}
			}
		}
		catch (JavaModelException e) {
			// could also be e.g. a compilation error in Data class, but treat as "class not found".
			result.add(new TestDataModelDiff(segment.getName(), DiffType.CLASS_NOT_FOUND));
		}

		return result.toArray(new TestDataModelDiff[0]);
	}

	public static TestDataModelDiff[] checkModelCurrentness(ITestData data, IJavaProject javaProject) {
		List<TestDataModelDiff> result = new ArrayList<TestDataModelDiff>();

		ITestDataMetadata meta = data.getMetaData();
		for (ITestDataSegmentMetadata segment : meta.getSegments()) {
			result.addAll(Arrays.asList(checkModelCurrentness(segment, javaProject)));
		}

		return result.toArray(new TestDataModelDiff[0]);
	}

	private static void createValidationError(IAnnotationModel model, int totalOffset, String message, int severity)
			throws CoreException {
		if (model != null) {
			String annotationType;
			switch (severity) {
				case IMarker.SEVERITY_WARNING:
					annotationType = "org.eclipse.ui.workbench.texteditor.warning";
					break;
				case IMarker.SEVERITY_INFO:
					annotationType = "org.eclipse.ui.workbench.texteditor.info";
					break;
				default:
					annotationType = "org.eclipse.ui.workbench.texteditor.error";
					break;
			}
			Annotation a = new Annotation(annotationType, false, message);
			model.addAnnotation(a, new Position(totalOffset));
		}
		else {
			int statusSeverity;
			switch (severity) {
				case IMarker.SEVERITY_WARNING:
					statusSeverity = IStatus.WARNING;
					break;
				case IMarker.SEVERITY_INFO:
					statusSeverity = IStatus.INFO;
					break;
				default:
					statusSeverity = IStatus.ERROR;
					break;
			}
			throw new CoreException(new Status(statusSeverity, VdePlugin.PLUGIN_ID, message));
		}
	}

	public static IType findClass(IResource projectResource, String className) {
		IJavaProject javaProject = JavaCore.create(projectResource.getProject());

		try {
			return javaProject.findType(className);
		}
		catch (JavaModelException e) {
			// no Java project
			return null;
		}
	}

	public static IType findDataClass(IResource projectResource) {
		return findClass(projectResource, Data.class.getName());
	}

	public static void applyFieldType(IField javaField, ITestDataFieldMetadata metaField) throws JavaModelException {
		StringBuilder sb = new StringBuilder();
		TestDataFieldType fieldType = getFieldTypeAndSubType(javaField, sb);

		if (fieldType != TestDataFieldType.STRING) {
			metaField.setType(fieldType);
			metaField.setSubTypeClassName(sb.toString());
		}
	}

	private static TestDataFieldType getFieldTypeAndSubType(IField javaField, StringBuilder sbSubType) throws JavaModelException {
		String fieldType = javaField.getTypeSignature();
		String simpleName = Signature.getSignatureSimpleName(fieldType);

		if (!"String".equals(simpleName)) {
			// collection?
			String collType = getCollectionType(javaField);
			if (collType != null) {
				if (String.class.getName().equals(collType)) {
					return TestDataFieldType.STRING_LIST;
				}
				sbSubType.append(SignatureUtil.getFullyQualifiedType(javaField.getTypeRoot(), collType));
				return TestDataFieldType.OBJECT_LIST;
			}
			else {
				sbSubType.append(SignatureUtil.getFullyQualifiedType(javaField));
				return TestDataFieldType.OBJECT;
			}
		}
		else {
			return TestDataFieldType.STRING;
		}
	}

	private static String getCollectionType(IField field) {
		try {
			String fieldType = field.getTypeSignature();
			// is it a Collection class?
			if (SignatureUtil.isCollection(field)) {
				String[] typeParams = Signature.getTypeArguments(fieldType);
				if (typeParams.length == 0) {
					return null;
				}
				String param = typeParams[0];
				String qualifier = Signature.getSignatureQualifier(param);
				String simpleName = Signature.getSignatureSimpleName(param);
				return (qualifier == null || "".equals(qualifier)) ? simpleName : (qualifier + "." + simpleName);
			}
		}
		catch (JavaModelException e) {
			return null;
		}

		return null;
	}

}
