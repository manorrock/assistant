package com.manorrock.assistant.cli.issue;

import com.manorrock.assistant.api.Llm;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for constructing effective prompts from issue data for LLM processing.
 * This class follows the single responsibility principle by focusing only on prompt construction.
 * 
 * <p>
 * The {@code IssuePromptConstructor} provides methods to format data from {@code Issue} objects
 * into well-structured prompts for LLMs, with options to customize the prompt format and detail level.
 * </p>
 * 
 * @author Manorrock Assistant
 */
public class IssuePromptConstructor {
    
    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(IssuePromptConstructor.class.getName());
    
    /**
     * Default system message template for issue implementation.
     */
    private static final String DEFAULT_SYSTEM_MESSAGE = 
            "You are an expert software developer tasked with implementing features from issue descriptions. "
            + "Your goal is to analyze the issue, understand the requirements, and implement the necessary code changes.";
    
    /**
     * Project context information for the prompts.
     */
    private final Map<String, String> projectContext;
    
    /**
     * System message template to be used at the beginning of the prompt.
     */
    private String systemMessageTemplate;
    
    /**
     * Constructor with default system message.
     */
    public IssuePromptConstructor() {
        this(DEFAULT_SYSTEM_MESSAGE);
    }
    
    /**
     * Constructor with custom system message.
     * 
     * @param systemMessage The system message template to use
     */
    public IssuePromptConstructor(String systemMessage) {
        this.systemMessageTemplate = systemMessage;
        this.projectContext = new HashMap<>();
        
        // Initialize with default project context values
        projectContext.put("PROJECT_NAME", "Unknown Project");
        projectContext.put("LANGUAGE", "Java");
        projectContext.put("FRAMEWORKS", "None");
        projectContext.put("ARCHITECTURE_PATTERN", "Unknown");
        projectContext.put("CURRENT_DIRECTORY", System.getProperty("user.dir"));
        projectContext.put("ADDITIONAL_CONTEXT", "");
    }
    
    /**
     * Construct a prompt from the issue data with standard detail level.
     * 
     * @param issue The issue data
     * @return A formatted prompt string
     */
    public String constructPrompt(Issue issue) {
        return constructPrompt(issue, DetailLevel.STANDARD);
    }
    
    /**
     * Construct a prompt from the issue data with specified detail level.
     * 
     * @param issue The issue data
     * @param detailLevel The level of detail to include in the prompt
     * @return A formatted prompt string
     */
    public String constructPrompt(Issue issue, DetailLevel detailLevel) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Add system message
        addSystemMessage(promptBuilder);
        
        // Add issue title and header
        promptBuilder.append("# IMPLEMENT ISSUE: ").append(issue.getTitle()).append("\n\n");
        
        // Add issue description
        promptBuilder.append("## Description\n").append(issue.getDescription()).append("\n\n");
        
        // Add acceptance criteria
        promptBuilder.append("## Acceptance Criteria\n");
        for (String criterion : issue.getAcceptanceCriteria()) {
            promptBuilder.append("- ").append(criterion).append("\n");
        }
        promptBuilder.append("\n");
        
        // Add additional details based on detail level
        if (detailLevel != DetailLevel.MINIMAL) {
            // Add priority
            promptBuilder.append("## Priority: ").append(issue.getPriority()).append("\n\n");
            
            // Add labels if any
            if (!issue.getLabels().isEmpty()) {
                promptBuilder.append("## Labels: ");
                promptBuilder.append(String.join(", ", issue.getLabels())).append("\n\n");
            }
        }
        
        // Add project context
        if (detailLevel == DetailLevel.COMPREHENSIVE) {
            addProjectContext(promptBuilder);
        }
        
        // Add implementation instructions
        addImplementationInstructions(promptBuilder, detailLevel);
        
