package com.manorrock.assistant.command;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ClearCommandTest {

    @Test
    public void testExecuteToString() {
        ClearCommand command = new ClearCommand();
        String result = command.executeToString("any input");
        assertThat(result, is(equalTo("Response area cleared.")));
    }

    @Test
    public void testExecuteToStream() throws IOException {
        ClearCommand command = new ClearCommand();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(command.executeToStream("any input"), StandardCharsets.UTF_8))) {
            String result = reader.lines().collect(Collectors.joining("\n"));
            assertThat(result, is(equalTo("Response area cleared.")));
        }
    }

    @Test
    public void testGetDescription() {
        ClearCommand command = new ClearCommand();
        String description = command.getDescription();
        assertThat(description, is(notNullValue()));
        assertThat(description, is(equalTo("Clear the response window")));
    }
}