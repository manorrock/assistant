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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.core.CoreAssistantMessage;

public class AssistantView extends ViewPart implements ISelectionListener {
    public static final String ID = "com.manorrock.assistant.eclipse.AssistantView";

    private StyledText responseArea;
    private StyledText requestArea;
    private Button sendButton;
    private ProgressBar progressBar;
    
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private MessageConsole console;
    private MessageConsoleStream consoleStream;
    private static CoreAssistant assistantInstance;
    private static IEditorPart lastActiveEditorInstance;
    private CodeCompletionProvider completionProvider;
    
    @Override
    public void createPartControl(Composite parent) {
        // Create console for logging
        console = findConsole("Manorrock Assistant Log");
        consoleStream = console.newMessageStream();
        
        // Initialize CoreAssistant directly
        assistantInstance = new CoreAssistant();
        // Initialize CodeCompletionProvider
        completionProvider = new CodeCompletionProvider(assistantInstance);
        
        // Set up UI layout
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        parent.setLayout(layout);
        
        // Response area
        responseArea = new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP);
        responseArea.setEditable(false);
        GridData responseData = new GridData(SWT.FILL, SWT.FILL, true, true);
        responseData.heightHint = 300;
        responseArea.setLayoutData(responseData);
        
        // Request area
        requestArea = new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData requestData = new GridData(SWT.FILL, SWT.FILL, true, false);
        requestData.heightHint = 100;
        requestArea.setLayoutData(requestData);
        
        // Button panel
        Composite buttonPanel = new Composite(parent, SWT.NONE);
        buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout buttonLayout = new GridLayout(1, false);
        buttonPanel.setLayout(buttonLayout);
        
        sendButton = new Button(buttonPanel, SWT.PUSH);
        sendButton.setText("Send");
        sendButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        
        // Progress bar
        progressBar = new ProgressBar(parent, SWT.HORIZONTAL | SWT.INDETERMINATE);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        progressBar.setVisible(false);
        
        // Set initial welcome message
        responseArea.setText("Welcome to Manorrock Assistant\n\nType /help for a list of commands.");
        
