package com.example;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class PluginView extends ViewPart {
    private PluginController controller;

    @Override
    public void createPartControl(Composite parent) {
        controller = new PluginController();
        controller.createPartControl(parent);
    }

    @Override
    public void setFocus() {
        // Set focus logic if needed
    }
}
