package com.manorrock.assistant.api;

/**
 * Represents a parameter for a tool.
 */
public class ToolParameter {
    private String name;
    private String type;
    private String description;
    private boolean required;

    public ToolParameter(String name, String type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }
}
