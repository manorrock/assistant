package com.manorrock.assistant.netbeans;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;

@TopComponent.Description(preferredID = "NetBeansControllerTopComponent", persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "com.example.NetBeansControllerTopComponent")
@ActionRegistration(displayName = "#CTL_NetBeansControllerAction")
@ActionReferences({@ActionReference(path = "Menu/Window", position = 0)})
@TopComponent.OpenActionRegistration(displayName = "#CTL_NetBeansControllerAction", preferredID = "NetBeansControllerTopComponent")
@Messages({"CTL_NetBeansControllerAction=Manorrock Assistant",
    "CTL_NetBeansControllerTopComponent=Manorrock Assistant Window",
    "HINT_NetBeansControllerTopComponent=This is a Manorrock Assistant window"})
public final class NetBeansControllerTopComponent extends TopComponent implements ActionListener, FocusListener {

    private final CLIExecutor cliExecutor;
    private JTextArea responseArea;
    private JTextArea requestArea;
    private JButton sendButton;
    private JProgressBar progressBar;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private InputOutput io;
    private TopComponent lastFocusedEditor;

    public NetBeansControllerTopComponent() {
        cliExecutor = new CLIExecutor();
        initComponents();
        setName(Bundle.CTL_NetBeansControllerTopComponent());
        setToolTipText(Bundle.HINT_NetBeansControllerTopComponent());
        io = IOProvider.getDefault().getIO("Chat Log", false);
        
        if (!cliExecutor.isCliAvailable()) {
            responseArea.setText("Manorrock Assistant CLI not found. Please visit " +
                "https://github.com/manorrock/assistant?tab=readme-ov-file#quick-install " +
                "for installation instructions.");
            sendButton.setEnabled(false);
        } else {
            responseArea.setText("Welcome to Manorrock Assistant\n\nType /help for a list of commands.");
        }
        
        TopComponent.getRegistry().addPropertyChangeListener(evt -> {
            if (TopComponent.Registry.PROP_ACTIVATED.equals(evt.getPropertyName())) {
                TopComponent activated = TopComponent.getRegistry().getActivated();
                if (activated != null && activated.getLookup().lookup(EditorCookie.class) != null) {
                    lastFocusedEditor = activated;
                }
            }
        });
    }

    private void initComponents() {
        responseArea = new JTextArea();
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        requestArea = new JTextArea();
        sendButton = new JButton("Send");
        progressBar = new JProgressBar(0, 100);

        // Set initial message
        responseArea.setText("Welcome to Manorrock Assistant");

        // Show help message on startup
        responseArea.append("\n\nType /help for a list of commands.");

        // Setup key event handler for the requestArea
        requestArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER && !event.isShiftDown()) {
                    handleSendAction();
                    event.consume(); // Prevents the newline from being added
                }
            }
        });

        sendButton.addActionListener(this);

        // Layout setup (simplified)
        setLayout(new BorderLayout());
        add(new JScrollPane(responseArea), BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(requestArea), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) {
            handleSendAction();
        }
    }

    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();

        if (!userMessage.isEmpty()) {
            String timestamp = LocalDateTime.now().format(formatter);

            // Display the user's message in the response area
            responseArea.append("\n\nYou: " + userMessage);

            // Add timestamped message to the Output Window
            io.getOut().println("[" + timestamp + " - You]\n" + userMessage);

            // Clear the request area
            requestArea.setText("");

            // Handle /new command specially to clear UI first
            if (userMessage.equals("/new")) {
                // Clear the UI
                responseArea.setText("");
                responseArea.append("Started a new chat session.\n");
                // Continue to send /new to CLI to reset its state
            }

            // Handle /explain command specially like VSCode does
            if (userMessage.startsWith("/explain")) {
                EditorCookie editorCookie = getLastFocusedEditorCookie();
                if (editorCookie != null) {
                    try {
                        String selectedText = editorCookie.getOpenedPanes()[0].getSelectedText();
                        if (selectedText == null || selectedText.isEmpty()) {
                            selectedText = editorCookie.getDocument().getText(0, editorCookie.getDocument().getLength());
                        }
                        if (selectedText.trim().length() == 0) {
                            responseArea.append("\n\nSystem: No content to explain. Please select some text or ensure the file has content.");
                            return; // This return is correct - no content to explain
                        }
                        
                        DataObject dataObj = lastFocusedEditor.getLookup().lookup(DataObject.class);
                        String fileName = dataObj.getPrimaryFile().getNameExt();
                        String fileInfo = selectedText.equals(editorCookie.getDocument().getText(0, editorCookie.getDocument().getLength())) ?
                            "entire file: " + fileName :
                            "selection from " + fileName;
                        
                        // Modify the message to match VSCode format exactly
                        userMessage = "/explain\nExplaining " + fileInfo + 
                            "\n-----------------------------------------\n" + selectedText;
                        
                        // Let user know what's being explained
                        responseArea.append("\n\nExplaining " + fileInfo + "...\n");
                    } catch (javax.swing.text.BadLocationException e) {
                        responseArea.append("\n\nSystem: Error retrieving text from the editor.");
                        return; // This return is correct - editor error
                    }
                } else {
                    responseArea.append("\n\nSystem: Please select a snippet or open a file to use the /explain command.");
                    return; // This return is correct - no editor
                }
            }

            // Process message through CLI
            processMessage(userMessage);
        }
    }

    private void processMessage(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        
        sendButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        
        cliExecutor.executeCommand(message)
            .thenAccept(response -> {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    responseArea.append("\n\nAssistant: " + response);
                    responseArea.setCaretPosition(responseArea.getDocument().getLength());
                    io.getOut().println("[" + timestamp + " - Assistant]\n" + response);
                    sendButton.setEnabled(true);
                    progressBar.setIndeterminate(false);
                });
            })
            .exceptionally(e -> {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    String errorMessage = "Error: " + e.getMessage();
                    responseArea.append("\n\nSystem: " + errorMessage);
                    io.getOut().println("[" + timestamp + " - Error]\n" + errorMessage);
                    sendButton.setEnabled(true);
                    progressBar.setIndeterminate(false);
                });
                return null;
            });
    }

    private EditorCookie getLastFocusedEditorCookie() {
        if (lastFocusedEditor != null) {
            DataObject dataObject = lastFocusedEditor.getLookup().lookup(DataObject.class);
            if (dataObject != null) {
                return dataObject.getLookup().lookup(EditorCookie.class);
            }
        }
        return null;
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (e.getComponent() instanceof TopComponent) {
            TopComponent tc = (TopComponent) e.getComponent();
            if (tc.getLookup().lookup(EditorCookie.class) != null) {
                lastFocusedEditor = tc;
            }
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        // No action needed
    }
}
