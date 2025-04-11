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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for the "Explain Error" context menu item in the Problems view.
 */
public class ExplainProblemHandler extends AbstractHandler {

    private static final int CONTEXT_LINES = 5; // Number of lines to include before/after the error

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Get the current selection in the Problems view
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            StringBuilder errorDetails = new StringBuilder();
            
            // Print debugging information about the selection
            System.out.println("Selection has " + structuredSelection.size() + " elements");
            
            // For each selected problem
            for (Object element : structuredSelection.toArray()) {
                System.out.println("Selected element class: " + (element != null ? element.getClass().getName() : "null"));
                
                // Try to adapt the element to an IMarker if it's not already one
                IMarker marker = null;
                if (element instanceof IMarker) {
                    marker = (IMarker) element;
                } else if (element != null) {
                    // Try to adapt the object to an IMarker
                    marker = (IMarker) org.eclipse.core.runtime.Platform.getAdapterManager()
                            .getAdapter(element, IMarker.class);
                    
                    if (marker == null) {
                        // Try using the object's own adapter method if available
                        if (element instanceof org.eclipse.core.runtime.IAdaptable) {
                            marker = (IMarker) ((org.eclipse.core.runtime.IAdaptable) element)
                                    .getAdapter(IMarker.class);
                        }
                    }
                }
                
                if (marker != null) {
                    try {
                        // Get problem details
                        String message = (String) marker.getAttribute(IMarker.MESSAGE, "Unknown error");
                        Integer lineNumber = (Integer) marker.getAttribute(IMarker.LINE_NUMBER, 0);
                        Integer severity = (Integer) marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                        String severityStr = getSeverityString(severity);
                        
                        // Get the file resource
                        IFile file = (IFile) marker.getResource();
                        String fileName = file.getName();
                        
                        // Add error details to our StringBuilder
                        errorDetails.append("File: ").append(fileName).append("\n");
                        errorDetails.append("Line: ").append(lineNumber).append("\n");
                        errorDetails.append("Severity: ").append(severityStr).append("\n");
                        errorDetails.append("Message: ").append(message).append("\n\n");
                        
                        try {
                            // Get source code context
                            String sourceContext = getSourceContext(file, lineNumber);
                            if (sourceContext != null && !sourceContext.isEmpty()) {
                                errorDetails.append("Source context:\n```java\n");
                                errorDetails.append(sourceContext);
                                errorDetails.append("\n```\n\n");
                            }
                            
                            // Get imports if it's a Java file
                            if (fileName.endsWith(".java")) {
                                String imports = getImports(file);
                                if (imports != null && !imports.isEmpty()) {
                                    errorDetails.append("Import statements:\n```java\n");
                                    errorDetails.append(imports);
                                    errorDetails.append("\n```\n\n");
                                }
                            }
                        } catch (Exception e) {
                            errorDetails.append("Error retrieving additional information: ")
                                      .append(e.getMessage())
                                      .append("\n");
                        }
                        
                        errorDetails.append("-------------------\n\n");
                    } catch (Exception e) {
                        errorDetails.append("Error accessing marker information: ")
                                    .append(e.getMessage())
                                    .append("\n");
                    }
                }
            }
            
