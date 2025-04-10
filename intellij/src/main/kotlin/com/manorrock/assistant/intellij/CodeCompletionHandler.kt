package com.manorrock.assistant.intellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.manorrock.assistant.core.CoreAssistant
import com.manorrock.assistant.core.CoreAssistantMessage
import javax.swing.Icon
import java.util.concurrent.CompletableFuture

/**
 * Handles code completion suggestions based on the selection context.
 */
class CodeCompletionHandler : AnAction() {
    private val assistant = CoreAssistant()
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // Get the selection context
        val selectionContext = getSelectionContext(editor)
        if (selectionContext.isNullOrEmpty()) return
        
        // Show a loading indicator
        val message = "Generating code suggestions..."
        val loadingPopup = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, null, null)
            .createBalloon()
        loadingPopup.show(
            com.intellij.ui.awt.RelativePoint(editor.contentComponent, java.awt.Point(0, 0)),
            com.intellij.openapi.ui.popup.Balloon.Position.above)
        
        // Generate the completion suggestions asynchronously
        generateCompletionSuggestions(selectionContext)
            .thenAccept { suggestions ->
                // Dismiss the loading popup
                loadingPopup.dispose()
                
                if (suggestions.isEmpty()) {
                    showNoSuggestionsPopup(editor)
                    return@thenAccept
                }
                
                // Show the suggestions in a popup
                val step = object : BaseListPopupStep<String>("Code Suggestions", suggestions) {
                    override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                        if (finalChoice) {
                            // Insert the selected code at the cursor position
                            insertCodeAtCursor(editor, selectedValue)
                        }
                        return super.onChosen(selectedValue, finalChoice)
                    }
                }
                
                val popup = JBPopupFactory.getInstance().createListPopup(step)
                popup.showInBestPositionFor(editor)
            }
            .exceptionally { e ->
                loadingPopup.dispose()
                showErrorPopup(editor, "Failed to generate suggestions: ${e.message}")
                null
            }
    }

    /**
     * Returns the relevant context from the selected text or surrounding code.
     */
    private fun getSelectionContext(editor: Editor): String? {
        val selectionModel = editor.selectionModel
        val document = editor.document
        
        // If text is selected, use it as context
        if (selectionModel.hasSelection()) {
            return selectionModel.selectedText
        }
        
        // Otherwise, get the current line and surrounding code
        val caretOffset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(caretOffset)
        
        // Get a few lines before and after for context
        val contextStartLine = maxOf(0, lineNumber - 5)
        val contextEndLine = minOf(document.lineCount - 1, lineNumber + 5)
        
        val startOffset = document.getLineStartOffset(contextStartLine)
        val endOffset = document.getLineEndOffset(contextEndLine)
        
        return document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
    }

    /**
     * Generates code completion suggestions based on the provided context.
     */
    private fun generateCompletionSuggestions(context: String): CompletableFuture<List<String>> {
        val promptMessage = """I need code completion suggestions based on the following context. 
            |Please provide 3-5 relevant code snippets that could logically follow this code:
            |
            |```
            |$context
            |```
            |
            |Format your response as a list of code snippets, each prefixed with 'SUGGESTION:' on a new line.
            |Don't include explanations, just the code snippets.
        """.trimMargin()
        
        val message = CoreAssistantMessage(promptMessage)
        
        return assistant.sendMessage(message)
            .thenApply { response ->
                // Parse the response to extract the suggestions
                parseCompletionSuggestions(response.content)
            }
    }
    
    /**
     * Parses the completion suggestions from the assistant's response.
     */
    private fun parseCompletionSuggestions(response: String): List<String> {
        return response.lines()
            .filter { it.trim().startsWith("SUGGESTION:") }
            .map { it.substringAfter("SUGGESTION:").trim() }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
            ?: run {
                // If no properly formatted suggestions were found, try to extract code blocks
                val codeBlockRegex = "```(?:kotlin|java)?\\s*([\\s\\S]*?)```".toRegex()
                val matches = codeBlockRegex.findAll(response)
                matches.map { it.groupValues[1].trim() }.toList()
            }
    }
    
    /**
     * Inserts the selected code at the current cursor position.
     */
    private fun insertCodeAtCursor(editor: Editor, code: String) {
        val document = editor.document
        val caretModel = editor.caretModel
        
        // Create a runnable for the write action
        val runnable = Runnable {
            document.insertString(caretModel.offset, code)
        }
        
        // Execute the write action
        com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction(runnable)
    }
    
    /**
     * Shows a popup indicating that no suggestions could be generated.
     */
    private fun showNoSuggestionsPopup(editor: Editor) {
        val message = "No code suggestions could be generated."
        val popup = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, null, null)
            .createBalloon()
        popup.show(
            com.intellij.ui.awt.RelativePoint(editor.contentComponent, java.awt.Point(0, 0)),
            com.intellij.openapi.ui.popup.Balloon.Position.above)
    }
    
    /**
     * Shows an error popup.
     */
    private fun showErrorPopup(editor: Editor, message: String) {
        val popup = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, null, null)
            .createBalloon()
        popup.show(
            com.intellij.ui.awt.RelativePoint(editor.contentComponent, java.awt.Point(0, 0)),
            com.intellij.openapi.ui.popup.Balloon.Position.above)
    }
}