        return promptBuilder.toString();
    }
    
    /**
     * Optimize the prompt for a specific LLM by adjusting format and length.
     * 
     * @param prompt The original prompt
     * @param llm The target LLM
     * @return An optimized prompt for the specific LLM
     */
    public String optimizePromptForLlm(String prompt, Llm llm) {
        // Get the LLM properties
        String llmName = llm.getProperties().getProperty("model", "unknown").toLowerCase();
        int maxContextLength = getMaxContextLength(llm);
        
        // Adjust prompt based on LLM-specific requirements
        String optimizedPrompt = prompt;
        
        // Check if prompt exceeds max context length and truncate if necessary
        if (prompt.length() > maxContextLength) {
            LOGGER.log(Level.WARNING, 
                    "Prompt exceeds maximum context length for LLM {0}. Truncating...", llmName);
            
            // Truncate while preserving core content
            optimizedPrompt = truncatePrompt(prompt, maxContextLength);
        }
        
        // Add LLM-specific formatting if needed
        if (llmName.contains("llama")) {
            // For Llama models, ensure clear separation of sections
            optimizedPrompt = optimizedPrompt.replace("\n\n", "\n\n");
        } else if (llmName.contains("gpt")) {
            // For GPT models, use their preferred formatting
            // (currently no special adjustments needed)
        }
        
        return optimizedPrompt;
    }
    
    /**
     * Set the system message template.
     * 
     * @param systemMessage The new system message template
     * @return This constructor instance for method chaining
     */
    public IssuePromptConstructor setSystemMessageTemplate(String systemMessage) {
        if (systemMessage != null && !systemMessage.isBlank()) {
            this.systemMessageTemplate = systemMessage;
        }
        return this;
    }
    
    /**
     * Set a project context value.
     * 
     * @param key The context key
     * @param value The context value
     * @return This constructor instance for method chaining
     */
    public IssuePromptConstructor setProjectContext(String key, String value) {
        if (key != null && !key.isBlank() && value != null) {
            this.projectContext.put(key, value);
        }
        return this;
    }
    
    /**
     * Set multiple project context values.
     * 
     * @param contextMap Map of context keys and values
     * @return This constructor instance for method chaining
     */
    public IssuePromptConstructor setProjectContext(Map<String, String> contextMap) {
        if (contextMap != null) {
            this.projectContext.putAll(contextMap);
        }
        return this;
    }
    
    /**
     * Get the current project context.
     * 
     * @return An unmodifiable map of the current project context
     */
    public Map<String, String> getProjectContext() {
        return Map.copyOf(projectContext);
    }
    
    /**
     * Get the current system message template.
     * 
     * @return The current system message template
     */
    public String getSystemMessageTemplate() {
        return systemMessageTemplate;
    }
    
    /**
     * Add the system message to the prompt.
     * 
     * @param promptBuilder The prompt builder to append to
     */
    private void addSystemMessage(StringBuilder promptBuilder) {
        promptBuilder.append(systemMessageTemplate).append("\n\n");
    }
    
    /**
     * Add project context information to the prompt.
     * 
     * @param promptBuilder The prompt builder to append to
     */
    private void addProjectContext(StringBuilder promptBuilder) {
        promptBuilder.append("## Project Context\n");
        
        for (Map.Entry<String, String> entry : projectContext.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                promptBuilder.append("- ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append("\n");
            }
        }
        
        promptBuilder.append("\n");
    }
    
    /**
     * Add implementation instructions to the prompt.
     * 
     * @param promptBuilder The prompt builder to append to
     * @param detailLevel The level of detail to include
     */
    private void addImplementationInstructions(StringBuilder promptBuilder, DetailLevel detailLevel) {
        promptBuilder.append("## Implementation Instructions\n");
        
        switch (detailLevel) {
            case MINIMAL:
                promptBuilder.append("Implement this issue according to the description and acceptance criteria.\n");
                break;
            case STANDARD:
                promptBuilder.append("Please implement this issue according to the description and acceptance criteria above.\n");
                promptBuilder.append("1. Analyze the issue and describe your implementation approach\n");
                promptBuilder.append("2. List the files that need to be modified or created\n");
                promptBuilder.append("3. Provide the code changes required for each file\n");
                promptBuilder.append("4. Verify that your implementation satisfies all acceptance criteria\n");
                break;
            case COMPREHENSIVE:
                promptBuilder.append("Please implement this issue according to the description and acceptance criteria above.\n");
                promptBuilder.append("1. Analyze the issue carefully and describe your implementation approach in detail\n");
                promptBuilder.append("2. Explain the design decisions and tradeoffs you're making\n");
                promptBuilder.append("3. List all files that need to be modified or created\n");
                promptBuilder.append("4. Provide detailed code changes required for each file\n");
                promptBuilder.append("5. Include any necessary unit tests or validation steps\n");
                promptBuilder.append("6. Verify that your implementation satisfies all acceptance criteria\n");
                promptBuilder.append("7. Consider edge cases and error handling carefully\n");
                promptBuilder.append("8. Suggest any additional improvements or future work\n");
                break;
        }
        
        promptBuilder.append("\nBegin your implementation now.\n");
    }
    
    /**
     * Truncate a prompt to fit within the maximum context length.
     * 
     * @param prompt The prompt to truncate
     * @param maxLength The maximum length in characters
     * @return A truncated prompt
     */
    private String truncatePrompt(String prompt, int maxLength) {
        if (prompt.length() <= maxLength) {
            return prompt;
        }
        
        // Split prompt into sections
        String[] sections = prompt.split("\n\n");
        
        // Always include the system message and issue title
        StringBuilder truncatedPrompt = new StringBuilder();
        int usedLength = 0;
        
        // Add system message and title (first two sections)
        for (int i = 0; i < Math.min(2, sections.length); i++) {
            truncatedPrompt.append(sections[i]).append("\n\n");
            usedLength += sections[i].length() + 2;
        }
        
        // Add acceptance criteria (critical information)
        for (int i = 2; i < sections.length; i++) {
            if (sections[i].startsWith("## Acceptance Criteria")) {
                truncatedPrompt.append(sections[i]).append("\n\n");
                usedLength += sections[i].length() + 2;
                break;
            }
        }
        
        // Add instructions section (always at the end)
        for (int i = sections.length - 1; i >= 0; i--) {
            if (sections[i].startsWith("## Implementation Instructions")) {
                // Check if adding this would exceed the limit
                if (usedLength + sections[i].length() + 2 <= maxLength) {
                    truncatedPrompt.append(sections[i]).append("\n\n");
                } else {
                    // Add a shorter version of the instructions
                    truncatedPrompt.append("## Implementation Instructions\n");
                    truncatedPrompt.append("Implement this issue according to the description and criteria.\n\n");
                }
                break;
            }
        }
        
        // If we still have room, add description
        for (int i = 2; i < sections.length; i++) {
            if (sections[i].startsWith("## Description")) {
                int remainingSpace = maxLength - truncatedPrompt.length();
                if (remainingSpace > 100) { // Ensure at least 100 chars of description
                    if (sections[i].length() <= remainingSpace) {
                        truncatedPrompt.append(sections[i]).append("\n\n");
                    } else {
                        // Truncate the description
                        String description = sections[i];
                        truncatedPrompt.append(description.substring(0, remainingSpace - 20))
                                .append("... [truncated]\n\n");
                    }
                }
                break;
            }
        }
        
        return truncatedPrompt.toString().trim();
    }
    
    /**
     * Get the maximum context length for an LLM.
     * This could be enhanced to use actual LLM properties when available.
     * 
     * @param llm The LLM to check
     * @return The estimated maximum context length in characters
     */
    private int getMaxContextLength(Llm llm) {
        // Default values based on known models
        String llmName = llm.getProperties().getProperty("model", "unknown").toLowerCase();
        
        if (llmName.contains("gpt-4")) {
            return 24000; // Approx 8k tokens
        } else if (llmName.contains("gpt-3.5")) {
            return 12000; // Approx 4k tokens
        } else if (llmName.contains("llama-3")) {
            return 24000; // Approx 8k tokens
        } else if (llmName.contains("llama-2")) {
            return 12000; // Approx 4k tokens
        } else {
            return 6000; // Conservative default
        }
    }
    
    /**
     * Enum representing the level of detail to include in the prompt.
     */
    public enum DetailLevel {
        /**
         * Minimal detail level - core information only.
         */
        MINIMAL,
        
        /**
         * Standard detail level - balanced approach suitable for most issues.
         */
        STANDARD,
        
        /**
         * Comprehensive detail level - extensive information and guidance.
         */
        COMPREHENSIVE
    }
}