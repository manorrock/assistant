package com.manorrock.assistant.intellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.manorrock.assistant.api.AssistantMessage
import com.manorrock.assistant.core.CoreAssistant
import com.manorrock.assistant.core.CoreAssistantMessage
import org.jetbrains.annotations.NotNull
import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.LinkedList
import java.util.UUID
import org.json.JSONObject
import org.json.JSONArray
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.function.Consumer

class AssistantToolWindow : ToolWindowFactory, ActionListener {
    private lateinit var responseArea: JTextArea
    private lateinit var requestArea: JTextArea
    private lateinit var sendButton: JButton
    private lateinit var progressBar: JProgressBar
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
    private val assistant: CoreAssistant = CoreAssistant()
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        responseArea = JTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
        }
        requestArea = JTextArea()
        sendButton = JButton("Send")
        progressBar = JProgressBar(0, 100)

        // Display welcome message (same as NetBeans)
        responseArea.text = "Welcome to Manorrock Assistant\n\nType /help for a list of commands."

        // Setup key event handler for the requestArea
        requestArea.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(event: java.awt.event.KeyEvent) {
                if (event.keyCode == java.awt.event.KeyEvent.VK_ENTER && !event.isShiftDown) {
                    handleSendAction()
                    event.consume() // Prevents the newline from being added
                }
            }
        })

        sendButton.addActionListener(this)

        // Layout setup (simplified)
        panel.add(JScrollPane(responseArea), BorderLayout.CENTER)
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.add(JScrollPane(requestArea), BorderLayout.CENTER)
        val buttonPanel = JPanel()
        buttonPanel.add(sendButton)
        bottomPanel.add(buttonPanel, BorderLayout.EAST)
        bottomPanel.add(progressBar, BorderLayout.SOUTH)
        panel.add(bottomPanel, BorderLayout.SOUTH)

        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.source) {
            sendButton -> handleSendAction()
        }
    }

    private fun handleSendAction() {
        val userMessage = requestArea.text.trim()

        if (userMessage.isNotEmpty()) {
            val timestamp = LocalDateTime.now().format(formatter)
            responseArea.append("\n\nYou: $userMessage")
            requestArea.text = ""
            
            // Handle special commands
            if (userMessage == "/new") {
                responseArea.text = ""
                responseArea.append("Started a new chat session.\n")
                // Reset assistant state
                assistant.reset()
                return
            }
            
            if (handleUiCommand(userMessage)) {
                return
            }
            
            // Process through CoreAssistant
            processMessage(userMessage)
        }
    }

    private fun handleUiCommand(command: String): Boolean {
        when {
            command == "/help" -> {
                showHelp()
                return true
            }
            command == "/clear" -> {
                clearResponseArea()
                return true
            }
            command.startsWith("/explain") -> {
                // Always pass to handleExplain which will determine appropriate action
                handleExplain(command)
                return true
            }
            // Let other commands be handled by CoreAssistant
            else -> return false
        }
    }

    private fun showHelp() {
        // Indicate that help is being fetched
        responseArea.append("\n\nFetching available commands...")
        
        // Create a help message
        val helpMessage = CoreAssistantMessage("/help")
        assistant.sendMessage(helpMessage)
            .thenAccept { response ->
                javax.swing.SwingUtilities.invokeLater {
                    // Clear the "Fetching..." message
                    responseArea.text = responseArea.text.replace("\n\nFetching available commands...", "")
                    
                    // Clean and deduplicate the combined help output
                    val cleanedOutput = cleanHelpOutput(response.content)
                    
                    // Show all commands
                    responseArea.append("\n\nAvailable Commands:\n$cleanedOutput")
                    
                    // Ensure caret is at the end to show the help
                    responseArea.caretPosition = responseArea.document.length
                }
            }
            .exceptionally { e ->
                javax.swing.SwingUtilities.invokeLater {
                    // If help command fails, just show UI help
                    responseArea.text = responseArea.text.replace("\n\nFetching available commands...", "")
                    
                    val uiHelpMessage = """
                        |/explain - Explain the selected text or current document
                        |/help - Show this help message
                        |/clear - Clear the response window
                        |/new - Start a new chat session
                    """.trimMargin()
                    
                    responseArea.append("\n\nSystem: Unable to fetch all commands: ${e.message}")
                    responseArea.append("\n\nAvailable Commands:\n$uiHelpMessage")
                    responseArea.caretPosition = responseArea.document.length
                }
                null
            }
    }
    
    /**
     * Cleans up the help output by removing duplicates and ensuring consistency.
     */
    private fun cleanHelpOutput(cliHelpResponse: String): String {
        // Extract all commands from the CLI response
        val cliCommands = cliHelpResponse.lines().filter { it.trim().startsWith("/") }
        
        // Define UI-specific commands
        val uiCommands = listOf(
            "/explain - Explain the selected text or current document",
            "/help - Show this help message",
            "/clear - Clear the response window",
            "/new - Start a new chat session"
        )
        
        // Combine all commands and remove duplicates based on the command name (before the space)
        val allCommands = (cliCommands + uiCommands)
            .filter { it.trim().isNotEmpty() }
            .map { it.trim() }
            .distinctBy { it.substringBefore(" ") }
            .sorted()
        
        return allCommands.joinToString("\n")
    }

    private fun clearResponseArea() {
        responseArea.text = ""
    }

    private fun handleExplain(command: String) {
        // Parse command to extract potential file path argument
        val parts = command.trim().split("\\s+".toRegex(), 2)
        val hasFilePath = parts.size > 1 && parts[1].isNotEmpty()
        
        if (hasFilePath) {
            // If path is provided, pass directly to CoreAssistant
            processMessage(command)
            return
        }
        
        // Get the current project and editor
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project == null) {
            // No project open, pass through to CoreAssistant
            processMessage(command)
            return
        }
        
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            // No editor open, pass through to CoreAssistant
            processMessage(command)
            return
        }

        try {
            // Get selected text or entire document if no selection
            val selectionModel = editor.selectionModel
            val document = editor.document
            val file = FileEditorManager.getInstance(project).selectedEditor?.file
            val fileName = file?.name ?: "unknown file"
            
            val selectedText = if (selectionModel.hasSelection()) {
                selectionModel.selectedText
            } else {
                document.text
            }
            
            if (selectedText.isNullOrBlank()) {
                // No content available, pass through to CoreAssistant
                processMessage(command)
                return
            }
            
            val fileInfo = if (selectionModel.hasSelection()) 
                "selection from $fileName" 
            else 
                "entire file: $fileName"
            
            // Let user know what's being explained
            responseArea.append("\n\nExplaining $fileInfo...\n")
            
            // Format the message with the required prefix and separator
            val messageToSend = "Explain the following in an easy to understand way\n\n--------\n\n$selectedText"
            
            // Process through CoreAssistant
            processMessage(messageToSend)
        } catch (e: Exception) {
            // On any error, pass through to CoreAssistant
            processMessage(command)
        }
    }

    private fun processMessage(message: String) {
        sendButton.isEnabled = false
        progressBar.isIndeterminate = true

        val assistantMessage = CoreAssistantMessage(message)
        assistant.sendMessage(assistantMessage)
            .thenAccept { response ->
                javax.swing.SwingUtilities.invokeLater {
                    // Handle empty responses gracefully
                    if (response.content.trim().isNotEmpty()) {
                        responseArea.append("\n\nAssistant: ${response.content}")
                    }
                    responseArea.caretPosition = responseArea.document.length
                    sendButton.isEnabled = true
                    progressBar.isIndeterminate = false
                }
            }
            .exceptionally { e ->
                javax.swing.SwingUtilities.invokeLater {
                    var errorMessage = "Error: ${e.message}"
                    
                    // More user-friendly error message when LLM is not configured
                    if (errorMessage.contains("Unable to determine which LLM to use")) {
                        errorMessage = "No language model (LLM) is configured. Please configure an LLM using the /llm commands. Type /help llm for more information."
                    }
                    
                    // For /new command, don't show the error (same as NetBeans)
                    if (!message.trim().equals("/new")) {
                        responseArea.append("\n\nSystem: $errorMessage")
                    }
                    sendButton.isEnabled = true
                    progressBar.isIndeterminate = false
                }
                null
            }
    }
}
