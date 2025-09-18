package com.manorrock.assistant.tool.filesystem;

import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.core.CoreLlmManager;
import com.manorrock.assistant.core.CoreLlm;
import com.manorrock.assistant.core.CoreToolManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "OLLAMA_HOST", matches = ".+")
class CreateDirectoryToolIntegrationTest {
    @Test
    void testLlmCreatesDirectory() {
        // Setup CoreAssistant and CoreToolManager
        CoreAssistant assistant = new CoreAssistant();
        CoreToolManager toolManager = new CoreToolManager(assistant);
        toolManager.registerTool(new CreateDirectoryTool());
        assistant.setToolManager(toolManager);

        // Setup CoreLlmManager and CoreLlm
    CoreLlmManager llmManager = new CoreLlmManager(assistant);
    CoreLlm llm = new CoreLlm(llmManager);
    llm.setFunctionCallingEnabled(true);
    Properties props = new Properties();
    props.setProperty("baseUrl", System.getenv("OLLAMA_HOST"));
    props.setProperty("modelName", "llama3.2");
    llm.setProperties(props);
    llmManager.registerLlm("llama3.2", llm);
    // No need to setLlmManager, CoreAssistant already has getLlmManager()

        // Directory to create
        String testDir = "target/llmIntegrationTestDir";
        File dir = new File(testDir);
        if (dir.exists()) dir.delete();

        // Ask LLM to create the directory
        String prompt = "Please create a directory called '" + testDir + "'.";
        System.out.println("[PROMPT]" + prompt);
        String response = llm.process(prompt);
        System.out.println("[RESPONSE]" + response);

        // Validate directory was created
        assertTrue(dir.exists() && dir.isDirectory(), "LLM should have created the directory");
        // Cleanup
        dir.delete();
    }
}
