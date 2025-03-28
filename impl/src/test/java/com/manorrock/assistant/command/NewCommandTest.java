package com.manorrock.assistant.command;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NewCommandTest {

    @Test
    public void testGetDescription() {
        NewCommand command = new NewCommand(() -> {});
        String description = command.getDescription();
        assertThat(description, is("Create a new chat session"));
    }

    @Test
    public void testExecuteToString() {
        boolean[] handlerCalled = {false};
        NewCommand command = new NewCommand(() -> handlerCalled[0] = true);
        
        String result = command.executeToString("any input");
        
        assertThat(result, is("Started new chat session"));
        assertTrue("New session handler should be called", handlerCalled[0]);
    }

    @Test
    public void testExecuteToStream() throws IOException {
        boolean[] handlerCalled = {false};
        NewCommand command = new NewCommand(() -> handlerCalled[0] = true);
        
        InputStream resultStream = command.executeToStream("any input");
        
        // Read the stream content
        byte[] buffer = new byte[100];
        int bytesRead = resultStream.read(buffer);
        String result = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        
        assertThat(result, is("Started new chat session"));
        assertTrue("New session handler should be called", handlerCalled[0]);
    }

    @Test
    public void testConstructor() {
        Runnable mockHandler = () -> {};
        NewCommand command = new NewCommand(mockHandler);
        
        // This test verifies the constructor doesn't throw exceptions
        assertNotNull(command);
    }

    @Test
    public void testExecuteToStringWithNullInput() {
        boolean[] handlerCalled = {false};
        NewCommand command = new NewCommand(() -> handlerCalled[0] = true);
        
        String result = command.executeToString(null);
        
        assertThat(result, is("Started new chat session"));
        assertTrue("New session handler should be called even with null input", handlerCalled[0]);
    }

    @Test
    public void testExecuteToStreamWithNullInput() throws IOException {
        boolean[] handlerCalled = {false};
        NewCommand command = new NewCommand(() -> handlerCalled[0] = true);
        
        InputStream resultStream = command.executeToStream(null);
        
        // Read the stream content
        byte[] buffer = new byte[100];
        int bytesRead = resultStream.read(buffer);
        String result = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        
        assertThat(result, is("Started new chat session"));
        assertTrue("New session handler should be called even with null input", handlerCalled[0]);
    }
}