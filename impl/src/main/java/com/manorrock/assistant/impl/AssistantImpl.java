package com.manorrock.assistant.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The shared implementation of the Assistant that executes commands using the CLI.
 */
@Deprecated
public class AssistantImpl {
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private final String cliPath;
    private final String javaPath;

    /**
     * Constructor defaulting to the standard CLI path.
     */
    public AssistantImpl() {
        this.cliPath = System.getProperty("user.home") + File.separator + 
                      ".manorrock" + File.separator + 
                      "assistant" + File.separator + 
                      "cli.jar";
        this.javaPath = getJavaPath();
    }
    
    /**
     * Constructor with a custom CLI path.
     * 
     * @param cliPath Path to the CLI jar
     */
    public AssistantImpl(String cliPath) {
        this.cliPath = cliPath;
        this.javaPath = getJavaPath();
    }

    /**
     * Get the Java executable path.
     * 
     * @return Path to the Java executable
     */
    private String getJavaPath() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            return javaHome + File.separator + "bin" + File.separator + "java";
        }
        return "java"; // Fall back to PATH
    }

    /**
     * Check if the CLI is available.
     * 
     * @return true if the CLI is available
     */
    public boolean isCliAvailable() {
        return new File(cliPath).exists();
    }

    /**
     * Execute a command using the CLI.
     * 
     * @param command Command to execute
     * @return CompletableFuture with the response
     */
    public CompletableFuture<String> executeCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(
                    javaPath,
                    "-jar",
                    cliPath,
                    "--stdin"
                );
                builder.redirectErrorStream(true);
                Process process = builder.start();

                // Write command to stdin
                process.getOutputStream().write((command + "\n").getBytes());
                process.getOutputStream().flush();
                process.getOutputStream().close();

                // Read the output before waiting for the process to complete
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                // Now wait for the process to complete
                int exitCode = process.waitFor();
                
                // Even if exit code is non-zero, return the output for debug purposes
                if (exitCode != 0) {
                    output.append("\nExit code: ").append(exitCode);
                    throw new RuntimeException("CLI execution failed with code: " + exitCode + "\nOutput: " + output);
                }

                return output.toString();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to execute CLI command: " + e.getMessage(), e);
            }
        }, EXECUTOR);
    }
}