        // Add listeners
        requestArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR && (e.stateMask & SWT.SHIFT) == 0) {
                    handleSendAction();
                    e.doit = false;  // Prevents the newline from being added
                }
            }
        });
        
        sendButton.addListener(SWT.Selection, event -> handleSendAction());
        
        // Add tool bar actions
        createActions();
        
        // Listen for editor selections
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
    }
    
    private void createActions() {
        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        
        Action codeCompletionAction = new Action("Code Completion") {
            @Override
            public void run() {
                handleCodeCompletion();
            }
        };
        codeCompletionAction.setToolTipText("Generate code completion suggestions for current selection");
        toolBarManager.add(codeCompletionAction);
        
        Action explainAction = new Action("Explain Selection") {
            @Override
            public void run() {
                handleExplain();
            }
        };
        explainAction.setToolTipText("Explain the selected code");
        toolBarManager.add(explainAction);
        
        Action helpAction = new Action("Help") {
            @Override
            public void run() {
                processMessage("/help");
            }
        };
        helpAction.setToolTipText("Show help");
        toolBarManager.add(helpAction);
    }
    
    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();
        
        if (!userMessage.isEmpty()) {
            String timestamp = LocalDateTime.now().format(formatter);
            
            // Display the user's message in the response area
            responseArea.append("\n\nYou: " + userMessage);
            
            // Add timestamped message to the Console
            consoleStream.println("[" + timestamp + " - You]\n" + userMessage);
            
            // Clear the request area
            requestArea.setText("");
            
            // Handle /new command specially to clear UI first
            if (userMessage.equals("/new")) {
                handleNew();
            }
            
            // Handle /explain command specially
            if (userMessage.startsWith("/explain")) {
                handleExplain();
                return;
            }
            
            // Process message using CoreAssistant
            processMessage(userMessage);
        }
    }
    
    private void handleNew() {
        responseArea.setText("");
        responseArea.append("Started a new chat session.\n");
        processMessage("/new");
    }
    
    private void handleExplain() {
        if (lastActiveEditorInstance != null && lastActiveEditorInstance instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) lastActiveEditorInstance;
            IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            ISelection selection = textEditor.getSelectionProvider().getSelection();
            
            try {
                String selectedText;
                if (selection instanceof ITextSelection) {
                    ITextSelection textSelection = (ITextSelection) selection;
                    selectedText = textSelection.getText();
                    if (selectedText == null || selectedText.isEmpty()) {
                        selectedText = document.get();
                    }
                } else {
                    selectedText = document.get();
                }
                
                String prompt = "Please explain the content below the line\n-----------------------------------------\n" + selectedText;
                responseArea.append("\n\nYou: " + prompt);
                processMessage(prompt);
            } catch (Exception e) {
                responseArea.append("\n\nSystem: Error retrieving text from the editor.");
                e.printStackTrace();
            }
        } else {
            responseArea.append("\n\nSystem: No active editor window found.");
        }
    }
    
    private void processMessage(String message) {
        String timestamp = LocalDateTime.now().format(formatter);

        if (message.startsWith("/explain")) {
            if (lastActiveEditorInstance != null && lastActiveEditorInstance instanceof ITextEditor) {
                ITextEditor textEditor = (ITextEditor) lastActiveEditorInstance;
                IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
                ISelection selection = textEditor.getSelectionProvider().getSelection();
                
                try {
                    String selectedText;
                    if (selection instanceof ITextSelection) {
                        ITextSelection textSelection = (ITextSelection) selection;
                        selectedText = textSelection.getText();
                        if (selectedText == null || selectedText.isEmpty()) {
                            selectedText = document.get();
                        }
                    } else {
                        selectedText = document.get();
                    }
                    
                    if (selectedText.trim().length() == 0) {
                        responseArea.append("\n\nSystem: No content to explain. Please select some text or ensure the file has content.");
                        return;
                    }
                    
                    String fileName = textEditor.getEditorInput().getName();
                    String fileInfo = selectedText.equals(document.get()) ?
                        "entire file: " + fileName :
                        "selection from " + fileName;
                    
                    message = "/explain\nExplaining " + fileInfo + 
                        "\n-----------------------------------------\n" + selectedText;
                    
                    responseArea.append("\n\nExplaining " + fileInfo + "...\n");
                } catch (Exception e) {
                    responseArea.append("\n\nSystem: Error retrieving text from the editor.");
                    return;
                }
            } else {
                responseArea.append("\n\nSystem: Please select a snippet or open a file to use the /explain command.");
                return;
            }
        }

        sendButton.setEnabled(false);
        progressBar.setVisible(true);

        // Create an AssistantMessage and send it through CoreAssistant
        AssistantMessage assistantMessage = new CoreAssistantMessage(message);
        assistantInstance.sendMessage(assistantMessage)
            .thenAccept(response -> {
                Display.getDefault().asyncExec(() -> {
                    responseArea.append("\n\nAssistant: " + response.getContent());
                    consoleStream.println("[" + timestamp + " - Assistant]\n" + response.getContent());
                    sendButton.setEnabled(true);
                    progressBar.setVisible(false);
                    responseArea.setTopIndex(responseArea.getLineCount() - 1);
                });
            })
            .exceptionally(e -> {
                Display.getDefault().asyncExec(() -> {
                    String errorMessage = "Error: " + e.getMessage();
                    responseArea.append("\n\nSystem: " + errorMessage);
                    consoleStream.println("[" + timestamp + " - Error]\n" + errorMessage);
                    sendButton.setEnabled(true);
                    progressBar.setVisible(false);
                });
                return null;
            });
    }
    
    /**
     * Public method to trigger code completion from external commands or keyboard shortcuts.
     * This method is called by the CodeCompletionHandler when the user presses the keyboard shortcut.
     */
    public void triggerCodeCompletion() {
        handleCodeCompletion();
    }
    
    /**
     * Handles code completion requests from the toolbar or command.
     * Generates and displays completion suggestions based on the current context and selection.
     */
    private void handleCodeCompletion() {
        if (lastActiveEditorInstance != null && lastActiveEditorInstance instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) lastActiveEditorInstance;
            ISelection selection = textEditor.getSelectionProvider().getSelection();
            
            if (selection instanceof ITextSelection) {
                ITextSelection textSelection = (ITextSelection) selection;
                String fileName = textEditor.getEditorInput().getName();
                
                // Show a message in the response area
                responseArea.append("\n\nGenerating code completion suggestions for " + fileName + "...");
                
                // Start the progress indicator
                sendButton.setEnabled(false);
                progressBar.setVisible(true);
                
                // Generate completion proposals using the CodeCompletionProvider
                completionProvider.generateCompletionSuggestions(textEditor, textSelection)
                    .thenAccept(proposals -> {
                        Display.getDefault().asyncExec(() -> {
                            if (proposals.isEmpty()) {
                                responseArea.append("\n\nNo completion suggestions available for the current context.");
                            } else {
                                // Display the completion suggestions in the response area
                                responseArea.append("\n\nCode completion suggestions:");
                                
                                for (int i = 0; i < proposals.size(); i++) {
                                    ICompletionProposal proposal = proposals.get(i);
                                    String displayString = proposal.getDisplayString();
                                    String additionalInfo = proposal.getAdditionalProposalInfo();
                                    
                                    responseArea.append("\n\n" + (i + 1) + ". " + displayString);
                                    if (additionalInfo != null && !additionalInfo.isEmpty()) {
                                        responseArea.append("\n   " + additionalInfo);
                                    }
                                }
                                
                                // Create a dialog to let the user select a completion
                                showCompletionSelectionDialog(textEditor, proposals);
                            }
                            
                            sendButton.setEnabled(true);
                            progressBar.setVisible(false);
                            responseArea.setTopIndex(responseArea.getLineCount() - 1);
                        });
                    })
                    .exceptionally(e -> {
                        Display.getDefault().asyncExec(() -> {
                            String errorMessage = "Error generating completion suggestions: " + e.getMessage();
                            responseArea.append("\n\nSystem: " + errorMessage);
                            e.printStackTrace();
                            
                            sendButton.setEnabled(true);
                            progressBar.setVisible(false);
                        });
                        return null;
                    });
            } else {
                responseArea.append("\n\nSystem: Please place the cursor at a position where you want code completion.");
            }
        } else {
            responseArea.append("\n\nSystem: No active editor window found. Please open a file to use code completion.");
        }
    }
    
    /**
     * Shows a dialog allowing the user to select from available completion proposals.
     * 
     * @param editor The text editor where the completion will be applied
     * @param proposals The list of completion proposals to choose from
     */
    private void showCompletionSelectionDialog(ITextEditor editor, List<ICompletionProposal> proposals) {
        // Simple implementation using direct application
        // For a real implementation, you would create a proper selection dialog
        // For now, we'll just apply the first suggestion as a demonstration
        if (!proposals.isEmpty()) {
            ICompletionProposal selectedProposal = proposals.get(0);
            completionProvider.applyCompletion(editor, selectedProposal);
            
            // Log the applied completion
            String message = "Applied completion: " + selectedProposal.getDisplayString();
            responseArea.append("\n\nSystem: " + message);
            consoleStream.println("[" + LocalDateTime.now().format(formatter) + " - System]\n" + message);
        }
    }
    
    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (IConsole console : existing) {
            if (name.equals(console.getName())) {
                return (MessageConsole) console;
            }
        }
        // No console found, create a new one
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }
    
    @Override
    public void setFocus() {
        requestArea.setFocus();
    }
    
    @Override
    public void dispose() {
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        super.dispose();
    }
    
    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (part instanceof IEditorPart) {
            lastActiveEditorInstance = (IEditorPart) part;
        }
    }

    // Static getter for the CoreAssistant instance
    public static CoreAssistant getAssistant() {
        return assistantInstance;
    }
    
    // Static getter for the last active editor
    public static ITextEditor getLastActiveEditor() {
        if (lastActiveEditorInstance instanceof ITextEditor) {
            return (ITextEditor) lastActiveEditorInstance;
        }
        return null;
    }
}