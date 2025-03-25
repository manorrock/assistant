package com.manorrock.assistant.cli;

import com.manorrock.assistant.shared.Tool;
import com.manorrock.assistant.shared.ToolManager;
import com.manorrock.assistant.shared.DefaultToolManager;
import com.manorrock.assistant.shared.ToolLifecycle;
import com.manorrock.assistant.shared.tools.FileReadTool;
import com.manorrock.assistant.shared.tools.FileWriteTool;
import com.manorrock.assistant.shared.tools.DirectoryListTool;
import com.manorrock.assistant.shared.tools.ProcessExecutionTool;
import com.manorrock.assistant.shared.tools.ProjectStructureAnalysisTool;
import com.manorrock.assistant.shared.tools.DependencyAnalysisTool;
import com.manorrock.assistant.shared.tools.WebScraperTool;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI-specific implementation of the Tool Registry.
 * This class is responsible for registering the default set of tools,
 * discovering custom tools, and managing the tool lifecycle.
 */
public class CliToolRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(CliToolRegistry.class.getName());
    
    private final ToolManager toolManager;
    private final List<Tool> registeredTools;
    private final List<String> customToolDirectories;
    
    /**
     * Create a new CLI Tool Registry with the default tool manager.
     */
    public CliToolRegistry() {
        this(new DefaultToolManager());
    }
    
    /**
     * Create a new CLI Tool Registry with the specified tool manager.
     * 
     * @param toolManager The tool manager to use
     */
    public CliToolRegistry(ToolManager toolManager) {
        this.toolManager = toolManager;
        this.registeredTools = new ArrayList<>();
        this.customToolDirectories = new ArrayList<>();
        
        // Add default tool locations
        this.customToolDirectories.add(System.getProperty("user.home") + "/.manorrock/assistant/tools");
        
        // Register default tools
        registerDefaultTools();
    }
    
    /**
     * Register the default set of tools.
     */
    private void registerDefaultTools() {
        // File system tools
        registerTool(new FileReadTool());
        registerTool(new FileWriteTool());
        registerTool(new DirectoryListTool());
        
        // Process tools
        registerTool(new ProcessExecutionTool());
        
        // Project analysis tools
        registerTool(new ProjectStructureAnalysisTool());
        registerTool(new DependencyAnalysisTool());
        
        // Web tools
        registerTool(new WebScraperTool());
        
        LOGGER.info("Registered " + registeredTools.size() + " default tools");
    }
    
    /**
     * Register a tool with the tool manager.
     * 
     * @param tool The tool to register
     * @return true if the tool was registered successfully, false otherwise
     */
    public boolean registerTool(Tool tool) {
        try {
            validateTool(tool);
            toolManager.registerTool(tool);
            registeredTools.add(tool);
            LOGGER.info("Registered tool: " + tool.getName());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to register tool: " + 
                    (tool != null ? tool.getName() : "null"), e);
            return false;
        }
    }
    
    /**
     * Unregister a tool from the tool manager.
     * 
     * @param toolName The name of the tool to unregister
     * @return true if the tool was unregistered successfully, false otherwise
     */
    public boolean unregisterTool(String toolName) {
        try {
            toolManager.unregisterTool(toolName);
            registeredTools.removeIf(tool -> tool.getName().equals(toolName));
            LOGGER.info("Unregistered tool: " + toolName);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to unregister tool: " + toolName, e);
            return false;
        }
    }
    
    /**
     * Enable a tool that was previously disabled.
     * 
     * @param toolName The name of the tool to enable
     * @return true if the tool was enabled, false if not found or already enabled
     */
    public boolean enableTool(String toolName) {
        Tool tool = findTool(toolName);
        if (tool != null && tool.getLifecycle() == ToolLifecycle.DISABLED) {
            toolManager.enableTool(toolName);
            LOGGER.info("Enabled tool: " + toolName);
            return true;
        }
        return false;
    }
    
    /**
     * Disable a tool temporarily.
     * 
     * @param toolName The name of the tool to disable
     * @return true if the tool was disabled, false if not found or already disabled
     */
    public boolean disableTool(String toolName) {
        Tool tool = findTool(toolName);
        if (tool != null && tool.getLifecycle() == ToolLifecycle.READY) {
            toolManager.disableTool(toolName);
            LOGGER.info("Disabled tool: " + toolName);
            return true;
        }
        return false;
    }
    
    /**
     * Get the current lifecycle state of a tool.
     * 
     * @param toolName The name of the tool
     * @return The tool's lifecycle state, or null if tool not found
     */
    public ToolLifecycle getToolLifecycle(String toolName) {
        Tool tool = findTool(toolName);
        return tool != null ? tool.getLifecycle() : null;
    }
    
    /**
     * Find a tool by name.
     * 
     * @param toolName The name of the tool to find
     * @return The tool if found, null otherwise
     */
    private Tool findTool(String toolName) {
        return registeredTools.stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Add a directory to search for custom tools.
     * 
     * @param directory The directory to add
     */
    public void addCustomToolDirectory(String directory) {
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory()) {
            customToolDirectories.add(directory);
            LOGGER.info("Added custom tool directory: " + directory);
        } else {
            LOGGER.warning("Custom tool directory does not exist or is not a directory: " + directory);
        }
    }
    
    /**
     * Discover and register tools from custom directories.
     * 
     * @return The number of custom tools registered
     */
    public int discoverAndRegisterCustomTools() {
        int count = 0;
        
        // First, use ServiceLoader to discover tools on the classpath
        ServiceLoader<Tool> serviceLoader = ServiceLoader.load(Tool.class);
        for (Tool tool : serviceLoader) {
            if (registerTool(tool)) {
                count++;
            }
        }
        
        // Then, scan custom directories
        for (String directory : customToolDirectories) {
            count += scanDirectoryForTools(directory);
        }
        
        LOGGER.info("Discovered and registered " + count + " custom tools");
        return count;
    }
    
    /**
     * Scan a directory for custom tools.
     * 
     * @param directory The directory to scan
     * @return The number of tools registered from this directory
     */
    private int scanDirectoryForTools(String directory) {
        // This is a placeholder for the actual implementation
        // In a real implementation, this would:
        // 1. Scan the directory for JAR files or scripts
        // 2. Load those as tools
        // 3. Register them with the tool manager
        LOGGER.info("Scanning directory for tools: " + directory);
        return 0;
    }
    
    /**
     * Get the tool manager.
     * 
     * @return The tool manager
     */
    public ToolManager getToolManager() {
        return toolManager;
    }
    
    /**
     * Get the list of registered tools.
     * 
     * @return The list of registered tools
     */
    public List<Tool> getRegisteredTools() {
        return new ArrayList<>(registeredTools);
    }
    
    /**
     * Validate a tool before registration.
     * 
     * @param tool The tool to validate
     * @throws IllegalArgumentException If the tool is invalid
     */
    private void validateTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        
        if (tool.getName() == null || tool.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }
        
        if (tool.getDescription() == null || tool.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool description cannot be null or empty");
        }
        
        if (tool.getParameters() == null) {
            throw new IllegalArgumentException("Tool parameters cannot be null");
        }
        
        // Check for duplicate tool names
        for (Tool registeredTool : registeredTools) {
            if (registeredTool.getName().equals(tool.getName())) {
                throw new IllegalArgumentException("Tool with name '" + tool.getName() + "' is already registered");
            }
        }
    }
}
