package com.manorrock.assistant.eclipse;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.manorrock.assistant.eclipse.perspectives.PerspectiveListener;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.manorrock.assistant.eclipse"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		// Register perspective listeners for all workbench windows
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			for (org.eclipse.ui.IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
				window.addPerspectiveListener(new PerspectiveListener(window));
			}
			
			// Add listener for future windows
			PlatformUI.getWorkbench().addWindowListener(new org.eclipse.ui.IWindowListener() {
				@Override
				public void windowOpened(org.eclipse.ui.IWorkbenchWindow window) {
					window.addPerspectiveListener(new PerspectiveListener(window));
				}
				
				@Override
				public void windowClosed(org.eclipse.ui.IWorkbenchWindow window) {
					// Nothing to do
				}
				
				@Override
				public void windowActivated(org.eclipse.ui.IWorkbenchWindow window) {
					// Nothing to do
				}
				
				@Override
				public void windowDeactivated(org.eclipse.ui.IWorkbenchWindow window) {
					// Nothing to do
				}
			});
		});
	}

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
	public static Activator getDefault() {
		return plugin;
	}

}
