package com.manorrock.assistant.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of executing an assistant.
 * Contains success/failure status and result data.
 */
public class AssistantResult {
    private final boolean success;
    private final Map<String, Object> data;
    private final String message;

    /**
     * Creates a successful assistant result.
     *
     * @param data the result data
     * @param message success message
     * @return a successful AssistantResult
     */
    public static AssistantResult success(Map<String, Object> data, String message) {
        return new AssistantResult(true, data, message);
    }

    /**
     * Creates a successful assistant result with just data.
     *
     * @param data the result data
     * @return a successful AssistantResult
     */
    public static AssistantResult success(Map<String, Object> data) {
        return new AssistantResult(true, data, "Assistant executed successfully");
    }

    /**
     * Creates a failed assistant result.
     *
     * @param message the error message
     * @return a failed AssistantResult
     */
    public static AssistantResult failure(String message) {
        return new AssistantResult(false, null, message);
    }

    AssistantResult(boolean success, Map<String, Object> data, String message) {
        this.success = success;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.message = message;
    }

    /**
     * @return true if the assistant execution was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return the result data if successful, null otherwise
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * @return a message describing the result or error
     */
    public String getMessage() {
        return message;
    }

    /**
     * Converts this result to a map suitable for serialization.
     *
     * @return a map representation of this result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", message);
        if (data != null) {
            result.put("data", data);
        }
        return result;
    }
}
