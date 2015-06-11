package org.aludratest.eclipse.vde.internal;

import java.util.Locale;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.service.localization.LocaleProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class VdePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.aludratest.eclipse.vde"; //$NON-NLS-1$

	// The shared instance
	private static VdePlugin plugin;
	
	/**
	 * The constructor
	 */
	public VdePlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);

		for (VdeImage image : VdeImage.values()) {
			reg.put(image.name(), getImageDescriptor(image.getResource()));
		}
	}

	public Image getImage(VdeImage image) {
		return getImageRegistry().get(image.name());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static VdePlugin getDefault() {
		return plugin;
	}

	public Locale getCurrentLocale() {
		ServiceReference<LocaleProvider> ref = getBundle().getBundleContext().getServiceReference(LocaleProvider.class);
		if (ref == null) {
			return Locale.getDefault();
		}

		LocaleProvider provider = getBundle().getBundleContext().getService(ref);
		try {
			Locale l = provider.getLocale();
			return l == null ? Locale.getDefault() : l;
		}
		catch (Throwable t) {
			return Locale.getDefault();
		}
		finally {
			getBundle().getBundleContext().ungetService(ref);
		}
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public void logException(String message, Throwable t) {
		getLog().log(new Status(Status.ERROR, PLUGIN_ID, message, t));
	}
}
