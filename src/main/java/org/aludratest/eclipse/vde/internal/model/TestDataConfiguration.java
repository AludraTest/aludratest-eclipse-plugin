package org.aludratest.eclipse.vde.internal.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.databene.commons.Filter;
import org.w3c.dom.Element;

public class TestDataConfiguration extends AbstractModelNode implements ITestDataConfiguration {

	public TestDataConfiguration(AbstractModelNode parentNode, String wrapperElementName, int indexInParent) {
		super(parentNode, wrapperElementName, indexInParent);
	}

	@Override
	protected String getElementName() {
		return CONFIGURATION;
	}

	@Override
	public String getName() {
		return getAttributeValueOrNull(NAME);
	}

	@Override
	public void setName(String name) {
		setAttributeValue(NAME, name);
	}

	@Override
	public boolean isIgnored() {
		return "true".equals(getAttributeValueOrNull(IGNORED));
	}

	@Override
	public void setIgnored(boolean ignored) {
		// this removes the ignored attribute when set to false
		setAttributeValue(IGNORED, ignored ? "true" : null);
	}

	@Override
	public String getIgnoredReason() {
		return getAttributeValueOrNull(IGNORED_REASON);
	}

	@Override
	public void setIgnoredReason(String reason) {
		setAttributeValue(IGNORED_REASON, reason);
	}
	
	@Override
	public String getExternalTestId() {
		return getAttributeValueOrNull(EXTERNAL_TEST_ID);
	}

	@Override
	public void setExternalTestId(String externalTestId) {
		setAttributeValue(EXTERNAL_TEST_ID, externalTestId);
	}

	@Override
	public ITestDataConfigurationSegment[] getSegments() {
		return getChildObjects(TestDataConfigurationSegment.class, new ITestDataConfigurationSegment[0], SEGMENTS);
	}

	@Override
	public ITestDataConfigurationSegment getSegment(final String name) {
		if (name == null) {
			return null;
		}

		Filter<Element> elementFilter = new Filter<Element>() {
			@Override
			public boolean accept(Element element) {
				return name.equals(element.getAttribute("name"));
			}
		};

		ITestDataConfigurationSegment[] segments = getChildObjects(TestDataConfigurationSegment.class,
				new ITestDataConfigurationSegment[0], SEGMENTS, elementFilter);
		return segments == null || segments.length == 0 ? null : segments[0];
	}

	public void syncToMetadata(ITestDataMetadata metadata) {
		Set<String> metadataSegments = new HashSet<String>();

		// a small optimization for VERY large files
		ITestDataConfigurationSegment[] configSegments = getSegments();
		Map<ITestDataConfigurationSegment, String> configSegmentNames = new HashMap<ITestDataConfigurationSegment, String>();

		for (ITestDataSegmentMetadata segment : metadata.getSegments()) {
			String segmentName = segment.getName();
			boolean found = false;
			for (ITestDataConfigurationSegment configSegment : configSegments) {
				String configSegmentName = configSegmentNames.get(configSegment);
				if (configSegmentName == null) {
					configSegmentNames.put(configSegment, configSegmentName = configSegment.getName());
				}
				if (configSegmentName.equals(segmentName) && (configSegment instanceof TestDataConfigurationSegment)) {
					((TestDataConfigurationSegment) configSegment).syncToMetadata(segment);
					found = true;
					break;
				}
			}
			if (!found) {
				addSegment(segmentName, segment);
			}
			metadataSegments.add(segmentName);
		}

		for (ITestDataConfigurationSegment configSegment : getSegments()) {
			if (configSegment instanceof TestDataConfigurationSegment) {
				boolean notReferenced = !metadataSegments.contains(configSegment.getName());
				((TestDataConfigurationSegment) configSegment).setNotReferencedInMetadata(notReferenced);
			}
		}
	}

	public void removeSegment(ITestDataConfigurationSegment segment) {
		if (segment instanceof AbstractModelNode) {
			removeChild(SEGMENTS, (AbstractModelNode) segment);
		}
	}

	private void addSegment(final String name, final ITestDataSegmentMetadata metadata) {
		DOMOperation<Void> op = new DOMOperation<Void>() {
			@Override
			public Void perform(Element element) {
				Element newSegment = TestDataConfigurationSegment.create(getChildElement(element, SEGMENTS, true), name);
				new TestDataConfigurationSegment(TestDataConfiguration.this, SEGMENTS, getIndexOfElementInParent(newSegment))
						.syncToMetadata(metadata);
				return null;
			}

			@Override
			public boolean isEdit() {
				return true;
			}

			@Override
			public String getName() {
				return "Add Segment";
			}
		};
		performDOMOperation(op);
	}

	public static Element create(Element parent, final String name) {
		Element element = appendChildElement(parent, CONFIGURATION);
		element.setAttribute(NAME, name);
		return element;

	}
}
