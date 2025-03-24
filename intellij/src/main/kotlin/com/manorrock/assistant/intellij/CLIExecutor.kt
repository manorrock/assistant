package com.manorrock.assistant.intellij

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CLIExecutor {
    private val executor: Executor = Executors.newCachedThreadPool()
    private val cliPath: String
    private val javaPath: String

    init {
        // Similar to VSCode extension, get CLI path from user home
        this.cliPath = System.getProperty("user.home") + File.separator +
                ".manorrock" + File.separator +
                "assistant" + File.separator +
                "cli.jar"
        this.javaPath = getJavaPath()
    }

    private fun getJavaPath(): String {
        val javaHome = System.getProperty("java.home")
        return if (javaHome != null) {
            javaHome + File.separator + "bin" + File.separator + "java"
        } else {
            "java" // Fall back to PATH
        }
    }

    fun isCliAvailable(): Boolean {
        return File(cliPath).exists()
    }

    fun executeCommand(command: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync({
            try {
                val builder = ProcessBuilder(
                    javaPath,
                    "-jar",
                    cliPath,
                    "--stdin"
                )
                
                builder.redirectErrorStream(true)
                val process = builder.start()

                // Write command to stdin
                process.outputStream.write((command + "\n").toByteArray())
                process.outputStream.flush()
                process.outputStream.close()

                // Read the output before waiting for the process to complete
                val output = StringBuilder()
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        output.append(line).append("\n")
                    }
                }

                // Now wait for the process to complete
                val exitCode = process.waitFor()
                
                // Even if exit code is non-zero, return the output for debug purposes
                // This will allow us to see any error messages from the CLI
                if (exitCode != 0) {
                    // Add the exit code to the output for debugging
                    output.append("\nExit code: ").append(exitCode)
                    throw RuntimeException("CLI execution failed with code: " + exitCode + "\nOutput: " + output)
                }

                return@supplyAsync output.toString()
            } catch (e: IOException) {
                throw RuntimeException("Failed to execute CLI command: " + e.message, e)
            } catch (e: InterruptedException) {
                throw RuntimeException("CLI execution was interrupted: " + e.message, e)
            }
        }, executor)
    }
}
