package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.core.CoreAssistantMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced command for code file analysis and explanation.
 * This command provides language-specific understanding and specialized prompts
 * for different programming languages and file types.
 */
public class EnhancedCodeExplainCommand implements Command {

    /**
     * The logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EnhancedCodeExplainCommand.class.getName());
    
    /**
     * Maximum size for a single chunk (in characters).
     */
    private static final int MAX_CHUNK_SIZE = 8000;
    
    /**
     * The assistant instance.
     */
    private final Assistant assistant;
    
    /**
     * Maps file extensions to appropriate language prompts.
     */
    private final Map<String, String> languagePrompts = new HashMap<>();

    /**
     * Constructor to initialize the command with the assistant.
     * 
     * @param assistant the assistant
     */
    public EnhancedCodeExplainCommand(Assistant assistant) {
        this.assistant = assistant;
        initializeLanguagePrompts();
    }
    
    /**
     * Initialize language-specific prompts for different file types.
     */
    private void initializeLanguagePrompts() {
        // Programming languages
        languagePrompts.put("java", "Analyze this Java code with focus on class structure, methods, inheritance, and design patterns.");
        languagePrompts.put("js", "Analyze this JavaScript code with focus on functions, closures, and JS-specific patterns.");
        languagePrompts.put("ts", "Analyze this TypeScript code with focus on types, interfaces, classes, and TypeScript-specific patterns.");
        languagePrompts.put("py", "Analyze this Python code with focus on functions, classes, and Pythonic idioms.");
        languagePrompts.put("rb", "Analyze this Ruby code with focus on Ruby idioms, blocks, and metaprogramming patterns.");
        languagePrompts.put("c", "Analyze this C code with focus on memory management, pointers, and C-specific constructs.");
        languagePrompts.put("cpp", "Analyze this C++ code with focus on classes, templates, STL usage, and memory management.");
        languagePrompts.put("cs", "Analyze this C# code with focus on .NET features, LINQ, and C#-specific patterns.");
        languagePrompts.put("go", "Analyze this Go code with focus on goroutines, interfaces, and idiomatic Go patterns.");
        languagePrompts.put("rs", "Analyze this Rust code with focus on ownership, borrowing, traits, and Rust idioms.");
        languagePrompts.put("php", "Analyze this PHP code with focus on functions, classes, and PHP-specific patterns.");
        languagePrompts.put("kt", "Analyze this Kotlin code with focus on null safety, functional features, and Kotlin-specific idioms.");
        languagePrompts.put("swift", "Analyze this Swift code with focus on optionals, protocols, and Swift-specific patterns.");
        languagePrompts.put("scala", "Analyze this Scala code with focus on functional programming, traits, and Scala-specific patterns.");
        
        // Config/markup files
        languagePrompts.put("json", "Analyze this JSON configuration with focus on structure, schema, and data organization.");
        languagePrompts.put("yaml", "Analyze this YAML configuration with focus on structure, anchors, and data organization.");
        languagePrompts.put("xml", "Analyze this XML document with focus on structure, namespaces, and schema compliance.");
        languagePrompts.put("html", "Analyze this HTML document with focus on structure, semantic elements, and accessibility.");
        languagePrompts.put("css", "Analyze this CSS stylesheet with focus on selectors, specificity, and styling patterns.");
        languagePrompts.put("scss", "Analyze this SCSS stylesheet with focus on nesting, variables, mixins, and SCSS-specific features.");
        languagePrompts.put("md", "Analyze this Markdown document with focus on structure, formatting, and documentation patterns.");
        
        // Shell scripts and other types
        languagePrompts.put("sh", "Analyze this shell script with focus on shell commands, conditionals, and script flow.");
        languagePrompts.put("bash", "Analyze this bash script with focus on bash-specific features, functions, and script flow.");
        languagePrompts.put("sql", "Analyze this SQL with focus on query structure, joins, indexes, and database operations.");
        languagePrompts.put("properties", "Analyze this properties file with focus on key-value pairs and configuration patterns.");
        languagePrompts.put("gradle", "Analyze this Gradle build script with focus on build configuration, dependencies, and plugins.");
    }

