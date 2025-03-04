package com.example;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

public class App implements ApplicationComponent {
    @Override
    public void initComponent() {
        // Initialization code
        initializeDesktopComponents();
    }

    @Override
    public void disposeComponent() {
        // Cleanup code
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "ExamplePlugin";
    }

    private void initializeDesktopComponents() {
        // Reuse the initialization logic from the desktop version
        // For example:
        // DesktopComponent component = new DesktopComponent();
        // component.initialize();
    }
}
