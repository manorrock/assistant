package com.manorrock.assistant.shared.tools;

import com.manorrock.assistant.shared.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebScraperToolTest {
    private WebScraperTool tool;

    @BeforeEach
    void setUp() {
        tool = new WebScraperTool();
    }

    @Test
    void testBasicProperties() {
        assertEquals("web_scraper", tool.getName());
        assertNotNull(tool.getDescription());
        assertTrue(tool.getParameters().size() > 0);
    }

    @Test
    void testMissingRequiredUrl() {
        ToolResult result = tool.execute(Collections.emptyMap());
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Missing required parameter: url"));
    }

    @Test
    void testInvalidUrl() {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "not-a-url");
        
        ToolResult result = tool.execute(params);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("no protocol"));
    }
}
