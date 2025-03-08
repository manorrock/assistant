package com.manorrock.assistant.intellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
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

class IntelliJControllerTopComponent : ToolWindowFactory, ActionListener {

    private lateinit var responseArea: JTextArea
    private lateinit var requestArea: JTextArea
    private lateinit var sendButton: JButton
    private lateinit var startOverButton: JButton
    private lateinit var progressBar: JProgressBar
    private var sessionId: String = UUID.randomUUID().toString()
    private val history: LinkedList<JSONObject> = LinkedList()
    private var ollamaEndpoint: String = "http://localhost:11434/api/chat"
    private var model: String = "llama3"
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        responseArea = JTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
        }
        requestArea = JTextArea()
        sendButton = JButton("Send")
        startOverButton = JButton("Start Over")
        progressBar = JProgressBar(0, 100)

        // Set initial message
        responseArea.text = "Welcome to Manorrock Assistant"

        // Show help message on startup
        showHelp()

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
        startOverButton.addActionListener(this)

        // Layout setup (simplified)
        panel.add(JScrollPane(responseArea), BorderLayout.CENTER)
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.add(JScrollPane(requestArea), BorderLayout.CENTER)
        val buttonPanel = JPanel()
        buttonPanel.add(sendButton)
        buttonPanel.add(startOverButton)
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
            startOverButton -> handleStartOverAction()
        }
    }

    private fun handleSendAction() {
        val userMessage = requestArea.text.trim()

        if (userMessage.isNotEmpty()) {
            // Check if the message is a command
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage)
                return
            }

            // Get current timestamp
            val timestamp = LocalDateTime.now().format(formatter)

            // Display the user's message in the response area
            responseArea.append("\n\nYou: $userMessage")

            // Clear the request area
            requestArea.text = ""

            // Process the message and display a response
            processMessage(userMessage)
        }
    }

    private fun handleCommand(command: String) {
        when {
            command.startsWith("/endpoint ") -> changeEndpoint(command)
            command.startsWith("/model ") -> changeModel(command)
            command == "/help" -> showHelp()
            command == "/clear" -> clearResponseArea()
            else -> responseArea.append("\n\nSystem: Unknown command. Type /help for a list of commands.")
        }
        requestArea.text = ""
    }

    private fun changeEndpoint(command: String) {
        val pattern = java.util.regex.Pattern.compile("/endpoint\\s+(\\S+)")
        val matcher = pattern.matcher(command)
        if (matcher.find()) {
            val newEndpoint = matcher.group(1)
            ollamaEndpoint = "http://$newEndpoint/api/chat"
            responseArea.append("\n\nSystem: Endpoint changed to $ollamaEndpoint")
        } else {
            responseArea.append("\n\nSystem: Invalid endpoint format. Use /endpoint myhostname:myport")
        }
    }

    private fun changeModel(command: String) {
        val pattern = java.util.regex.Pattern.compile("/model\\s+(\\S+)")
        val matcher = pattern.matcher(command)
        if (matcher.find()) {
            model = matcher.group(1)
            responseArea.append("\n\nSystem: Model changed to $model")
        } else {
            responseArea.append("\n\nSystem: Invalid model format. Use /model <name>")
        }
    }

    private fun showHelp() {
        val helpMessage = """
            |System: Available commands:
            |/endpoint myhostname:myport - Change the Ollama endpoint
            |/model <name> - Change the model used
            |/help - Show this help message
            |/clear - Clear the response window
        """.trimMargin()
        responseArea.append(helpMessage)
    }

    private fun clearResponseArea() {
        responseArea.text = ""
    }

    private fun handleStartOverAction() {
        // Clear the history and reset the session ID
        history.clear()
        sessionId = UUID.randomUUID().toString()

        // Clear the response area
        responseArea.text = ""

        // Set initial messages
        responseArea.text = "Welcome to Manorrock Assistant"

        // Show help message
        showHelp()
    }

    private fun processMessage(message: String) {
        val timestamp = LocalDateTime.now().format(formatter)

        try {
            val messageObject = JSONObject().apply {
                put("role", "user")
                put("content", message)
            }

            // Add the new message to the history
            history.add(messageObject)
            if (history.size > 50) {
                history.removeFirst()
            }

            val jsonInput = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray(history))
                put("stream", true)
                put("session_id", sessionId)
            }

            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput.toString()))
                .build()

            sendButton.isEnabled = false
            progressBar.isIndeterminate = true

            client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenApply { it.body() }
                .thenAccept { lines ->
                    val responseBuilder = StringBuilder()
                    val isFirstLine = booleanArrayOf(true)
                    lines.forEach { line: String ->
                        val jsonObject = JSONObject(line)
                        if (jsonObject.has("session_id")) {
                            sessionId = jsonObject.getString("session_id")
                        }
                        if (jsonObject.has("messages")) {
                            val messages = jsonObject.getJSONArray("messages")
                            for (i in 0 until messages.length()) {
                                val msg = messages.getJSONObject(i)
                                if ("assistant" == msg.getString("role")) {
                                    val content = msg.getString("content")
                                    responseBuilder.append(content.toString())
                                    javax.swing.SwingUtilities.invokeLater {
                                        if (isFirstLine[0]) {
                                            responseArea.append("\n\nAssistant: " + content.toString())
                                            isFirstLine[0] = false
                                        } else {
                                            responseArea.append(content.toString())
                                        }
                                        responseArea.caretPosition = responseArea.document.length
                                    }
                                }
                            }
                        } else {
                            val content = jsonObject.getJSONObject("message").getString("content")
                            responseBuilder.append(content.toString())
                            javax.swing.SwingUtilities.invokeLater {
                                if (isFirstLine[0]) {
                                    responseArea.append("\n\nAssistant: " + content.toString())
                                    isFirstLine[0] = false
                                } else {
                                    responseArea.append(content.toString())
                                }
                                responseArea.caretPosition = responseArea.document.length
                            }
                        }
                    }

                    val response = responseBuilder.toString().trim()
                    javax.swing.SwingUtilities.invokeLater {
                        sendButton.isEnabled = true
                        progressBar.isIndeterminate = false

                        // Add the assistant's response to the history
                        val responseObject = JSONObject().apply {
                            put("role", "assistant")
                            put("content", response)
                        }
                        history.add(responseObject)
                        if (history.size > 50) {
                            history.removeFirst()
                        }
                    }
                }
                .exceptionally { e ->
                    javax.swing.SwingUtilities.invokeLater {
                        val errorMessage = "Ollama is unavailable."
                        responseArea.append("\n\nAssistant: $errorMessage")
                        responseArea.caretPosition = responseArea.document.length
                        sendButton.isEnabled = true
                        progressBar.isIndeterminate = false
                    }
                    null
                }
        } catch (e: Exception) {
            val errorMessage = "Ollama is unavailable."
            responseArea.append("\n\nAssistant: $errorMessage")
            responseArea.caretPosition = responseArea.document.length
            sendButton.isEnabled = true
            progressBar.isIndeterminate = false
        }
    }
}
