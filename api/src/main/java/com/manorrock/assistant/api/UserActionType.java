package com.manorrock.assistant.api;

/**
 * Standard action types for user interactions in conversations.
 */
public enum UserActionType {
    TEXT_INPUT("text", "Request for text input"),
    FILE_UPLOAD("file", "Request for file upload"),
    PERMISSION("permission", "Request for permission"),
    CHOICE("choice", "Request for choice selection"),
    CONFIRM("confirm", "Request for confirmation"),
    NUMBER_INPUT("number", "Request for numeric input"),
    DATE_INPUT("date", "Request for date input"),
    DIALOG("dialog", "Display a dialog box"),
    FORM("form", "Display a form for input"),
    NOTIFICATION("notification", "Display a notification"),
    PROGRESS("progress", "Display progress information"),
    CUSTOM("custom", "Custom UI interaction");

    private final String type;
    private final String description;

    UserActionType(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public static UserActionType fromString(String type) {
        for (UserActionType action : UserActionType.values()) {
            if (action.type.equalsIgnoreCase(type)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown action type: " + type);
    }
}