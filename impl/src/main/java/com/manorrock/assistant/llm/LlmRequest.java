package com.manorrock.assistant.llm;

/**
 * A request to the LLM.
 */
@Deprecated
public class LlmRequest {
    /**
     * Stores the request.
     */
    private String request;

    /*
     * Get the request.
     * 
     * @return the request.
     */
    public String getRequest() {
        return request;
    }

    /**
     * Set the request.
     *
     * @param request the request.
     */
    public void setRequest(String request) {
        this.request = request;
    }
}
