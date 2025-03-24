package com.manorrock.assistant.netbeans;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CLIExecutor {
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private final String cliPath;
    private final String javaPath;

    public CLIExecutor() {
        // Similar to VSCode extension, get CLI path from user home
        this.cliPath = System.getProperty("user.home") + File.separator + 
                      ".manorrock" + File.separator + 
                      "assistant" + File.separator + 
                      "cli.jar";
        this.javaPath = getJavaPath();
    }

    private String getJavaPath() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            return javaHome + File.separator + "bin" + File.separator + "java";
        }
        return "java"; // Fall back to PATH
    }

    public boolean isCliAvailable() {
        return new File(cliPath).exists();
    }

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

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("CLI execution failed with code: " + exitCode);
                }

                return output.toString();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to execute CLI command", e);
            }
        }, EXECUTOR);
    }
}
