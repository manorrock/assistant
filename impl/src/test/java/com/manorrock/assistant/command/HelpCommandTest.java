package com.manorrock.assistant.command;

import org.junit.Test;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class HelpCommandTest {

    @Test
    public void testExecuteToString() {
        HelpCommand command = new HelpCommand();
        String result = command.execute("");
        
        assertThat(result, containsString("Available commands:"));
        assertThat(result, containsString("/clear - Clears the response window"));
        assertThat(result, containsString("/explain - Explains text from clipboard"));
        assertThat(result, containsString("/help - Displays available commands"));
        assertThat(result, containsString("/llm - Displays or configures LLM settings"));
        assertThat(result, containsString("/new - Starts a new chat session"));
        assertThat(result, containsString("/source - Executes commands from a file"));
        assertThat(result, containsString("/tool - Manages tool integrations"));
    }
    
    @Test
    public void testGetDescription() {
        HelpCommand command = new HelpCommand();
        assertThat(command.getDescription(), equalTo("Show available commands"));
    }
    
    private String inputStreamToString(InputStream inputStream) throws IOException {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}