package com.example;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class PluginActivator extends AbstractUIPlugin {
    private static PluginActivator instance;

    public PluginActivator() {
        instance = this;
    }

    public static PluginActivator getDefault() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        super.stop(context);
    }
}
