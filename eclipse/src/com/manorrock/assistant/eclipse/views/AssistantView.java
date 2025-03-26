package com.manorrock.assistant.eclipse.views;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
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

import com.manorrock.assistant.impl.AssistantImpl;

public class AssistantView extends ViewPart implements ISelectionListener {
    public static final String ID = "com.manorrock.assistant.eclipse.views.AssistantView";

    private StyledText responseArea;
    private StyledText requestArea;
    private Button sendButton;
    private ProgressBar progressBar;
    private IEditorPart lastActiveEditor;
    
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private MessageConsole console;
    private MessageConsoleStream consoleStream;
    private AssistantImpl assistant;
    
    @Override
    public void createPartControl(Composite parent) {
        // Create console for logging
        console = findConsole("Manorrock Assistant Log");
        consoleStream = console.newMessageStream();
        
        // Initialize AssistantImpl
        assistant = new AssistantImpl();
        
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
        
        // Set initial message based on CLI availability
        if (!assistant.isCliAvailable()) {
            responseArea.setText("Manorrock Assistant CLI not found. Please visit " +
                "https://github.com/manorrock/assistant?tab=readme-ov-file#quick-install " +
                "for installation instructions.");
            sendButton.setEnabled(false);
        } else {
            responseArea.setText("Welcome to Manorrock Assistant\n\nType /help for a list of commands.");
        }
        
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
            
            // Process message through CLI
            processMessage(userMessage);
        }
    }
    
    private void handleNew() {
        responseArea.setText("");
        responseArea.append("Started a new chat session.\n");
        processMessage("/new");
    }
    
    private void handleExplain() {
        if (lastActiveEditor != null && lastActiveEditor instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) lastActiveEditor;
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
            if (lastActiveEditor != null && lastActiveEditor instanceof ITextEditor) {
                ITextEditor textEditor = (ITextEditor) lastActiveEditor;
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

        assistant.executeCommand(message)
            .thenAccept(response -> {
                Display.getDefault().asyncExec(() -> {
                    responseArea.append("\n\nAssistant: " + response);
                    consoleStream.println("[" + timestamp + " - Assistant]\n" + response);
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
            lastActiveEditor = (IEditorPart) part;
        }
    }
}