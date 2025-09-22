

package com.manorrock.assistant.api;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Interface for a conversational agent, closely aligned with Llm interface.
 * Agents support stateless and session-based chat, initialization, and streaming.
 */
public interface Agent {
    /**
     * Initialize the agent (load persona, context, etc).
     */
    void init();

    /**
     * Destroy the agent and clean up resources.
     */
    void destroy();

    /**
     * Get the agent's configuration properties.
     *
     * @return the properties
     */
    Properties getProperties();

    /**
     * Set the agent's configuration properties.
     *
     * @param properties the properties
     */
    void setProperties(Properties properties);

    /**
     * Get the name of the agent/persona.
     *
     * @return the agent name
     */
    String getName();

    /**
     * Get a description of the agent/persona.
     *
     * @return the agent description
     */
    String getDescription();

    /**
     * Process a stateless prompt (single-turn, no session).
     *
     * @param prompt the prompt or message
     * @return the agent's response
     */
    String process(String prompt);

    /**
     * Process a prompt with streaming response (stateless).
     *
     * @param prompt the prompt
     * @param handler the handler for streaming response tokens
     */
    void processStreaming(String prompt, AgentStreamingResponseHandler handler);

    /**
     * Start a new chat session with the agent.
     *
     * @param context Optional initial context (persona, system prompt, etc)
     * @return a unique session ID
     */
    String startSession(Map<String, Object> context);

    /**
     * Send a message to the agent in the context of a session (multi-turn).
     *
     * @param sessionId the session ID
     * @param message the user message
     * @param history the full chat history (list of messages, can be null for stateless)
     * @return the agent's response
     */
    AgentResponse sendMessage(String sessionId, String message, List<AgentMessage> history);

    /**
     * Send a message with streaming response in a session.
     *
     * @param sessionId the session ID
     * @param message the user message
     * @param history the full chat history
     * @param handler the handler for streaming response tokens
     */
    void sendMessageStreaming(String sessionId, String message, List<AgentMessage> history, AgentStreamingResponseHandler handler);

    /**
     * End the chat session and clean up resources.
     *
     * @param sessionId the session ID
     */
    void endSession(String sessionId);

    /**
     * Get the current session context (system prompt, persona, etc).
     *
     * @param sessionId the session ID
     * @return context map
     */
    Map<String, Object> getSessionContext(String sessionId);
}
