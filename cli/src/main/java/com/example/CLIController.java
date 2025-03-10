package com.example;

import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "assistant-cli", mixinStandardHelpOptions = true, version = "1.0",
        description = "CLI version of the Manorrock Assistant")
public class CLIController implements Callable<Integer> {

    private String llmEndpoint;

    // Default endpoint
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434/api/chat";

    @Option(names = {"-m", "--model"}, description = "Model to use")
    private String model = "llama3";

    @Option(names = {"--stdin"}, description = "Read message from standard input")
    private boolean readFromStdin = false;

    @Parameters(paramLabel = "MESSAGE", description = "Message to send", arity = "0..1")
    private String message;

    private String sessionId = UUID.randomUUID().toString();
    private LinkedList<JSONObject> history = new LinkedList<>();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private Path stateDir = Paths.get(System.getProperty("user.home"), ".manorrock", "assistant", "cli-state");

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CLIController()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        loadState();
        if (llmEndpoint == null) {
            llmEndpoint = DEFAULT_ENDPOINT;
        }
        if (readFromStdin) {
            message = new String(System.in.readAllBytes()).trim();
        }
        if (message != null) {
            handleSendAction(message);
        } else {
            showHelp();
        }
        saveState();
        return 0;
    }

    private void handleSendAction(String userMessage) {
        if (!userMessage.isEmpty()) {
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage);
                return;
            }

            System.out.println("You: " + userMessage);
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
        } else if (command.startsWith("/explain")) {
            explainFromClipboardOrFile(command);
        } else {
            System.out.println("System: Unknown command. Type /help for a list of commands.");
        }
    }

    private void explainFromClipboardOrFile(String command) {
        String textToExplain = null;
        
        // Parse optional file path if provided
        String filePath = null;
        if (command.length() > 9) { // "/explain " + something
            filePath = command.substring(9).trim();
        }
        
        if (filePath != null && !filePath.isEmpty()) {
            // Read from file
            try {
                textToExplain = Files.readString(Paths.get(filePath));
                System.out.println("System: Explaining content from file: " + filePath);
            } catch (IOException e) {
                System.out.println("System: Error reading file: " + e.getMessage());
                return;
            }
        } else {
            // Read from clipboard
            try {
                textToExplain = getClipboardContent();
                System.out.println("System: Explaining content from clipboard");
            } catch (Exception e) {
                System.out.println("System: Failed to access clipboard: " + e.getMessage());
                System.out.println("System: Usage: /explain [file_path] - Explains text from clipboard or specified file");
                return;
            }
        }
        
        if (textToExplain != null && !textToExplain.trim().isEmpty()) {
            String promptPrefix = "Please explain the following text in a clear and concise manner:\n\n";
            processMessage(promptPrefix + textToExplain);
        } else {
            System.out.println("System: No content found to explain.");
        }
    }

    private String getClipboardContent() throws Exception {
        // For Mac/Linux, we can use the 'pbpaste' or 'xclip' commands
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        
        if (os.contains("mac")) {
            pb = new ProcessBuilder("pbpaste");
        } else if (os.contains("nix") || os.contains("nux")) {
            pb = new ProcessBuilder("xclip", "-selection", "clipboard", "-o");
        } else if (os.contains("win")) {
            pb = new ProcessBuilder("powershell.exe", "-command", "Get-Clipboard");
        } else {
            throw new UnsupportedOperationException("Clipboard access not supported on this OS");
        }
        
        Process process = pb.start();
        String content = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("Failed to get clipboard content, exit code: " + exitCode);
        }
        
        return content;
    }

    private void changeEndpoint(String command) {
        String newEndpoint = command.substring(12).trim();
        if (!newEndpoint.startsWith("http://") && !newEndpoint.startsWith("https://")) {
            newEndpoint = "http://" + newEndpoint;
        }
        llmEndpoint = newEndpoint + "/api/chat";
        System.out.println("System: Endpoint changed to " + llmEndpoint);
        saveState();
    }

    private void changeModel(String command) {
        String newModel = command.substring(7).trim();
        model = newModel;
        System.out.println("System: Model changed to " + model);
        saveState();
    }

    private void showHelp() {
        String helpMessage = "\n\nSystem: Available commands:\n" +
                             "/llmEndpoint myhostname:myport - Change the Ollama endpoint\n" +
                             "/model <name> - Change the model used\n" +
                             "/help - Show this help message\n" +
                             "/clear - Clear the response window\n" +
                             "/explain [file_path] - Explain text from clipboard or specified file";
        System.out.println(helpMessage);
    }

    private void clearResponseArea() {
        System.out.println("System: Response area cleared.");
    }

    private void processMessage(String message) {
        String timestamp = LocalDateTime.now().format(formatter);

        try {
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", message);

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
                    .uri(URI.create(llmEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInput.toString()))
                    .build();

            HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
            StringBuilder responseBuilder = new StringBuilder();
            final boolean[] isFirstLine = {true};
            response.body().forEach(line -> {
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
                            if (isFirstLine[0]) {
                                System.out.print("Assistant: " + content);
                                isFirstLine[0] = false;
                            } else {
                                System.out.print(content);
                            }
                        }
                    }
                } else {
                    String content = jsonObject.getJSONObject("message").getString("content");
                    responseBuilder.append(content);
                    if (isFirstLine[0]) {
                        System.out.print("Assistant: " + content);
                        isFirstLine[0] = false;
                    } else {
                        System.out.print(content);
                    }
                }
            });

            String responseText = responseBuilder.toString().trim();

            JSONObject responseObject = new JSONObject();
            responseObject.put("role", "assistant");
            responseObject.put("content", responseText);
            history.add(responseObject);
            if (history.size() > 50) {
                history.removeFirst();
            }
        } catch (Exception e) {
            System.out.println("Assistant: Ollama is unavailable.");
            System.out.println("[" + timestamp + " - Error]\n" + e.getMessage());
        }
    }

    private void loadState() {
        try {
            if (Files.exists(stateDir)) {
                Path historyFile = stateDir.resolve("history.json");
                if (Files.exists(historyFile)) {
                    String content = Files.readString(historyFile);
                    JSONArray jsonArray = new JSONArray(content);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        history.add(jsonArray.getJSONObject(i));
                    }
                }
                Path sessionIdFile = stateDir.resolve("session_id.txt");
                if (Files.exists(sessionIdFile)) {
                    sessionId = Files.readString(sessionIdFile).trim();
                }
                Path endpointFile = stateDir.resolve("endpoint.txt");
                if (Files.exists(endpointFile)) {
                    llmEndpoint = Files.readString(endpointFile).trim();
                } else {
                    llmEndpoint = DEFAULT_ENDPOINT;
                }
                Path modelFile = stateDir.resolve("model.txt");
                if (Files.exists(modelFile)) {
                    model = Files.readString(modelFile).trim();
                } else {
                    model = "llama3";
                }
            } else {
                Files.createDirectories(stateDir);
            }
        } catch (IOException e) {
            System.out.println("Error loading state: " + e.getMessage());
        }
    }

    private void saveState() {
        try {
            Path historyFile = stateDir.resolve("history.json");
            Files.writeString(historyFile, new JSONArray(history).toString());

            Path sessionIdFile = stateDir.resolve("session_id.txt");
            Files.writeString(sessionIdFile, sessionId);

            Path endpointFile = stateDir.resolve("endpoint.txt");
            Files.writeString(endpointFile, llmEndpoint);

            Path modelFile = stateDir.resolve("model.txt");
            Files.writeString(modelFile, model);
        } catch (IOException e) {
            System.out.println("Error saving state: " + e.getMessage());
        }
    }
}
