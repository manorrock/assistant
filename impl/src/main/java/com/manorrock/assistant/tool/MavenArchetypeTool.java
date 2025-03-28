package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tool for scaffolding Maven projects using Maven archetypes.
 */
public class MavenArchetypeTool extends AbstractTool {
    
    private static final String NAME = "maven_archetype";
    private static final String DESCRIPTION = "Scaffolds a new Maven project using Maven archetypes";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
            new ToolParameter("command", "string", "Command to execute: 'scaffold' to create a project, or leave empty to list archetypes", false),
            new ToolParameter("archetypeGroupId", "string", "Group ID of the archetype (required for scaffold)", false),
            new ToolParameter("archetypeArtifactId", "string", "Artifact ID of the archetype (required for scaffold)", false),
            new ToolParameter("archetypeVersion", "string", "Version of the archetype (required for scaffold)", false),
            new ToolParameter("groupId", "string", "Group ID for the new project (required for scaffold)", false),
            new ToolParameter("artifactId", "string", "Artifact ID for the new project (required for scaffold)", false),
            new ToolParameter("version", "string", "Version for the new project (defaults to 1.0-SNAPSHOT)", false),
            new ToolParameter("package", "string", "Java package for the new project (defaults to groupId)", false),
            new ToolParameter("outputDirectory", "string", "Directory where the project should be created (defaults to current directory)", false),
            new ToolParameter("interactive", "boolean", "Whether to run Maven in interactive mode (defaults to false)", false),
            new ToolParameter("additionalProperties", "object", "Additional properties to pass to the archetype generator", false)
    );
    
    /**
     * Creates a new MavenArchetypeTool.
     */
    public MavenArchetypeTool() {
        super(NAME, DESCRIPTION, PARAMETERS);
    }
    
    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters) throws Exception {
        String command = parameters.containsKey("command") ? (String) parameters.get("command") : "";
        
        if ("scaffold".equalsIgnoreCase(command)) {
            return executeScaffold(parameters);
        } else {
            return executeListArchetypes();
        }
    }
    
    /**
     * Lists available Maven archetypes.
     * 
     * @return A ToolResult containing the list of available archetypes
     * @throws IOException If an error occurs during execution
     */
    private ToolResult executeListArchetypes() throws IOException {
        // Build the Maven command to list archetypes
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("archetype:generate");
        command.add("-Dcatalog=internal");
        command.add("-DarchetypeCatalog=internal");
        command.add("-DinteractiveMode=false");
        command.add("-B");
        
        // Execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Capture the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<String> output = reader.lines().collect(Collectors.toList());
        
        // Parse output to extract archetype information
        List<Map<String, String>> archetypes = parseArchetypeOutput(output);
        
        Map<String, Object> result = new HashMap<>();
        result.put("archetypes", archetypes);
        result.put("count", archetypes.size());
        
        return ToolResult.success(result);
    }
    
    /**
     * Parse Maven output to extract archetype information.
     * 
     * @param output The command output lines
     * @return A list of archetype information maps
     */
    private List<Map<String, String>> parseArchetypeOutput(List<String> output) {
        List<Map<String, String>> archetypes = new ArrayList<>();
        
        // Example pattern: [groupId]:[artifactId]:[version]
        for (String line : output) {
            if (line.contains(":") && line.contains("maven-archetype")) {
                try {
                    String[] parts = line.trim().split(":");
                    if (parts.length >= 3) {
                        Map<String, String> archetype = new HashMap<>();
                        archetype.put("groupId", parts[0]);
                        archetype.put("artifactId", parts[1]);
                        archetype.put("version", parts[2]);
                        archetypes.add(archetype);
                    }
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }
        
        return archetypes;
    }
    
    /**
     * Executes the scaffold command to create a new Maven project.
     * 
     * @param parameters The parameters for project creation
     * @return A ToolResult indicating success or failure
     * @throws IOException If IO errors occur
     * @throws InterruptedException If the process is interrupted
     */
    private ToolResult executeScaffold(Map<String, Object> parameters) throws IOException, InterruptedException {
        // Extract required parameters
        if (!parameters.containsKey("archetypeGroupId") || !parameters.containsKey("archetypeArtifactId") ||
            !parameters.containsKey("archetypeVersion") || !parameters.containsKey("groupId") || 
            !parameters.containsKey("artifactId")) {
            return ToolResult.failure("Missing required parameters for scaffold command. " +
                "Required: archetypeGroupId, archetypeArtifactId, archetypeVersion, groupId, artifactId");
        }
        
        String archetypeGroupId = (String) parameters.get("archetypeGroupId");
        String archetypeArtifactId = (String) parameters.get("archetypeArtifactId");
        String archetypeVersion = (String) parameters.get("archetypeVersion");
        String groupId = (String) parameters.get("groupId");
        String artifactId = (String) parameters.get("artifactId");
        
        // Extract optional parameters with defaults
        String version = parameters.containsKey("version") ? (String) parameters.get("version") : "1.0-SNAPSHOT";
        String packageName = parameters.containsKey("package") ? (String) parameters.get("package") : groupId;
        String outputDirectory = parameters.containsKey("outputDirectory") ? 
                (String) parameters.get("outputDirectory") : System.getProperty("user.dir");
        boolean interactive = parameters.containsKey("interactive") && (boolean) parameters.get("interactive");
        
        // Create the output directory if it doesn't exist
        Path outputPath = Paths.get(outputDirectory);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        // Build the Maven command
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("archetype:generate");
        command.add("-DarchetypeGroupId=" + archetypeGroupId);
        command.add("-DarchetypeArtifactId=" + archetypeArtifactId);
        command.add("-DarchetypeVersion=" + archetypeVersion);
        command.add("-DgroupId=" + groupId);
        command.add("-DartifactId=" + artifactId);
        command.add("-Dversion=" + version);
        command.add("-Dpackage=" + packageName);
        
        // Add additional properties if provided
        if (parameters.containsKey("additionalProperties") && parameters.get("additionalProperties") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalProps = (Map<String, Object>) parameters.get("additionalProperties");
            for (Map.Entry<String, Object> entry : additionalProps.entrySet()) {
                command.add("-D" + entry.getKey() + "=" + entry.getValue().toString());
            }
        }
        
        // Set batch mode unless interactive is true
        if (!interactive) {
            command.add("-B");
        }
        
        // Execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(outputDirectory));
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Capture the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<String> output = reader.lines().collect(Collectors.toList());
        
        int exitCode = process.waitFor();
        
        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("exitCode", exitCode);
        result.put("output", String.join("\n", output));
        
        // Check project creation success
        Path projectPath = Paths.get(outputDirectory, artifactId);
        boolean projectCreated = Files.exists(projectPath) && Files.isDirectory(projectPath);
        result.put("projectCreated", projectCreated);
        
        // Include project structure if created
        if (projectCreated) {
            result.put("projectDirectory", projectPath.toString());
            result.put("pomExists", Files.exists(projectPath.resolve("pom.xml")));
            
            // Add project metadata
            Map<String, String> projectMetadata = new HashMap<>();
            projectMetadata.put("groupId", groupId);
            projectMetadata.put("artifactId", artifactId);
            projectMetadata.put("version", version);
            projectMetadata.put("package", packageName);
            result.put("projectMetadata", projectMetadata);
        }
        
        if (exitCode == 0 && projectCreated) {
            return ToolResult.success(result);
        } else {
            // Create a combined result with both error message and data
            Map<String, Object> errorResult = new HashMap<>(result);
            errorResult.put("error", "Maven archetype generation failed with exit code: " + exitCode);
            return ToolResult.failure("Maven archetype generation failed: " + exitCode);
        }
    }
    
    /**
     * Helper method to verify that Maven is installed and available.
     * 
     * @return true if Maven is available, false otherwise
     */
    public boolean isMavenAvailable() {
        try {
            Process process = new ProcessBuilder("mvn", "--version").start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
