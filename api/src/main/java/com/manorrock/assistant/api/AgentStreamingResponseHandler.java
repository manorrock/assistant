package com.manorrock.assistant.api;

/**
 * Handler for streaming agent response tokens.
 */
public interface AgentStreamingResponseHandler {
    /**
     * Called when a new token is received from the agent.
     *
     * @param token the response token
     */
    void onToken(String token);

    /**
     * Called when the streaming response is complete.
     */
    void onComplete();

    /**
     * Called if an error occurs during streaming.
     *
     * @param error the error message
     */
    void onError(String error);
}
