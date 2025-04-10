package com.manorrock.assistant.intellij

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.manorrock.assistant.core.CoreAssistant
import com.manorrock.assistant.core.CoreAssistantMessage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Contributes AI-powered code completion suggestions to IntelliJ's native completion system.
 */
class AICompletionContributor : CompletionContributor() {
    private val assistant = CoreAssistant()
    
    init {
        // Register a provider for all locations
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), AICompletionProvider())
    }
    
    /**
     * The completion provider that generates AI suggestions.
     */
    inner class AICompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            // Get the editor and document
            val editor = parameters.editor
            val document = editor.document
            val file = parameters.originalFile
            
            // Don't provide suggestions for very large files
            if (document.textLength > 50000) return
            
            // Get the context around the cursor position
            val offset = parameters.offset
            val lineNumber = document.getLineNumber(offset)
            
            // Get a few lines before and after for context
            val contextStartLine = maxOf(0, lineNumber - 5)
            val contextEndLine = minOf(document.lineCount - 1, lineNumber + 5)
            
            val startOffset = document.getLineStartOffset(contextStartLine)
            val endOffset = document.getLineEndOffset(contextEndLine)
            
            val context = document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
            if (context.isEmpty()) return
            
            // Show a processing indicator in the completion window
            result.addElement(LookupElementBuilder.create("Generating AI suggestions...").withItemTextForeground(java.awt.Color.GRAY))
            
            // Generate suggestions asynchronously
            val future = generateCompletionSuggestions(context, file.language.displayName)
            
            try {
                // Wait for a reasonable time for suggestions
                val suggestions = future.get(8, TimeUnit.SECONDS)
                
                // Replace the processing indicator with actual suggestions
                suggestions.forEachIndexed { index, suggestion ->
                    val priority = suggestions.size - index
                    val lookupElement = LookupElementBuilder.create(suggestion)
                        .bold()
                        .withTypeText("AI", true)
                        .withIcon(com.intellij.icons.AllIcons.Actions.Lightning)
                    
                    result.addElement(PrioritizedLookupElement.withPriority(lookupElement, priority.toDouble()))
                }
            } catch (e: Exception) {
                // If there's a timeout or error, show a message
                result.addElement(LookupElementBuilder.create("AI suggestions not available")
                    .withItemTextForeground(java.awt.Color.RED))
            }
        }
    }
    
    /**
     * Generates code completion suggestions based on the provided context.
     */
    private fun generateCompletionSuggestions(context: String, language: String): CompletableFuture<List<String>> {
        val promptMessage = """I need code completion suggestions based on the following $language code context. 
            |Please provide 3-5 relevant code snippets that could logically follow at the cursor position:
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
                val codeBlockRegex = "```(?:\\w+)?\\s*([\\s\\S]*?)```".toRegex()
                val matches = codeBlockRegex.findAll(response)
                matches.map { it.groupValues[1].trim() }.toList()
            }
    }
}
