package com.manorrock.assistant.eclipse.views;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.BadLocationException;
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class AssistantView extends ViewPart implements ISelectionListener {
    public static final String ID = "com.manorrock.assistant.eclipse.views.AssistantView";

    private StyledText responseArea;
    private StyledText requestArea;
    private Button sendButton;
    private Button startOverButton;
    private ProgressBar progressBar;
    private IEditorPart lastActiveEditor;
    
    private String sessionId = UUID.randomUUID().toString();
    private LinkedList<JSONObject> history = new LinkedList<>();
    private String ollamaEndpoint = "http://localhost:11434/api/chat";
    private String model = "llama3";
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private MessageConsole console;
    private MessageConsoleStream consoleStream;
    
    @Override
    public void createPartControl(Composite parent) {
        // Create console for logging
        console = findConsole("Manorrock Assistant Log");
        consoleStream = console.newMessageStream();
        
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
        GridLayout buttonLayout = new GridLayout(2, false);
        buttonPanel.setLayout(buttonLayout);
        
        sendButton = new Button(buttonPanel, SWT.PUSH);
        sendButton.setText("Send");
        sendButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        
        startOverButton = new Button(buttonPanel, SWT.PUSH);
        startOverButton.setText("Start Over");
        startOverButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        // Progress bar
        progressBar = new ProgressBar(parent, SWT.HORIZONTAL | SWT.INDETERMINATE);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        progressBar.setVisible(false);
        
        // Set initial message
        responseArea.setText("Welcome to Manorrock Assistant");
        
        // Show help message on startup
        showHelp();
        
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
        startOverButton.addListener(SWT.Selection, event -> handleStartOverAction());
        
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
                explainSelection();
            }
        };
        explainAction.setToolTipText("Explain the selected code");
        toolBarManager.add(explainAction);
        
        Action clearAction = new Action("Clear") {
            @Override
            public void run() {
                clearResponseArea();
            }
        };
        clearAction.setToolTipText("Clear the response area");
        toolBarManager.add(clearAction);
        
        Action helpAction = new Action("Help") {
            @Override
            public void run() {
                showHelp();
            }
        };
        helpAction.setToolTipText("Show help");
        toolBarManager.add(helpAction);
    }
    
    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();
        
        if (!userMessage.isEmpty()) {
            // Check if the message is a command
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage);
                return;
            }
            
            // Get current timestamp
            String timestamp = LocalDateTime.now().format(formatter);
            
            // Display the user's message in the response area
            responseArea.append("\n\nYou: " + userMessage);
            
            // Add timestamped message to the Console
            consoleStream.println("[" + timestamp + " - You]\n" + userMessage);
            
            // Clear the request area
            requestArea.setText("");
            
            // Process the message and display a response
            processMessage(userMessage);
        }
    }
    
    private void handleCommand(String command) {
        if (command.startsWith("/llmEndpoint ")) {
            changeEndpoint(command);
        } else if (command.startsWith("/model ")) {
            changeModel(command);
        } else if (command.equals("/help")) {
            showHelp();
        } else if (command.equals("/clear")) {
            clearResponseArea();
        } else if (command.equals("/explain")) {
            explainSelection();
        } else {
            responseArea.append("\n\nSystem: Unknown command. Type /help for a list of commands.");
        }
        requestArea.setText("");
    }

    private void explainSelection() {
        IEditorPart editor = lastActiveEditor;
        if (editor == null) {
            editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        }
        
        if (editor instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) editor;
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
            }
        } else {
            responseArea.append("\n\nSystem: No active editor window found.");
        }
    }
    
    private void changeEndpoint(String command) {
        Pattern pattern = Pattern.compile("/llmEndpoint\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String newEndpoint = matcher.group(1);
            ollamaEndpoint = "http://" + newEndpoint + "/api/chat";
            responseArea.append("\n\nSystem: Endpoint changed to " + ollamaEndpoint);
            consoleStream.println("[" + LocalDateTime.now().format(formatter) + " - System]\nEndpoint changed to " + ollamaEndpoint);
        } else {
            responseArea.append("\n\nSystem: Invalid endpoint format. Use /llmEndpoint myhostname:myport");
        }
    }
    
    private void changeModel(String command) {
        Pattern pattern = Pattern.compile("/model\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            model = matcher.group(1);
            responseArea.append("\n\nSystem: Model changed to " + model);
            consoleStream.println("[" + LocalDateTime.now().format(formatter) + " - System]\nModel changed to " + model);
        } else {
            responseArea.append("\n\nSystem: Invalid model format. Use /model <name>");
        }
    }
    
    private void showHelp() {
        String helpMessage = "\n\nSystem: Available commands:\n" +
                            "/llmEndpoint myhostname:myport - Change the Ollama endpoint\n" +
                            "/model <name> - Change the model used\n" +
                            "/help - Show this help message\n" +
                            "/clear - Clear the response window\n" +
                            "/explain - Explain the selected text";
        responseArea.append(helpMessage);
    }
    
    private void clearResponseArea() {
        responseArea.setText("");
    }
    
    private void handleStartOverAction() {
        // Clear the history and reset the session ID
        history.clear();
        sessionId = UUID.randomUUID().toString();
        
        // Clear the response area
        responseArea.setText("");
        
        // Set initial messages
        responseArea.setText("Welcome to Manorrock Assistant");
        consoleStream.println("Chat Log\n---------");
        
        // Show help message
        showHelp();
    }
    
    private void processMessage(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", message);
            
            // Add the new message to the history
            history.add(messageObject);
            if (history.size() > 50) {
                history.removeFirst();
            }
            
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("model", model);
            jsonInput.put("messages", new JSONArray(history));
            jsonInput.put("stream", true);
            jsonInput.put("session_id", sessionId);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput.toString()))
                .build();
            
            sendButton.setEnabled(false);
            progressBar.setVisible(true);
            
            client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenApply(HttpResponse::body)
                .thenAccept(lines -> {
                    StringBuilder responseBuilder = new StringBuilder();
                    final boolean[] isFirstLine = {true};
                    
                    lines.forEach(line -> {
                        try {
                            JSONObject jsonObject = new JSONObject(line);
                            if (jsonObject.has("session_id")) {
                                sessionId = jsonObject.getString("session_id");
                            }
                            
                            if (jsonObject.has("messages")) {
                                JSONArray messages = jsonObject.getJSONArray("messages");
                                for (int i = 0; i < messages.length(); i++) {
                                    JSONObject msg = messages.getJSONObject(i);
                                    if ("assistant".equals(msg.getString("role"))) {
                                        String content = msg.getString("content");
                                        responseBuilder.append(content);
                                        Display.getDefault().asyncExec(() -> {
                                            if (isFirstLine[0]) {
                                                responseArea.append("\n\nAssistant: " + content);
                                                isFirstLine[0] = false;
                                            } else {
                                                responseArea.append(content);
                                            }
                                        });
                                    }
                                }
                            } else if (jsonObject.has("message")) {
                                String content = jsonObject.getJSONObject("message").getString("content");
                                responseBuilder.append(content);
                                Display.getDefault().asyncExec(() -> {
                                    if (isFirstLine[0]) {
                                        responseArea.append("\n\nAssistant: " + content);
                                        isFirstLine[0] = false;
                                    } else {
                                        responseArea.append(content);
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Display.getDefault().asyncExec(() -> {
                                consoleStream.println("[" + timestamp + " - Error]\n" + e.getMessage());
                                String errorMessage = "Failed to parse JSON response: " + e.getMessage();
                                responseArea.append("\n\nAssistant: " + errorMessage);
                            });
                        }
                    });
                    
                    String response = responseBuilder.toString().trim();
                    Display.getDefault().asyncExec(() -> {
                        consoleStream.println("[" + timestamp + " - Assistant]\n" + response);
                        sendButton.setEnabled(true);
                        progressBar.setVisible(false);
                        
                        // Add the assistant's response to the history
                        try {
                            JSONObject responseObject = new JSONObject();
                            responseObject.put("role", "assistant");
                            responseObject.put("content", response);
                            history.add(responseObject);
                            if (history.size() > 50) {
                                history.removeFirst();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                })
                .exceptionally(e -> {
                    Display.getDefault().asyncExec(() -> {
                        consoleStream.println("[" + timestamp + " - Error]\n" + e.getMessage());
                        String errorMessage = "Ollama is unavailable.";
                        responseArea.append("\n\nAssistant: " + errorMessage);
                        sendButton.setEnabled(true);
                        progressBar.setVisible(false);
                    });
                    return null;
                });
        } catch (Exception e) {
            consoleStream.println("[" + timestamp + " - Error]\n" + e.getMessage());
            String errorMessage = "Ollama is unavailable.";
            responseArea.append("\n\nAssistant: " + errorMessage);
            sendButton.setEnabled(true);
            progressBar.setVisible(false);
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
            lastActiveEditor = (IEditorPart) part;
        }
    }
}
