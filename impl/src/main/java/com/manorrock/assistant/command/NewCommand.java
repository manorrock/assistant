package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;

/**
 * Command to start a new chat session. Clears conversation history and resets context.
 */
public class NewCommand implements Command {

    private final Runnable newSessionHandler;

    public NewCommand(Runnable newSessionHandler) {
        this.newSessionHandler = newSessionHandler;
    }

    @Override
    public String getDescription() {
        return "Create a new chat session";
    }

    @Override
    public String execute(String input) {
        newSessionHandler.run();
        return "Started new chat session";
    }

    @Override
    public String getShortDescription() {
        return "Start a new chat session";
    }
}