    @Override
    public String execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Please specify a file path to analyze.";
        }
        
        String filePath = input.trim();
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            return "Error: File not found: " + filePath;
        }
        
        if (Files.isDirectory(path)) {
            return "Error: Specified path is a directory, not a file: " + filePath;
        }
        
        // Detect file extension and language
        String fileExtension = getFileExtension(filePath);
        String languagePrompt = getLanguagePrompt(fileExtension);
        
        try {
            LOGGER.log(Level.INFO, "Analyzing file: {0} with detected type: {1}", new Object[]{filePath, fileExtension});
            
            // Read file content
            String fileContent = Files.readString(path, StandardCharsets.UTF_8);
            
            // Process the file content
            return analyzeCode(fileContent, languagePrompt, filePath);
            
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }
    
    /**
     * Analyze code content with appropriate language-specific prompt.
     * 
     * @param content The code content to analyze
     * @param languagePrompt The language-specific prompt
     * @param filePath The file path for context
     * @return The analysis result
     */
    private String analyzeCode(String content, String languagePrompt, String filePath) {
        StringBuilder result = new StringBuilder();
        
        // Add file information header
        result.append("Analysis of file: ").append(filePath).append("\n\n");
        
        // Check if the content needs to be chunked
        if (content.length() > MAX_CHUNK_SIZE) {
            result.append(analyzeChunkedCode(content, languagePrompt, filePath));
        } else {
            // Build the complete prompt with language-specific instructions
            String prompt = buildPromptWithLanguageContext(content, languagePrompt);
            
            // Process with the assistant
            var response = assistant.processMessage(new CoreAssistantMessage(prompt));
            if (response != null) {
                result.append(response.getContent());
            } else {
                result.append("Failed to get explanation from assistant.");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Process large files by breaking them into chunks.
     * 
     * @param content The full content to chunk
     * @param languagePrompt The language-specific prompt
     * @param filePath The file path for context
     * @return The combined analysis result
     */
    private String analyzeChunkedCode(String content, String languagePrompt, String filePath) {
        StringBuilder result = new StringBuilder();
        
        // Add notice about chunking
        result.append("Note: The file has been split into chunks for analysis due to its size.\n\n");
        
        // Calculate number of chunks needed
        int chunkCount = (int) Math.ceil((double) content.length() / MAX_CHUNK_SIZE);
        
        // First pass: Generate an overview of the entire file structure
        String overviewPrompt = "Provide a high-level overview of the following " + 
                                getLanguageNameFromExtension(getFileExtension(filePath)) + 
                                " file structure. Focus only on main components, imports, and organization. " +
                                "Keep it concise:\n\n" + content;
        
        if (overviewPrompt.length() > MAX_CHUNK_SIZE) {
            // If even the overview is too long, use first 20% of the file
            overviewPrompt = "Provide a high-level overview of the following " + 
                            getLanguageNameFromExtension(getFileExtension(filePath)) + 
                            " file based on its beginning section. Focus only on main components, imports, and organization. " +
                            "Keep it concise:\n\n" + content.substring(0, Math.min(content.length(), MAX_CHUNK_SIZE));
        }
        
        var overviewResponse = assistant.processMessage(new CoreAssistantMessage(overviewPrompt));
        if (overviewResponse != null) {
            result.append("# File Overview\n\n");
            result.append(overviewResponse.getContent()).append("\n\n");
        }
        
        // Process important chunks
        result.append("# Detailed Analysis\n\n");
        
        // Process first chunk (usually imports, package declarations, etc.)
        int firstChunkSize = Math.min(content.length(), MAX_CHUNK_SIZE);
        String firstChunk = content.substring(0, firstChunkSize);
        result.append("## Beginning Section\n\n");
        result.append(analyzeCodeChunk(firstChunk, languagePrompt, 1, chunkCount)).append("\n\n");
        
        // If there are more than 2 chunks, analyze the last chunk as well (often contains important closing structures)
        if (chunkCount > 2) {
            int lastChunkStart = Math.max(0, content.length() - MAX_CHUNK_SIZE);
            String lastChunk = content.substring(lastChunkStart);
            result.append("## Ending Section\n\n");
            result.append(analyzeCodeChunk(lastChunk, languagePrompt, chunkCount, chunkCount)).append("\n\n");
        }
        
        // Provide a summary based on what we've analyzed
        String summaryPrompt = "Based on the analyzed sections of the code file, " +
                              "provide a concise summary of the purpose and structure of this " +
                              getLanguageNameFromExtension(getFileExtension(filePath)) + " file. " +
                              "Focus on design patterns, code quality, and potential areas for improvement.";
        
        var summaryResponse = assistant.processMessage(new CoreAssistantMessage(summaryPrompt));
        if (summaryResponse != null) {
            result.append("# Summary\n\n");
            result.append(summaryResponse.getContent());
        }
        
        return result.toString();
    }
    
    /**
     * Analyze a single chunk of code.
     * 
     * @param chunk The code chunk to analyze
     * @param languagePrompt The language-specific prompt
     * @param chunkNumber The current chunk number
     * @param totalChunks The total number of chunks
     * @return The analysis for this chunk
     */
    private String analyzeCodeChunk(String chunk, String languagePrompt, int chunkNumber, int totalChunks) {
        // Build chunk-specific prompt
        String chunkPrompt = buildPromptWithLanguageContext(chunk, languagePrompt + 
                " This is chunk " + chunkNumber + " of " + totalChunks + 
                ". Focus on the most important elements in this section.");
        
        // Process with the assistant
        var response = assistant.processMessage(new CoreAssistantMessage(chunkPrompt));
        if (response != null) {
            return response.getContent();
        } else {
            return "Failed to analyze this code section.";
        }
    }
    
    /**
     * Build a complete prompt with language context.
     * 
     * @param content The code content
     * @param languagePrompt The language-specific prompt
     * @return The complete prompt
     */
    private String buildPromptWithLanguageContext(String content, String languagePrompt) {
        return "You are a code analysis expert.\n\n" +
               languagePrompt + "\n\n" +
               "Provide a clear and insightful analysis following these guidelines:\n" +
               "1. Identify the main components and their responsibilities\n" +
               "2. Explain key patterns and techniques used\n" +
               "3. Highlight notable code practices (both good and questionable)\n" +
               "4. Explain complex or non-obvious sections\n\n" +
               "CODE TO ANALYZE:\n\n" + content;
    }
    
    /**
     * Extract file extension from a path.
     * 
     * @param filePath The file path
     * @return The file extension (without the dot)
     */
    private String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty() || !filePath.contains(".")) {
            return "";
        }
        return filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Get language-specific prompt based on file extension.
     * 
     * @param extension The file extension
     * @return The language-specific prompt
     */
    private String getLanguagePrompt(String extension) {
        return languagePrompts.getOrDefault(extension, 
                "Analyze this code focusing on structure, logic flow, and potential issues.");
    }
    
    /**
     * Convert file extension to human-readable language name.
     * 
     * @param extension The file extension
     * @return Human-readable language name
     */
    private String getLanguageNameFromExtension(String extension) {
        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("java", "Java");
        languageNames.put("js", "JavaScript");
        languageNames.put("ts", "TypeScript");
        languageNames.put("py", "Python");
        languageNames.put("rb", "Ruby");
        languageNames.put("c", "C");
        languageNames.put("cpp", "C++");
        languageNames.put("cs", "C#");
        languageNames.put("go", "Go");
        languageNames.put("rs", "Rust");
        languageNames.put("php", "PHP");
        languageNames.put("kt", "Kotlin");
        languageNames.put("swift", "Swift");
        languageNames.put("scala", "Scala");
        languageNames.put("json", "JSON");
        languageNames.put("yaml", "YAML");
        languageNames.put("xml", "XML");
        languageNames.put("html", "HTML");
        languageNames.put("css", "CSS");
        languageNames.put("scss", "SCSS");
        languageNames.put("md", "Markdown");
        languageNames.put("sh", "Shell");
        languageNames.put("bash", "Bash");
        languageNames.put("sql", "SQL");
        languageNames.put("properties", "Properties");
        languageNames.put("gradle", "Gradle");
        
        return languageNames.getOrDefault(extension, "Code");
    }

    @Override
    public String getDescription() {
        return "Enhanced code analysis tool that provides language-specific understanding of source code files.\n\n" +
               "Usage: /codeexplain <file_path>\n\n" +
               "Features:\n" +
               "- Language detection based on file extension\n" +
               "- Specialized analysis for different programming languages\n" +
               "- Handling of large files through chunking\n" +
               "- Focus on structure, patterns, and best practices";
    }

    @Override
    public String getShortDescription() {
        return "Analyzes source code with language-specific understanding";
    }
}