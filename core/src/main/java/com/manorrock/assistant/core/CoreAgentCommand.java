package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;

/**
 * The /agent command implementation.
 *
 * Usage:
 *   /agent list - lists available agents and their status
 */
public class CoreAgentCommand implements Command {
    @Override
    public String execute(String args) {
        String subcommand = args.trim();
        if (subcommand.equalsIgnoreCase("list")) {
            // For now, just scaffold with a static response
            return "Available agents:\n- default (active)\n- test-agent (inactive)";
        } else {
            return "Usage: /agent list";
        }
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
