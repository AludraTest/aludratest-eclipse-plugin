package org.aludratest.eclipse.vde.internal.model;

import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.w3c.dom.Element;

public class TestData extends AbstractModelNode implements ITestData {

	public TestData(DOMDocumentProvider documentProvider) {
		super(documentProvider);
	}

	@Override
	protected String getElementName() {
		return TESTDATA;
	}

	@Override
	public String getVersion() {
		return getAttributeValueOrNull(VERSION);
	}

	@Override
	public ITestDataMetadata getMetaData() {
		return getChildObject(TestDataMetadata.class, true);
	}

	@Override
	public ITestDataConfiguration[] getConfigurations() {
		return getChildObjects(TestDataConfiguration.class, new ITestDataConfiguration[0], CONFIGURATIONS);
	}

	@Override
	public void addConfiguration(final String name) {
		DOMOperation<Void> op = new DOMOperation<Void>() {
			@Override
			public Void perform(Element element) {
				Element configs = getChildElement(element, CONFIGURATIONS, true);
				format(TestDataConfiguration.create(configs, name), false);
				return null;
			}

			@Override
			public boolean isEdit() {
				return true;
			}

			@Override
			public String getName() {
				return "Add configuration";
			}
		};
		performDOMOperation(op);
	}

	@Override
	public void removeConfiguration(ITestDataConfiguration configuration) {
		if (configuration instanceof AbstractModelNode) {
			removeChild(CONFIGURATIONS, (AbstractModelNode) configuration);
		}
	}

}
