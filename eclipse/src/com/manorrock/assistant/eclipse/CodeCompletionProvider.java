/*
 * Copyright (c) 2002-2025, Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright 
 *        notice, this list of conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.manorrock.assistant.eclipse;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.core.CoreAssistantMessage;

/**
 * This class provides code completion suggestions using the LLM-based assistant.
 */
public class CodeCompletionProvider {
    
    private CoreAssistant assistant;
    
    public CodeCompletionProvider(CoreAssistant assistant) {
        this.assistant = assistant;
    }
    
    /**
     * Generates code completion suggestions based on the current context and selection.
     * 
     * @param editor The active text editor
     * @param selection The current text selection
     * @return A CompletableFuture containing a list of ICompletionProposal objects
     */
    public CompletableFuture<List<ICompletionProposal>> generateCompletionSuggestions(ITextEditor editor, ITextSelection selection) {
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        String fileName = editor.getEditorInput().getName();
        
        try {
            // Get the context surrounding the cursor/selection position
            int contextStartOffset = Math.max(0, selection.getOffset() - 500);
            int contextLength = Math.min(1000, document.getLength() - contextStartOffset);
            String contextText = document.get(contextStartOffset, contextLength);
            
            // Build a prompt for the assistant
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Please provide code completion suggestions for the following code in ")
                .append(fileName)
                .append(".\n\n")
                .append("Context:\n```\n")
                .append(contextText)
                .append("\n```\n\n")
                .append("The cursor is at the position marked by |> in the context.")
                .append("\n\nProvide 1-2 possible completions as JSON in the format:")
                .append("\n[\n  {\"text\": \"completion text\", \"description\": \"short description\"},\n  ...\n]");
            
            // Insert cursor marker in the context
            int relativeSelectionOffset = selection.getOffset() - contextStartOffset;
            if (relativeSelectionOffset >= 0 && relativeSelectionOffset <= contextText.length()) {
                promptBuilder.replace(
                    promptBuilder.indexOf("```\n") + 4 + relativeSelectionOffset,
                    promptBuilder.indexOf("```\n") + 4 + relativeSelectionOffset,
                    " |> "
                );
            }
            
            String prompt = promptBuilder.toString();
            
            // Send the request to the assistant
            AssistantMessage message = new CoreAssistantMessage(prompt);
            
            return assistant.sendMessage(message)
                .thenApply(response -> {
                    List<ICompletionProposal> proposals = new ArrayList<>();
                    
                    // Parse the JSON response
                    String content = response.getContent();
                    
                    try {
                        // Extract JSON array from the response
                        String jsonContent = extractJsonArrayFromResponse(content);
                        
                        // Parse the JSON array using Jackson (already available through LangChain4J)
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(jsonContent);
                        
                        // Create completion proposals from the JSON array
                        if (rootNode.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode suggestionNode : rootNode) {
                                if (suggestionNode.has("text")) {
                                    String completionText = suggestionNode.get("text").asText();
                                    String description = suggestionNode.has("description") ? 
                                        suggestionNode.get("description").asText() : "";
                                    
                                    proposals.add(new CompletionProposal(
                                        completionText, 
                                        selection.getOffset(), 
                                        selection.getLength(), 
                                        completionText.length(),
                                        null,
                                        description,
                                        null,
                                        description
                                    ));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Log the error but don't provide fallback suggestions
                        System.err.println("Error parsing LLM response: " + e.getMessage());
                        e.printStackTrace();
                        
                        // Return an empty list if we can't parse the response
                        // This is better than providing meaningless fallbacks
                    }
                    
                    return proposals;
                });
        } catch (BadLocationException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }
    
    /**
     * Applies a selected completion proposal to the document.
     * 
     * @param editor The active text editor
     * @param proposal The completion proposal to apply
     */
    public void applyCompletion(ITextEditor editor, ICompletionProposal proposal) {
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        proposal.apply(document);
    }
    
    /**
     * Extracts the JSON array from the LLM response text.
     * 
     * @param responseText The raw response text from the LLM
     * @return A string containing only the JSON array part of the response
     */
    private String extractJsonArrayFromResponse(String responseText) {
        // Look for a JSON array pattern in the response
        int startIndex = responseText.indexOf('[');
        int endIndex = responseText.lastIndexOf(']');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return responseText.substring(startIndex, endIndex + 1);
        }
        
        // If no JSON array is found, attempt to extract from a code block
        startIndex = responseText.indexOf("```json");
        if (startIndex != -1) {
            startIndex = responseText.indexOf('[', startIndex);
            endIndex = responseText.indexOf(']', startIndex);
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                return responseText.substring(startIndex, endIndex + 1);
            }
        }
        
        // If still no valid JSON array found, look for any code block
        startIndex = responseText.indexOf("```");
        if (startIndex != -1) {
            int codeBlockEnd = responseText.indexOf("```", startIndex + 3);
            if (codeBlockEnd != -1) {
                String codeBlock = responseText.substring(startIndex + 3, codeBlockEnd).trim();
                startIndex = codeBlock.indexOf('[');
                endIndex = codeBlock.lastIndexOf(']');
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    return codeBlock.substring(startIndex, endIndex + 1);
                }
            }
        }
        
        // If we still couldn't find a valid JSON array, throw an exception
        throw new IllegalArgumentException("Could not extract a valid JSON array from the response");
    }
}
