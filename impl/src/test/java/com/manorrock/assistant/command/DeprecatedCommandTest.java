package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class DeprecatedCommandTest {

    @Test
    public void testConstructorWithoutDelegate() {
        DeprecatedCommand command = new DeprecatedCommand("oldCommand", "newCommand");
        assertEquals("oldCommand", command.getDeprecatedCommandName());
        assertEquals("newCommand", command.getNewCommandName());
    }

    @Test
    public void testConstructorWithDelegate() {
        Command mockDelegate = new Command() {
            @Override
            public String execute(String input) {
                return executeToString(input);
            }
            
            @Override
            public String executeToString(String input) {
                return "Mock result";
            }
            
            @Override
            public InputStream executeToStream(String input) {
                return new ByteArrayInputStream("Mock result".getBytes(StandardCharsets.UTF_8));
            }
            
            @Override
            public String getDescription() {
                return "Mock command";
            }
        };
        
        DeprecatedCommand command = new DeprecatedCommand("oldCommand", "newCommand", mockDelegate);
        assertEquals("oldCommand", command.getDeprecatedCommandName());
        assertEquals("newCommand", command.getNewCommandName());
    }

    @Test
    public void testExecuteToStringWithoutDelegate() {
        DeprecatedCommand command = new DeprecatedCommand("oldCommand", "newCommand");
        String result = command.executeToString("test input");
        assertThat(result, containsString("WARNING: The command '/oldCommand' is deprecated"));
        assertThat(result, containsString("Please use '/newCommand' instead."));
    }

    @Test
    public void testExecuteToStringWithDelegate() {
        Command mockDelegate = new Command() {
            @Override
            public String execute(String input) {
                return executeToString(input);
            }
            
            @Override
            public String executeToString(String input) {
                return "Mock result for: " + input;
            }
            
            @Override
            public InputStream executeToStream(String input) {
                return new ByteArrayInputStream(("Mock result for: " + input).getBytes(StandardCharsets.UTF_8));
            }
            
            @Override
            public String getDescription() {
                return "Mock command";
            }
        };
        
        DeprecatedCommand command = new DeprecatedCommand("oldCommand", "newCommand", mockDelegate);
        String result = command.executeToString("test input");
        assertThat(result, containsString("WARNING: The command '/oldCommand' is deprecated"));
        assertThat(result, containsString("Please use '/newCommand' instead."));
        assertThat(result, containsString("Mock result for: test input"));
    }

    @Test
    public void testExecuteToStream() throws IOException {
        DeprecatedCommand command = new DeprecatedCommand("oldCommand", "newCommand");
        InputStream stream = command.executeToStream("test input");
        
        byte[] buffer = new byte[1024];
        int bytesRead = stream.read(buffer);
        String result = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        
        assertThat(result, containsString("WARNING: The command '/oldCommand' is deprecated"));
        assertThat(result, containsString("Please use '/newCommand' instead."));
    }

    @Test
    public void testGetDescription() {
        DeprecatedCommand command = new DeprecatedCommand("oldCommand", "newCommand");
        assertEquals("Deprecated: Use /newCommand instead", command.getDescription());
    }
}