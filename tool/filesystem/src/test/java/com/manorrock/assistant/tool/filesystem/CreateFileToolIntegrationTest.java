package com.manorrock.assistant.tool.filesystem;

import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.core.CoreLlmManager;
import com.manorrock.assistant.core.CoreLlm;
import com.manorrock.assistant.core.CoreToolManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "OLLAMA_HOST", matches = ".+")
class CreateFileToolIntegrationTest {

    @Test
    void testLlmCreatesTextFile() throws Exception {
        CoreAssistant assistant = new CoreAssistant();
        CoreToolManager toolManager = new CoreToolManager(assistant);
        toolManager.registerTool(new CreateFileTool());
        assistant.setToolManager(toolManager);

        CoreLlmManager llmManager = new CoreLlmManager(assistant);
        CoreLlm llm = new CoreLlm(llmManager);
        llm.setFunctionCallingEnabled(true);
        Properties props = new Properties();
        props.setProperty("baseUrl", System.getenv("OLLAMA_HOST"));
        props.setProperty("modelName", "llama3.2");
        llm.setProperties(props);
        llmManager.registerLlm("llama3.2", llm);

        String testFile = "target/llmIntegrationTestFile.txt";
        File file = new File(testFile);
        if (file.exists()) file.delete();

        String prompt = "Please create a file called '" + testFile + "' with the content 'Hello, LLM!'";
        System.out.println("[PROMPT]" + prompt);
        String response = llm.process(prompt);
        System.out.println("[RESPONSE]" + response);

        assertTrue(file.exists() && file.isFile(), "LLM should have created the file");
        assertEquals("Hello, LLM!", Files.readString(file.toPath()));
        file.delete();
    }

    @Test
    void testLlmCreatesBinaryFile() throws Exception {
        CoreAssistant assistant = new CoreAssistant();
        CoreToolManager toolManager = new CoreToolManager(assistant);
        toolManager.registerTool(new CreateFileTool());
        assistant.setToolManager(toolManager);

        CoreLlmManager llmManager = new CoreLlmManager(assistant);
        CoreLlm llm = new CoreLlm(llmManager);
        llm.setFunctionCallingEnabled(true);
        Properties props = new Properties();
        props.setProperty("baseUrl", System.getenv("OLLAMA_HOST"));
        props.setProperty("modelName", "llama3.2");
        llm.setProperties(props);
        llmManager.registerLlm("llama3.2", llm);

        String testFile = "target/llmIntegrationTestFile.bin";
        File file = new File(testFile);
        if (file.exists()) file.delete();
        byte[] data = {10, 20, 30, 40, 50};
        String base64 = Base64.getEncoder().encodeToString(data);
        String prompt = "Please create a binary file called '" + testFile + "' with the following base64 content: " + base64;
        System.out.println("[PROMPT]" + prompt);
        String response = llm.process(prompt);
        System.out.println("[RESPONSE]" + response);

        assertTrue(file.exists() && file.isFile(), "LLM should have created the binary file");
        assertArrayEquals(data, Files.readAllBytes(file.toPath()));
        file.delete();
    }
}
