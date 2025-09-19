package com.manorrock.assistant.tool.filesystem;

import com.manorrock.assistant.api.ToolResult;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class CreateFileToolTest {

    @Test
    void testCreateTextFile() throws Exception {
        CreateFileTool tool = new CreateFileTool();
        String testFile = "testFile.txt";
        File file = new File(testFile);
        if (file.exists()) file.delete();
        ToolResult result = tool.execute(java.util.Map.of(
            "path", testFile,
            "content", "Hello, World!",
            "binary", false
        ));
        assertTrue(result.isSuccess(), "File should be created");
        assertTrue(file.exists() && file.isFile(), "File should exist");
        assertEquals("Hello, World!", Files.readString(Path.of(testFile)));
        file.delete();
    }

    @Test
    void testCreateBinaryFile() throws Exception {
        CreateFileTool tool = new CreateFileTool();
        String testFile = "testFile.bin";
        File file = new File(testFile);
        if (file.exists()) file.delete();
        byte[] data = {1, 2, 3, 4, 5};
        String base64 = Base64.getEncoder().encodeToString(data);
        ToolResult result = tool.execute(java.util.Map.of(
            "path", testFile,
            "content", base64,
            "binary", true
        ));
        assertTrue(result.isSuccess(), "Binary file should be created");
        assertTrue(file.exists() && file.isFile(), "Binary file should exist");
        assertArrayEquals(data, Files.readAllBytes(Path.of(testFile)));
        file.delete();
    }

    @Test
    void testMissingParameters() {
        CreateFileTool tool = new CreateFileTool();
        ToolResult result = tool.execute(java.util.Map.of("path", "foo.txt"));
        assertFalse(result.isSuccess(), "Should fail if content is missing");
    }
}
