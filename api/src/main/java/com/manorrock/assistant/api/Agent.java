package com.manorrock.assistant.api;

import java.util.List;
import java.util.Optional;

/**
 * Interface defining an agent that can be managed and executed.
 */
public interface Agent {
    /**
     * Get the name of the agent.
     * @return the agent name
     */
    String getName();

    /**
     * Get the description of the agent.
     * @return the agent description
     */
    String getDescription();
}
