package com.manorrock.assistant.cli.issue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for processing and formatting LLM responses related to issue implementations.
 * This class parses the raw LLM output, extracts code blocks, identifies implementation
 * steps, and formats the response for various output formats.
 */
public class ImplementationOutputHandler {
    
    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ImplementationOutputHandler.class.getName());
    
    /**
     * The raw implementation result from the LLM.
     */
    private final String rawImplementation;
    
    /**
     * Extracted code blocks from the implementation result.
     */
    private final List<CodeBlock> codeBlocks;
    
    /**
     * Extracted implementation steps from the result.
     */
    private final List<String> implementationSteps;
    
    /**
     * Constructor to initialize with the raw implementation result.
     * 
     * @param rawImplementation the raw implementation result from the LLM
     */
    public ImplementationOutputHandler(String rawImplementation) {
        this.rawImplementation = rawImplementation;
        this.codeBlocks = extractCodeBlocks(rawImplementation);
        this.implementationSteps = extractImplementationSteps(rawImplementation);
    }
    
    /**
     * Get the raw implementation result.
     * 
     * @return the raw implementation result
     */
    public String getRawImplementation() {
        return rawImplementation;
    }
    
    /**
     * Get the extracted code blocks.
     * 
     * @return the list of code blocks
     */
    public List<CodeBlock> getCodeBlocks() {
        return codeBlocks;
    }
    
    /**
     * Get the extracted implementation steps.
     * 
     * @return the list of implementation steps
     */
    public List<String> getImplementationSteps() {
        return implementationSteps;
    }
    
    /**
     * Format the implementation result as Markdown.
     * 
     * @return the formatted implementation result as Markdown
     */
    public String formatAsMarkdown() {
        StringBuilder builder = new StringBuilder();
        
        // Add implementation steps if available
        if (!implementationSteps.isEmpty()) {
            builder.append("## Implementation Steps\n\n");
            for (int i = 0; i < implementationSteps.size(); i++) {
                builder.append(i + 1).append(". ").append(implementationSteps.get(i)).append("\n");
            }
            builder.append("\n");
        }
        
        // Add code blocks section if available
        if (!codeBlocks.isEmpty()) {
            builder.append("## Code Changes\n\n");
            for (CodeBlock codeBlock : codeBlocks) {
                // Add file path if available
                if (codeBlock.getFilePath() != null && !codeBlock.getFilePath().isEmpty()) {
                    builder.append("### ").append(codeBlock.getFilePath()).append("\n\n");
                }
                
                // Add the code block with language
                builder.append("```")
                       .append(codeBlock.getLanguage().isEmpty() ? "" : codeBlock.getLanguage())
                       .append("\n")
                       .append(codeBlock.getCode())
                       .append("\n```\n\n");
            }
        }
        
        // If no structured content was extracted, return the raw implementation
        if (implementationSteps.isEmpty() && codeBlocks.isEmpty()) {
            return rawImplementation;
        }
        
        // Add additional context section
        builder.append("## Additional Information\n\n");
        builder.append("This implementation addresses the issue according to the specified requirements. ");
        builder.append("If you need to make adjustments, focus on modifying the code changes shown above.\n\n");
        
        return builder.toString();
    }
    
    /**
     * Format the implementation result for terminal display.
     * 
     * @return the formatted implementation result for terminal display
     */
    public String formatForTerminal() {
        StringBuilder builder = new StringBuilder();
        
        // Add implementation steps if available
        if (!implementationSteps.isEmpty()) {
            builder.append("=== IMPLEMENTATION STEPS ===\n\n");
            for (int i = 0; i < implementationSteps.size(); i++) {
                builder.append(i + 1).append(". ").append(implementationSteps.get(i)).append("\n");
            }
            builder.append("\n");
        }
        
        // Add code blocks section if available
        if (!codeBlocks.isEmpty()) {
            builder.append("=== CODE CHANGES ===\n\n");
            for (CodeBlock codeBlock : codeBlocks) {
                // Add file path if available
                if (codeBlock.getFilePath() != null && !codeBlock.getFilePath().isEmpty()) {
                    builder.append("--- ").append(codeBlock.getFilePath()).append(" ---\n\n");
                }
                
                // Add the code block
                builder.append(codeBlock.getCode()).append("\n\n");
            }
        }
        
        // If no structured content was extracted, return the raw implementation
        if (implementationSteps.isEmpty() && codeBlocks.isEmpty()) {
            return rawImplementation;
        }
        
        return builder.toString();
    }
    
    /**
     * Extract code blocks from the implementation result.
     * 
     * @param implementation the implementation result
     * @return the list of code blocks
     */
    private List<CodeBlock> extractCodeBlocks(String implementation) {
        List<CodeBlock> blocks = new ArrayList<>();
        
        // Pattern to match markdown code blocks and extract language and content
        // Group 1: language identifier (optional)
        // Group 2: code content
        Pattern codeBlockPattern = Pattern.compile("```([a-zA-Z0-9]*)?\\s*\\n([\\s\\S]*?)\\n```");
        Matcher matcher = codeBlockPattern.matcher(implementation);
        
        while (matcher.find()) {
            String language = matcher.group(1) != null ? matcher.group(1).trim() : "";
            String code = matcher.group(2);
            
            // Try to extract file path from context before code block
            String filePath = extractFilePathBeforeCodeBlock(
                    implementation.substring(0, matcher.start())
            );
            
            blocks.add(new CodeBlock(code, language, filePath));
        }
        
        // If no code blocks found with markdown syntax, try to find potential code sections
        if (blocks.isEmpty()) {
            extractPotentialCodeSections(implementation, blocks);
        }
        
        return blocks;
    }
    
    /**
     * Extract implementation steps from the implementation result.
     * 
     * @param implementation the implementation result
     * @return the list of implementation steps
     */
    private List<String> extractImplementationSteps(String implementation) {
        List<String> steps = new ArrayList<>();
        
        // Pattern to match numbered steps like "1. Do something" or "Step 1: Do something"
        Pattern stepPattern = Pattern.compile("(?:^|\\n)(?:Step\\s*(\\d+)[:.\\s]+|(?:\\s*)(\\d+)[\\.\\s]+)([^\\n]+)");
        Matcher matcher = stepPattern.matcher(implementation);
        
        while (matcher.find()) {
            String step = matcher.group(3).trim();
            if (!step.isEmpty()) {
                steps.add(step);
            }
        }
        
        // If no numbered steps found, look for sections marked with headers
        if (steps.isEmpty()) {
            Pattern sectionPattern = Pattern.compile("(?:^|\\n)#+\\s+([^\\n]+)");
            matcher = sectionPattern.matcher(implementation);
            
            while (matcher.find()) {
                String sectionTitle = matcher.group(1).trim();
                // Skip common section titles that aren't implementation steps
                if (!sectionTitle.matches("(?i)(?:Implementation|Steps|Code|Changes|Overview|Introduction|Conclusion)")) {
                    steps.add(sectionTitle);
                }
            }
        }
        
        return steps;
    }
    
    /**
     * Try to find potential code sections that aren't formatted as markdown code blocks.
     * 
     * @param implementation the implementation result
     * @param blocks the list to add found code blocks to
     */
    private void extractPotentialCodeSections(String implementation, List<CodeBlock> blocks) {
        // Look for indented blocks that might be code
        Pattern indentedBlockPattern = Pattern.compile("(?:^|\\n)((?:    [^\\n]+\\n)+)");
        Matcher matcher = indentedBlockPattern.matcher(implementation);
        
        while (matcher.find()) {
            String potentialCode = matcher.group(1);
            // Try to determine language based on common patterns
            String language = determineLanguageFromContent(potentialCode);
            blocks.add(new CodeBlock(potentialCode, language, null));
        }
    }
    
    /**
     * Extract a file path from the context before a code block.
     * 
     * @param context the context before the code block
     * @return the extracted file path, or null if none found
     */
    private String extractFilePathBeforeCodeBlock(String context) {
        // Check for common file path patterns like "Create a file at path/to/file.ext" or "In file.ext:"
        Pattern filePathPattern = Pattern.compile("(?:file|create|modify|edit|in|at)\\s+(?:a\\s+file\\s+(?:at|named)\\s+)?(?:\"([^\"]+)\"|`([^`]+)`|(\\S+\\.\\w+))\\s*(?::|\\n|$)");
        Matcher matcher = filePathPattern.matcher(context);
        
        // Find the last match, which is likely closest to the code block
        String filePath = null;
        while (matcher.find()) {
            filePath = matcher.group(1) != null ? matcher.group(1) : 
                      (matcher.group(2) != null ? matcher.group(2) : matcher.group(3));
        }
        
        return filePath;
    }
    
    /**
     * Try to determine the programming language from code content.
     * 
     * @param code the code content
     * @return the detected language, or an empty string if unknown
     */
    private String determineLanguageFromContent(String code) {
        code = code.toLowerCase();
        
        if (code.contains("public class") || code.contains("import java.") || code.contains("package ")) {
            return "java";
        } else if (code.contains("function") || code.contains("var ") || code.contains("const ") || code.contains("let ")) {
            return "javascript";
        } else if (code.contains("def ") || code.contains("import ") && !code.contains("{")) {
            return "python";
        } else if (code.contains("<html") || code.contains("<!doctype html")) {
            return "html";
        } else if (code.contains("select ") || code.contains("from ") || code.contains("where ")) {
            return "sql";
        } else if (code.contains("#include") || code.contains("int main(")) {
            return "c";
        } else if (code.contains("using namespace") || code.contains("std::")) {
            return "cpp";
        }
        
        return "";
    }
    
    /**
     * Inner class to represent a code block.
     */
    public static class CodeBlock {
        private final String code;
        private final String language;
        private final String filePath;
        
        /**
         * Constructor to initialize a code block.
         * 
         * @param code the code content
         * @param language the programming language
         * @param filePath the file path (can be null)
         */
        public CodeBlock(String code, String language, String filePath) {
            this.code = code;
            this.language = language != null ? language : "";
            this.filePath = filePath;
        }
        
        /**
         * Get the code content.
         * 
         * @return the code content
         */
        public String getCode() {
            return code;
        }
        
        /**
         * Get the programming language.
         * 
         * @return the programming language
         */
        public String getLanguage() {
            return language;
        }
        
        /**
         * Get the file path.
         * 
         * @return the file path, or null if not available
         */
        public String getFilePath() {
            return filePath;
        }
    }
}