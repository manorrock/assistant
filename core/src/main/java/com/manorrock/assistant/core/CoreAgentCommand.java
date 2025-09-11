package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;

/**
 * The /agent command implementation.
 *
 * Usage:
 *   /agent list - lists available agents and their status
 */
public class CoreAgentCommand implements Command {

    private final CoreAssistant assistant;

    /**
     * Constructor.
     *
     * @param assistant The core assistant instance
     */
    public CoreAgentCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public String execute(String args) {
        String subcommand = args.trim();
        if (subcommand.equalsIgnoreCase("list")) {
            return listAgents();
        } else {
            return "Usage: /agent list";
        }
    }

    /**
     * List all available agents and their status.
     *
     * @return String representation of available agents
     */
    private String listAgents() {
        var agentManager = assistant.getAgentManager();
        var agents = agentManager.getAvailableAgents();

        if (agents.isEmpty()) {
            return "No agents available";
        }

        StringBuilder result = new StringBuilder("Available agents:\n");
        for (var agent : agents) {
            result.append("  - ").append(agent.getName())
                  .append(": ").append(agent.getDescription()).append("\n");
        }
        return result.toString();
    }

    @Override
    public String getDescription() {
        return "List available agents and their status";
    }

    @Override
    public String getShortDescription() {
        return "List available agents";
    }
}