            // If we have error details, send them to the Assistant View
            if (errorDetails.length() > 0) {
                sendToAssistantView(errorDetails.toString());
            }
        }
        
        return null;
    }
    
    /**
     * Get a string representation of the marker severity.
     * 
     * @param severity The severity integer from the marker
     * @return A human-readable severity string
     */
    private String getSeverityString(int severity) {
        switch (severity) {
            case IMarker.SEVERITY_ERROR:
                return "Error";
            case IMarker.SEVERITY_WARNING:
                return "Warning";
            case IMarker.SEVERITY_INFO:
                return "Info";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Gets the source code context around the specified line number.
     * 
     * @param file The file containing the code
     * @param lineNumber The line number where the error occurred
     * @return A string containing the source context
     */
    private String getSourceContext(IFile file, int lineNumber) {
        try {
            // Create a document from the file contents
            IDocument document = new Document(new String(file.getContents().readAllBytes()));
            
            // Calculate the start and end lines, ensuring they're within bounds
            int totalLines = document.getNumberOfLines();
            int startLine = Math.max(0, lineNumber - CONTEXT_LINES - 1);
            int endLine = Math.min(totalLines - 1, lineNumber + CONTEXT_LINES - 1);
            
            StringBuilder sourceContext = new StringBuilder();
            
            // Get the text for each line in the context range
            for (int i = startLine; i <= endLine; i++) {
                int lineOffset = document.getLineOffset(i);
                int lineLength = document.getLineLength(i);
                String lineText = document.get(lineOffset, lineLength);
                
                // Prefix the error line with a marker
                if (i == lineNumber - 1) {
                    sourceContext.append(">> ").append(lineText);
                } else {
                    sourceContext.append("   ").append(lineText);
                }
                
                // Don't add a newline if the line already ends with one
                if (!lineText.endsWith("\n")) {
                    sourceContext.append("\n");
                }
            }
            
            return sourceContext.toString();
        } catch (Exception e) {
            return "Error retrieving source context: " + e.getMessage();
        }
    }
    
    /**
     * Gets the import statements from a Java file.
     * 
     * @param file The Java file
     * @return A string containing the import statements
     */
    private String getImports(IFile file) {
        try {
            if (file.getFileExtension() != null && file.getFileExtension().equals("java")) {
                IJavaElement javaElement = JavaCore.create(file);
                if (javaElement instanceof ICompilationUnit) {
                    ICompilationUnit unit = (ICompilationUnit) javaElement;
                    
                    // Get the source of the compilation unit
                    String source = unit.getSource();
                    
                    // Extract import statements using a simple regex
                    StringBuilder imports = new StringBuilder();
                    
                    for (String line : source.split("\\n")) {
                        line = line.trim();
                        if (line.startsWith("import ")) {
                            imports.append(line).append("\n");
                        }
                        // Stop once we reach the class declaration
                        if (line.contains("class ") || line.contains("interface ") || line.contains("enum ")) {
                            break;
                        }
                    }
                    
                    return imports.toString();
                }
            }
        } catch (Exception e) {
            return "Error retrieving imports: " + e.getMessage();
        }
        return "";
    }
    
    /**
     * Sends the error details to the Assistant View for explanation.
     * 
     * @param errorDetails The formatted error details to explain
     */
    private void sendToAssistantView(String errorDetails) {
        try {
            // Find the Assistant View
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            
            // Debug message about the view ID we're looking for
            System.out.println("Looking for view with ID: " + AssistantView.ID);
            
            IViewPart viewPart = page.findView(AssistantView.ID);
            
            // Debug if view was found
            if (viewPart == null) {
                System.out.println("ERROR: AssistantView not found! Attempting to open it...");
                // Try to open the view first
                viewPart = page.showView(AssistantView.ID);
                if (viewPart == null) {
                    System.out.println("ERROR: Failed to open AssistantView!");
                    org.eclipse.jface.dialogs.MessageDialog.openError(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        "Assistant Error",
                        "Could not find or open the Assistant View.");
                    return;
                }
            } else {
                System.out.println("SUCCESS: Found AssistantView!");
            }
            
            if (viewPart instanceof AssistantView) {
                System.out.println("View is an instance of AssistantView, sending error details...");
                AssistantView assistantView = (AssistantView) viewPart;
                
                // Construct the prompt for the LLM
                String prompt = "Please explain and suggest fixes for the following compiler/build error:\n\n" 
                              + errorDetails;
                
                // Send the prompt to the Assistant View
                assistantView.explainError(prompt);
                
                // Show the Assistant View if it's not visible
                page.showView(AssistantView.ID);
            } else {
                System.out.println("ERROR: View is not an instance of AssistantView, it is: " + viewPart.getClass().getName());
                org.eclipse.jface.dialogs.MessageDialog.openError(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                    "Assistant Error",
                    "Found a view with the correct ID, but it's not an AssistantView instance.");
            }
        } catch (Exception e) {
            System.err.println("Error sending to Assistant View: " + e.getMessage());
            e.printStackTrace();
            
            // Try to show the error in a dialog for better visibility during debugging
            try {
                org.eclipse.jface.dialogs.MessageDialog.openError(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                    "Assistant Error",
                    "Error sending to Assistant View: " + e.getMessage());
            } catch (Exception dialogError) {
                // Fallback if we can't show dialog
                System.err.println("Failed to show error dialog: " + dialogError.getMessage());
            }
        }
    }
}
