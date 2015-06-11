package org.aludratest.eclipse.vde.internal.editors;

import org.eclipse.nebula.widgets.nattable.copy.command.CopyDataCommandHandler;
import org.eclipse.nebula.widgets.nattable.layer.AbstractIndexLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.util.IClientAreaProvider;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;

public class GridBodyStack extends AbstractIndexLayerTransform {

	private final SelectionLayer selectionLayer;
	private final ViewportLayer viewportLayer;

	public GridBodyStack(IUniqueIndexLayer underlyingLayer) {
		this.selectionLayer = new SelectionLayer(underlyingLayer);
		this.viewportLayer = new ViewportLayer(this.selectionLayer);
		setUnderlyingLayer(this.viewportLayer);

		registerCommandHandler(new CopyDataCommandHandler(this.selectionLayer));
	}

	@Override
	public void setClientAreaProvider(IClientAreaProvider clientAreaProvider) {
		super.setClientAreaProvider(clientAreaProvider);
	}

	public SelectionLayer getSelectionLayer() {
		return this.selectionLayer;
	}

	public ViewportLayer getViewportLayer() {
		return this.viewportLayer;
	}

}
