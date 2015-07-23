package org.aludratest.eclipse.vde.internal.model;

import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.w3c.dom.Element;

public class TestDataMetadata extends AbstractModelNode implements ITestDataMetadata {

	public TestDataMetadata(AbstractModelNode parentNode) {
		super(parentNode);
	}

	@Override
	public ITestDataSegmentMetadata[] getSegments() {
		return getChildObjects(TestDataSegmentMetadata.class, new ITestDataSegmentMetadata[0], SEGMENTS);
	}

	@Override
	public void addSegment(final String name, final String dataClassName) {
		DOMOperation<Void> op = new DOMOperation<Void>() {
			@Override
			public Void perform(Element element) {
				Element segments = getChildElement(element, SEGMENTS, true);
				format(TestDataSegmentMetadata.create(segments, name, dataClassName), false);
				return null;
			}

			@Override
			public boolean isEdit() {
				return true;
			}

			@Override
			public String getName() {
				return "Add segment";
			}
		};
		performDOMOperation(op);
	}

	@Override
	public void removeSegment(ITestDataSegmentMetadata segment) {
		if (!(segment instanceof AbstractModelNode)) {
			return;
		}
		removeChild(SEGMENTS, (AbstractModelNode) segment);
	}

	@Override
	public void moveSegment(ITestDataSegmentMetadata segment, int newIndex) {
		if (!(segment instanceof AbstractModelNode)) {
			return;
		}

		moveChild(SEGMENTS, ((AbstractModelNode) segment), newIndex);
	}

	@Override
	protected String getElementName() {
		return METADATA;
	}

}
