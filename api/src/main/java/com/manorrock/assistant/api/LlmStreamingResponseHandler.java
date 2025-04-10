package com.manorrock.assistant.api;

/**
 * Interface for handling streaming responses from LLMs.
 * 
 * <p>
 * This interface provides callbacks for streaming token generation from Large Language Models.
 * Implementations can handle tokens as they arrive, process the complete response, and manage errors.
 * </p>
 */
public interface LlmStreamingResponseHandler {
    
    /**
     * Called when a new token is received from the LLM.
     * 
     * @param token the generated token from the LLM
     */
    void onToken(String token);
    
    /**
     * Called when the LLM has completed generating the full response.
     * 
     * @param fullResponse the complete generated response
     */
    void onComplete(String fullResponse);
    
    /**
     * Called when an error occurs during streaming.
     * 
     * @param error the throwable representing the error
     */
    void onError(Throwable error);
}
