package com.manorrock.assistant.tool;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class WebSearchToolTest {

    @Test
    public void testGetName() {
        WebSearchTool tool = new WebSearchTool();
        String expectedName = "WebSearchTool";
        assertEquals("The name should match the expected value", expectedName, tool.getName());
    }
}