package org.aludratest.eclipse.vde.internal.refactor;

import org.aludratest.eclipse.vde.internal.model.AbstractModelNode;
import org.eclipse.search.ui.text.Match;

public class ModelNodeAttributeValueMatch extends Match {

	private String nodeXPath;

	private String attributeName;

	public ModelNodeAttributeValueMatch(Object element, AbstractModelNode node, String attributeName) {
		super(element, node.getAttributeValueRegion(attributeName).getOffset(), node.getAttributeValueRegion(attributeName)
				.getLength());
		this.nodeXPath = node.getElementXPath();
		this.attributeName = attributeName;
	}

	public String getNodeXPath() {
		return nodeXPath;
	}

	public String getAttributeName() {
		return attributeName;
	}

}
