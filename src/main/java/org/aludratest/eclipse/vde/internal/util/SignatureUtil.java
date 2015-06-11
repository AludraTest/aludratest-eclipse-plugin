package org.aludratest.eclipse.vde.internal.util;

import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public final class SignatureUtil {

	private SignatureUtil() {
	}

	public static String getFullyQualifiedType(IField field) throws JavaModelException {
		String typeSignature = field.getTypeSignature();
		String qualifier = Signature.getSignatureQualifier(typeSignature);
		String simpleName = Signature.getSignatureSimpleName(typeSignature);
		simpleName = Signature.getTypeErasure(simpleName);

		if (qualifier == null || "".equals(qualifier)) {
			return getFullyQualifiedType(field.getTypeRoot(), simpleName);

		}
		else {
			return Signature.getSignatureQualifier(typeSignature) + "." + Signature.getSignatureSimpleName(typeSignature);
		}
	}

	public static boolean isCollection(IField field) {
		try {
			String fqcn = getFullyQualifiedType(field);

			// check against our own classloader, as only Collection Framework types should be used
			Class<?> clz = Class.forName(fqcn);
			return Collection.class.isAssignableFrom(clz);
		}
		catch (Exception e) {
			// ignore; dunno class
			return false;
		}
	}

	public static String getFullyQualifiedSignatureType(ITypeRoot typeRoot, String signature) throws JavaModelException {
		String qualifier = Signature.getSignatureQualifier(signature);
		if (qualifier != null && !"".equals(qualifier)) {
			return qualifier + "." + Signature.getSignatureSimpleName(signature);
		}

		return getFullyQualifiedType(typeRoot, Signature.getSignatureSimpleName(signature));
	}

	public static String getFullyQualifiedType(ITypeRoot typeRoot, String simpleOrQualifiedName) throws JavaModelException {
		IJavaProject project = typeRoot.getJavaProject();

		// try if this already marks an existing class (could also be e.g. "Map.Entry")
		if (simpleOrQualifiedName.contains(".")) {
			IType tp = project.findType(simpleOrQualifiedName);
			if (tp != null) {
				return tp.getFullyQualifiedName();
			}
		}

		if (typeRoot instanceof ICompilationUnit) {
			ICompilationUnit cu = (ICompilationUnit) typeRoot;

			for (IImportDeclaration imp : cu.getImports()) {
				String importName = imp.getElementName();
				if (importName.endsWith(".*")) {
					String testName = importName.substring(0, importName.length() - 1) + simpleOrQualifiedName;
					IType testType = project.findType(testName);
					if (testType != null) {
						return testName;
					}
				}
				else if (importName.endsWith("." + simpleOrQualifiedName)) {
					return importName;
				}
			}
		}

		// OK, now check package of type
		IType primaryType = typeRoot.findPrimaryType();
		if (primaryType != null) {
			IPackageFragment pkg = primaryType.getPackageFragment();
			if (pkg != null && !pkg.isDefaultPackage()) {
				String testName = pkg.getElementName() + "." + simpleOrQualifiedName;
				IType testType = project.findType(testName);
				if (testType != null) {
					return testName;
				}
			}
		}

		// now check java.lang
		String testName = "java.lang." + simpleOrQualifiedName;
		IType testType = project.findType(testName);
		if (testType != null) {
			return testName;
		}

		// return simple name. Could be default package
		return simpleOrQualifiedName;
	}

}
