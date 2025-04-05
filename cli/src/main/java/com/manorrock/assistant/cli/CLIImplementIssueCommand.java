package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.cli.issue.AcceptanceCriteriaVerifier;
import com.manorrock.assistant.cli.issue.Issue;
import com.manorrock.assistant.cli.issue.IssueParser;
import com.manorrock.assistant.cli.issue.IssueParsingException;
import com.manorrock.assistant.cli.issue.IssuePromptConstructor;
import com.manorrock.assistant.cli.issue.IssuePromptConstructor.DetailLevel;
import com.manorrock.assistant.cli.issue.ImplementationOutputHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command implementation for implementing issues through the CLI.
 * This command allows users to provide issue content and generates
 * an implementation based on the issue description.
 */
public class CLIImplementIssueCommand implements Command {
    
    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CLIImplementIssueCommand.class.getName());
    
    /**
     * Stores the assistant instance.
     */
    private final Assistant assistant;
    
    /**
     * The issue parser for extracting structured information from issue text.
     */
    private final IssueParser issueParser;
    
    /**
     * The prompt constructor for creating LLM prompts from issue data.
     */
    private final IssuePromptConstructor promptConstructor;
    
    /**
     * Implementation options storage.
     */
    private final Map<String, String> implementationOptions;
    
    /**
     * Constructor to initialize the command with the assistant.
     * 
     * @param assistant the assistant.
     */
    public CLIImplementIssueCommand(Assistant assistant) {
        this.assistant = assistant;
        this.issueParser = new IssueParser();
        this.promptConstructor = new IssuePromptConstructor();
        this.implementationOptions = new HashMap<>();
        
        // Set default implementation options
        implementationOptions.put("detail_level", DetailLevel.STANDARD.name());
        implementationOptions.put("optimize_for_llm", "true");
    }
    
    @Override
    public String execute(String input) {
        if (input == null || input.isEmpty() || input.equals("help")) {
            return getHelpText();
        }
        
        // Parse command arguments
        String[] parts = input.trim().split("\\s+", 2);
        String command = parts[0];
        
        try {
            switch (command) {
                case "file":
                    if (parts.length > 1) {
                        return implementIssueFromFile(parts[1]);
                    } else {
                        return "Error: No file path provided. Usage: /implement-issue file <path>";
                    }
                case "stdin":
                    return implementIssueFromStdin();
                case "options":
                    if (parts.length > 1) {
                        return setImplementationOptions(parts[1]);
                    } else {
                        return "Error: No options provided. Usage: /implement-issue options key=value[,key=value,...]";
                    }
                default:
                    // Assume the input is an issue text or a file path
                    if (isFilePath(parts[0])) {
                        return implementIssueFromFile(parts[0]);
                    } else {
                        // Treat the whole input as issue text
                        return implementIssueFromText(input);
                    }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing implement-issue command", e);
            return "Error implementing issue: " + e.getMessage();
        }
    }
    
    /**
     * Implement issue from a file.
     * 
     * @param filePath Path to the issue file
     * @return Implementation result or error message
     */
    private String implementIssueFromFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "Error: File not found: " + filePath;
            }
            
            String issueText = Files.readString(path);
            return implementIssueFromText(issueText);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading issue file", e);
            return "Error reading issue file: " + e.getMessage();
        }
    }
    
    /**
     * Implement issue from standard input.
     * 
     * @return Implementation result or error message
     */
    private String implementIssueFromStdin() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            
            System.out.println("Enter or paste issue content below (type 'DONE' on a new line when finished):");
            StringBuilder issueContent = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("DONE")) {
                    break;
                }
                issueContent.append(line).append("\n");
            }
            
            if (issueContent.length() == 0) {
                return "Error: No issue content provided";
            }
            
            return implementIssueFromText(issueContent.toString());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading from stdin", e);
            return "Error reading issue from standard input: " + e.getMessage();
        }
    }
    
    /**
     * Implement issue from text.
     * 
     * @param issueText The issue text
     * @return Implementation result or error message
     */
    private String implementIssueFromText(String issueText) {
        String workflowState = "parsing";
        Issue issue = null;
        String prompt = null;
        String implementationResult = null;
        
        try {
            // --- PARSING PHASE ---
            workflowState = "parsing";
            LOGGER.info("Starting issue implementation - parsing issue text");
            
            // Parse the issue text to extract structured information
            issue = issueParser.parse(issueText);
            LOGGER.info("Successfully parsed issue: " + issue.getTitle());
            LOGGER.info("Found " + issue.getAcceptanceCriteria().size() + " acceptance criteria");
            
            // --- CONTEXT GATHERING PHASE ---
            workflowState = "context_gathering";
            LOGGER.info("Gathering project context information");
            
            // Set any project context information from system properties
            setProjectContextFromProperties();
            
            // --- PROMPT CONSTRUCTION PHASE ---
            workflowState = "prompt_construction";
            LOGGER.info("Constructing implementation prompt");
            
            // Get the currently active LLM
            Llm activeLlm = assistant.getLlmManager().getLlm(assistant.getActiveLlm());
            
            // Get detail level from options
            DetailLevel detailLevel = DetailLevel.valueOf(
                    implementationOptions.getOrDefault("detail_level", DetailLevel.STANDARD.name()));
            
            // Build a prompt for the LLM based on the issue
            prompt = promptConstructor.constructPrompt(issue, detailLevel);
            
            // Optimize the prompt for the LLM if option is enabled
            boolean optimizeForLlm = Boolean.parseBoolean(
                    implementationOptions.getOrDefault("optimize_for_llm", "true"));
            
            if (optimizeForLlm) {
                prompt = promptConstructor.optimizePromptForLlm(prompt, activeLlm);
            }
            
            // Log verbose information if enabled
            if (Boolean.parseBoolean(System.getProperty("implement.issue.verbose", "false"))) {
                LOGGER.info("Constructed prompt for issue: " + issue.getTitle());
                LOGGER.info("Detail level: " + detailLevel);
                LOGGER.info("Prompt length: " + prompt.length() + " characters");
            }
            
            // --- IMPLEMENTATION PHASE ---
            workflowState = "implementation";
            LOGGER.info("Sending prompt to LLM for implementation");
            
            // Send the prompt to the LLM and get the implementation result
            implementationResult = activeLlm.process(prompt);
            LOGGER.info("Received implementation response from LLM");
            
            // --- VERIFICATION PHASE ---
            workflowState = "verification";
            LOGGER.info("Verifying implementation against acceptance criteria");
            
            // Format and return the result
            return formatImplementationResult(issue, implementationResult);
            
        } catch (IssueParsingException e) {
            LOGGER.log(Level.WARNING, "Error parsing issue", e);
            return "Error parsing issue: " + e.getMessage();
        } catch (Exception e) {
            // Provide different error messages based on the workflow state
            switch (workflowState) {
                case "parsing":
                    LOGGER.log(Level.SEVERE, "Error parsing issue text", e);
                    return "Error parsing issue: " + e.getMessage();
                case "context_gathering":
                    LOGGER.log(Level.SEVERE, "Error gathering project context", e);
                    return "Error gathering project context: " + e.getMessage();
                case "prompt_construction":
                    LOGGER.log(Level.SEVERE, "Error constructing prompt", e);
                    return "Error constructing implementation prompt: " + e.getMessage();
                case "implementation":
                    LOGGER.log(Level.SEVERE, "Error during LLM implementation", e);
                    return "Error during LLM implementation: " + e.getMessage();
                case "verification":
                    LOGGER.log(Level.SEVERE, "Error verifying implementation", e);
                    
                    // Partial recovery - try to return the implementation without verification
                    if (issue != null && implementationResult != null) {
                        LOGGER.info("Attempting partial recovery - returning implementation without verification");
                        return formatImplementationResult(issue, implementationResult);
                    }
                    
                    return "Error verifying implementation: " + e.getMessage();
                default:
                    LOGGER.log(Level.SEVERE, "Error implementing issue", e);
                    return "Error implementing issue: " + e.getMessage();
            }
        }
    }
    
    /**
     * Set project context information from system properties.
     */
    private void setProjectContextFromProperties() {
        // Get current working directory name as project name if not already set
        if (!promptConstructor.getProjectContext().containsKey("PROJECT_NAME") 
                || promptConstructor.getProjectContext().get("PROJECT_NAME").equals("Unknown Project")) {
            Path currentDir = Paths.get(System.getProperty("user.dir"));
            String projectName = currentDir.getFileName().toString();
            promptConstructor.setProjectContext("PROJECT_NAME", projectName);
        }
        
        // Set current directory
        promptConstructor.setProjectContext("CURRENT_DIRECTORY", System.getProperty("user.dir"));
        
        // Additional context could be loaded from a project configuration file if available
        Path configPath = Paths.get(System.getProperty("user.dir"), ".assistant-config.properties");
        if (Files.exists(configPath)) {
            try {
                Map<String, String> configProperties = new HashMap<>();
                Files.lines(configPath)
                        .filter(line -> line.contains("="))
                        .forEach(line -> {
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                configProperties.put(parts[0].trim(), parts[1].trim());
                            }
                        });
                
                // Map configuration properties to project context
                if (configProperties.containsKey("project.language")) {
                    promptConstructor.setProjectContext("LANGUAGE", configProperties.get("project.language"));
                }
                if (configProperties.containsKey("project.frameworks")) {
                    promptConstructor.setProjectContext("FRAMEWORKS", configProperties.get("project.frameworks"));
                }
                if (configProperties.containsKey("project.architecture")) {
                    promptConstructor.setProjectContext("ARCHITECTURE_PATTERN", configProperties.get("project.architecture"));
                }
                if (configProperties.containsKey("project.additional.context")) {
                    promptConstructor.setProjectContext("ADDITIONAL_CONTEXT", configProperties.get("project.additional.context"));
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error reading project configuration: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Send a prompt to the LLM and get the response.
     * 
     * @param prompt The prompt to send
     * @return The LLM's response
     */
    private String sendToLlm(String prompt) {
        try {
            // Get the currently active LLM
            Llm activeLlm = assistant.getLlmManager().getLlm(assistant.getActiveLlm());
            
            // Send the prompt to the LLM using the process method
            return activeLlm.process(prompt);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error communicating with LLM", e);
            throw new RuntimeException("Failed to communicate with LLM: " + e.getMessage(), e);
        }
    }
    
    /**
     * Format the implementation result.
     * 
     * @param issue The issue that was implemented
     * @param implementationResult The raw implementation result from the LLM
     * @return Formatted implementation result
     */
    private String formatImplementationResult(Issue issue, String implementationResult) {
        StringBuilder resultBuilder = new StringBuilder();
        
        // Process the LLM response using the ImplementationOutputHandler
        ImplementationOutputHandler outputHandler = new ImplementationOutputHandler(implementationResult);
        
        // Determine output format from options
        String outputFormat = implementationOptions.getOrDefault("output_format", "markdown");
        
        // Run the acceptance criteria verification
        boolean verifyImplementation = Boolean.parseBoolean(
                implementationOptions.getOrDefault("verify_implementation", "true"));
        
        AcceptanceCriteriaVerifier.VerificationReport verificationReport = null;
        if (verifyImplementation && !issue.getAcceptanceCriteria().isEmpty()) {
            // Create the verifier and run verification
            AcceptanceCriteriaVerifier verifier = new AcceptanceCriteriaVerifier(issue);
            verificationReport = verifier.verify(implementationResult);
        }
        
        // Format the header differently based on output format
        if ("markdown".equalsIgnoreCase(outputFormat)) {
            resultBuilder.append("# Implementation for Issue: ").append(issue.getTitle()).append("\n\n");
            
            // Add Implementation Result section that the test expects
            resultBuilder.append("## Implementation Result\n\n");
            
            // Format the implementation results using the markdown formatter
            String formattedResult = outputHandler.formatAsMarkdown();
            resultBuilder.append(formattedResult).append("\n\n");
            
            // Add verification section with automated checking if available
            resultBuilder.append("## Verification Against Acceptance Criteria\n\n");
            
            if (verificationReport != null) {
                resultBuilder.append(verificationReport.format()).append("\n\n");
                
                // Add checklist with automated status
                resultBuilder.append("### Acceptance Criteria Checklist\n\n");
                for (String criterion : issue.getAcceptanceCriteria()) {
                    boolean isSatisfied = verificationReport.getSatisfiedCriteria().contains(criterion);
                    resultBuilder.append("- [").append(isSatisfied ? "x" : " ").append("] ")
                                .append(criterion).append("\n");
                }
            } else {
                resultBuilder.append("Review the implementation above to verify it meets these acceptance criteria:\n\n");
                for (String criterion : issue.getAcceptanceCriteria()) {
                    resultBuilder.append("- [ ] ").append(criterion).append("\n");
                }
            }
        } else if ("terminal".equalsIgnoreCase(outputFormat)) {
            resultBuilder.append("=== Implementation for Issue: ").append(issue.getTitle()).append(" ===\n\n");
            
            // Add Implementation Result section that the test expects
            resultBuilder.append("=== Implementation Result ===\n\n");
            
            // Format the implementation results using the terminal formatter
            String formattedResult = outputHandler.formatForTerminal();
            resultBuilder.append(formattedResult).append("\n\n");
            
            // Add verification section with extracted code changes if available
            resultBuilder.append("=== Verification Against Acceptance Criteria ===\n\n");
            
            if (verificationReport != null) {
                resultBuilder.append(verificationReport.format()).append("\n\n");
                
                // Add checklist with automated status
                resultBuilder.append("Acceptance Criteria Checklist:\n\n");
                for (int i = 0; i < issue.getAcceptanceCriteria().size(); i++) {
                    String criterion = issue.getAcceptanceCriteria().get(i);
                    boolean isSatisfied = verificationReport.getSatisfiedCriteria().contains(criterion);
                    resultBuilder.append(i + 1).append(". [").append(isSatisfied ? "x" : " ").append("] ")
                                .append(criterion).append("\n");
                }
            } else {
                for (int i = 0; i < issue.getAcceptanceCriteria().size(); i++) {
                    resultBuilder.append(i + 1).append(". [ ] ").append(issue.getAcceptanceCriteria().get(i)).append("\n");
                }
            }
        } else {
            // Raw format - just return the unprocessed LLM response with a simple header
            resultBuilder.append("Implementation for Issue: ").append(issue.getTitle()).append("\n\n");
            resultBuilder.append("Implementation Result\n\n");
            resultBuilder.append(implementationResult);
            
            // Add verification section if available
            if (verificationReport != null) {
                resultBuilder.append("\n\nVerification Against Acceptance Criteria\n\n");
                resultBuilder.append(verificationReport.format());
            }
        }
        
        // Write output to file if output directory is specified
        String outputDir = System.getProperty("implement.issue.output.dir");
        if (outputDir != null && !outputDir.isEmpty()) {
            try {
                Path dirPath = Paths.get(outputDir);
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                }
                
                String sanitizedTitle = issue.getTitle().replaceAll("[^a-zA-Z0-9-_]", "_");
                String fileName = sanitizedTitle + "_implementation";
                
                // Add appropriate extension based on format
                if ("markdown".equalsIgnoreCase(outputFormat)) {
                    fileName += ".md";
                } else {
                    fileName += ".txt";
                }
                
                Path filePath = dirPath.resolve(fileName);
                
                Files.writeString(filePath, resultBuilder.toString());
                
                // Add note about saved file
                resultBuilder.append("\n\nImplementation saved to: ").append(filePath);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error writing implementation to file", e);
                resultBuilder.append("\n\nError writing implementation to file: ").append(e.getMessage());
            }
        }
        
        return resultBuilder.toString();
    }
    
    /**
     * Set implementation options.
     * 
     * @param optionsStr Options string in format key=value[,key=value,...]
     * @return Success or error message
     */
    private String setImplementationOptions(String optionsStr) {
        List<String> invalidOptions = new ArrayList<>();
        List<String> validOptions = new ArrayList<>();
        
        String[] options = optionsStr.split(",");
        for (String option : options) {
            String[] keyValue = option.trim().split("=", 2);
            if (keyValue.length < 2) {
                invalidOptions.add(option);
                continue;
            }
            
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            
            // Process valid option keys
            switch (key.toLowerCase()) {
                case "output_dir":
                    // Set output directory
                    Path outputDir = Paths.get(value);
                    if (!Files.exists(outputDir)) {
                        try {
                            Files.createDirectories(outputDir);
                        } catch (IOException e) {
                            return "Error creating output directory: " + e.getMessage();
                        }
                    }
                    System.setProperty("implement.issue.output.dir", value);
                    validOptions.add(key + "=" + value);
                    break;
                case "verbose":
                    // Set verbose mode
                    System.setProperty("implement.issue.verbose", Boolean.parseBoolean(value) ? "true" : "false");
                    validOptions.add(key + "=" + value);
                    break;
                case "max_tokens":
                    // Set max tokens for response
                    try {
                        int maxTokens = Integer.parseInt(value);
                        if (maxTokens > 0) {
                            System.setProperty("implement.issue.max.tokens", String.valueOf(maxTokens));
                            validOptions.add(key + "=" + value);
                        } else {
                            invalidOptions.add(option);
                        }
                    } catch (NumberFormatException e) {
                        invalidOptions.add(option);
                    }
                    break;
                case "detail_level":
                    // Set detail level for prompt construction
                    try {
                        DetailLevel detailLevel = DetailLevel.valueOf(value.toUpperCase());
                        implementationOptions.put("detail_level", detailLevel.name());
                        validOptions.add(key + "=" + value);
                    } catch (IllegalArgumentException e) {
                        invalidOptions.add(option);
                    }
                    break;
                case "optimize_for_llm":
                    // Set optimization flag for LLM
                    implementationOptions.put("optimize_for_llm", value);
                    validOptions.add(key + "=" + value);
                    break;
                case "verify_implementation":
                    // Set verification flag for acceptance criteria
                    boolean boolValue = Boolean.parseBoolean(value);
                    implementationOptions.put("verify_implementation", String.valueOf(boolValue));
                    validOptions.add(key + "=" + value);
                    break;
                case "output_format":
                    // Set output format
                    if (value.equalsIgnoreCase("markdown") || 
                        value.equalsIgnoreCase("terminal") || 
                        value.equalsIgnoreCase("raw")) {
                        implementationOptions.put("output_format", value.toLowerCase());
                        validOptions.add(key + "=" + value);
                    } else {
                        invalidOptions.add(option);
                    }
                    break;
                default:
                    invalidOptions.add(option);
                    break;
            }
        }
        
        StringBuilder responseBuilder = new StringBuilder();
        
        if (!validOptions.isEmpty()) {
            responseBuilder.append("Successfully set options: ").append(String.join(", ", validOptions)).append("\n");
        }
        
        if (!invalidOptions.isEmpty()) {
            responseBuilder.append("Invalid options: ").append(String.join(", ", invalidOptions)).append("\n");
            responseBuilder.append("Valid options are: output_dir=<path>, verbose=<true|false>, max_tokens=<number>, detail_level=<STANDARD|DETAILED>, optimize_for_llm=<true|false>, verify_implementation=<true|false>, output_format=<markdown|terminal|raw>");
        }
        
        return responseBuilder.toString().trim();
    }
    
    /**
     * Check if a string is likely a file path.
     * 
     * @param str The string to check
     * @return true if the string is likely a file path, false otherwise
     */
    private boolean isFilePath(String str) {
        // Check if string contains path separators or has a file extension
        return str.contains(File.separator) || str.contains("/") || str.contains("\\") || 
               str.matches(".*\\.[a-zA-Z0-9]+$");
    }
    
    /**
     * Get help text for the implement-issue command.
     * 
     * @return The help text
     */
    private String getHelpText() {
        return """
               Implement Issue Command Help:
               
               /implement-issue                        - Show this help text
               /implement-issue <issue_text>           - Implement issue from the provided text
               /implement-issue file <path>            - Implement issue from a file
               /implement-issue stdin                  - Implement issue from standard input
               /implement-issue options key=value,...  - Set implementation options
                                                      
               Available options:
               - output_dir=<path>    : Directory for implementation output
               - verbose=<true|false> : Enable verbose mode
               - max_tokens=<number>  : Maximum tokens for LLM response
               - detail_level=<STANDARD|DETAILED> : Level of detail for prompt construction
               - optimize_for_llm=<true|false> : Optimize prompt for LLM
               - verify_implementation=<true|false> : Verify implementation against acceptance criteria
               - output_format=<markdown|terminal|raw> : Format of the output
                                                      
               Examples:
               /implement-issue file ./issues/my-issue.md
               /implement-issue "# Add Login Feature\\n\\n## Description\\nImplement user login..."
               /implement-issue options output_dir=./implementations,verbose=true,verify_implementation=true
               """;
    }
    
    @Override
    public String getDescription() {
        return "Implements features or fixes from issue descriptions using the LLM";
    }
    
    @Override
    public String getShortDescription() {
        return "Implement issues automatically";
    }
}