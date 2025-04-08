import * as vscode from 'vscode'; // Add this import statement
import { spawn } from 'child_process';
import * as os from 'os';
import * as path from 'path';
import * as fs from 'fs'; // Add this import for file system operations

function getJavaPath(): string {
  const javaHome = vscode.workspace.getConfiguration('java').get<string>('home') 
    || process.env.JAVA_HOME;
  
  return javaHome ? path.join(javaHome, 'bin', 'java') : 'java';
}

function checkCliExists(cliPath: string): boolean {
  return fs.existsSync(cliPath);
}

export function activate(context: vscode.ExtensionContext) {
  console.log('Activating Manorrock Assistant extension'); // Add logging
  const provider = new AssistantViewProvider(context);
  
  // Get CLI path
  const config = vscode.workspace.getConfiguration('assistant');
  let cliPath = config.get<string>('cliPath') || path.join(os.homedir(), '.manorrock', 'assistant', 'cli.jar');
  if (cliPath.startsWith('~') || cliPath.startsWith('%USERPROFILE%')) {
    cliPath = path.join(os.homedir(), cliPath.slice(cliPath.indexOf(path.sep) + 1));
  }
  
  // Check if CLI exists and store the result
  const cliExists = checkCliExists(cliPath);
  context.workspaceState.update('cliExists', cliExists);
  
  if (!cliExists) {
    vscode.window.showWarningMessage(
      'Manorrock Assistant CLI not found. Please visit https://github.com/manorrock/assistant?tab=readme-ov-file#quick-install for installation instructions.',
      'Open Instructions'
    ).then(selection => {
      if (selection === 'Open Instructions') {
        vscode.env.openExternal(vscode.Uri.parse('https://github.com/manorrock/assistant?tab=readme-ov-file#quick-install'));
      }
    });
  }
  
  // Set up initial endpoint configuration
  updateLLMEndpoint();

  // Listen for configuration changes
  context.subscriptions.push(
    vscode.workspace.onDidChangeConfiguration(e => {
      if (e.affectsConfiguration('com.manorrock.assistant.llmEndpoint')) {
        updateLLMEndpoint();
      }
    })
  );

  // We don't need the assistant.newSession command since we handle /new directly in the webview handler

  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider('assistantView', provider, {
      webviewOptions: { retainContextWhenHidden: true }
    }) // Ensure view ID matches
  );
  console.log('Webview provider registered'); // Add logging

  context.subscriptions.push(
    vscode.commands.registerCommand('assistant.show', async () => {
      await vscode.commands.executeCommand('workbench.view.extension.assistantView');
      vscode.commands.executeCommand('vscode.moveViews', {
        viewIds: ['assistantView'],
        destinationId: 'workbench.view.extension.secondarySideBar',
        position: 'right'
      });
    })
  );

  vscode.commands.executeCommand('setContext', 'assistantView', true);
}

function updateLLMEndpoint() {
  const config = vscode.workspace.getConfiguration();
  const endpoint = config.get<string>('com.manorrock.assistant.llmEndpoint');
  
  // If no endpoint is set, silently return and use default
  if (!endpoint) {
    return;
  }

  // We can't directly get the provider instance, so we'll need to register a new command
  // that the provider will need to handle
  vscode.commands.executeCommand('assistant.updateEndpoint', endpoint);
}

class AssistantViewProvider implements vscode.WebviewViewProvider {
  private _view: vscode.WebviewView | undefined;
  private cliProcess: any | undefined;
  public outputChannel: vscode.OutputChannel;

  constructor(private readonly context: vscode.ExtensionContext) {
    this.outputChannel = vscode.window.createOutputChannel('Manorrock Assistant');
  }

  private startCliProcess(cliPath: string): void {
    if (this.cliProcess) {
      return; // Process already running
    }
    
    this.outputChannel.appendLine(`Starting persistent CLI process with path: ${cliPath}`);
    this.cliProcess = spawn(getJavaPath(), ['-jar', cliPath, '-i', '--no-prefix']);

    let outputTimeout: NodeJS.Timeout;

    this.cliProcess.stdout.on('data', (data: Buffer) => {
      const output = data.toString();
      this.outputChannel.appendLine(`Output received: ${output}`);
      if (this._view) {
        this._view.webview.postMessage({ type: 'processing', value: true });
        this._view.webview.postMessage({ type: 'cli-output', text: output });
        
        // Clear any existing timeout
        if (outputTimeout) {
          clearTimeout(outputTimeout);
        }
        
        // Set a new timeout to hide the progress bar after a delay
        outputTimeout = setTimeout(() => {
          if (this._view) {
            this._view.webview.postMessage({ type: 'processing', value: false });
          }
        }, 500); // Wait 500ms after last output before hiding progress
      }
    });

    this.cliProcess.stderr.on('data', (data: Buffer) => {
      const error = data.toString();
      this.outputChannel.appendLine(`Error: ${error}`);
      if (this._view) {
        this._view.webview.postMessage({ type: 'processing', value: true });
        this._view.webview.postMessage({ type: 'cli-output', text: `Error: ${error}` });
      }
    });

    this.cliProcess.on('close', (code: number) => {
      this.outputChannel.appendLine(`CLI process exited with code ${code}`);
      this.cliProcess = undefined;
      if (code !== 0) {
        if (this._view) {
          this._view.webview.postMessage({ 
            type: 'cli-output', 
            text: `CLI process terminated unexpectedly with code ${code}. You may need to restart the extension.` 
          });
        }
      }
      if (outputTimeout) {
        clearTimeout(outputTimeout);
      }
      if (this._view) {
        this._view.webview.postMessage({ type: 'processing', value: false });
      }
    });

    this.cliProcess.on('error', (error: Error) => {
      this.outputChannel.appendLine(`CLI process error: ${error.message}`);
      if (this._view) {
        this._view.webview.postMessage({ type: 'cli-output', text: `Error: ${error.message}` });
        this._view.webview.postMessage({ type: 'processing', value: false });
      }
      this.cliProcess = undefined;
    });
  }

  public stopCliProcess(): void {
    if (this.cliProcess) {
      this.cliProcess.kill();
      this.cliProcess = undefined;
    }
  }

  resolveWebviewView(webviewView: vscode.WebviewView) {
    this._view = webviewView;
    console.log('Resolving Webview View');
    webviewView.webview.options = { enableScripts: true };
    webviewView.webview.html = this.getWebviewContent();

    const config = vscode.workspace.getConfiguration('assistant');
    let cliPath = config.get<string>('cliPath') || path.join(os.homedir(), '.manorrock', 'assistant', 'cli.jar');

    if (cliPath.startsWith('~') || cliPath.startsWith('%USERPROFILE%')) {
      cliPath = path.join(os.homedir(), cliPath.slice(cliPath.indexOf(path.sep) + 1));
    }

    // Check if CLI exists and show appropriate message
    const cliExists = this.context.workspaceState.get('cliExists', false);
    
    if (!cliExists) {
      webviewView.webview.postMessage({ 
        type: 'cli-output', 
        text: '⚠️ **CLI Not Found**\n\nThe Manorrock Assistant CLI was not found at the expected location.\n\nPlease visit [installation instructions](https://github.com/manorrock/assistant?tab=readme-ov-file#quick-install) to set up the CLI.'
      });
      return;
    }

    // Start the persistent CLI process
    this.startCliProcess(cliPath);

    // Initialize a new session with the Manorrock Assistant welcome message
    webviewView.webview.postMessage({
      type: 'newSession',
      message: '# Manorrock Assistant\n\nReady to answer! Use /help for help'
    });

    webviewView.webview.onDidReceiveMessage(async (message: { type: string; text: string }) => {
      if (message.type === 'sendMessage') {
        // Handle /clear command to clear the UI only (without resetting CLI state)
        if (message.text.trim() === '/clear') {
          webviewView.webview.postMessage({
            type: 'newSession',
            message: ''
          });
          return;
        }
        
        // Handle /new command
        if (message.text.trim() === '/new') {
          webviewView.webview.postMessage({
            type: 'newSession',
            message: 'Started a new chat session.'
          });
          message.text = '/new';
        }

        // Enhanced /explain command handling
        if (message.text.startsWith('/explain')) {
          const editor = vscode.window.activeTextEditor;
          if (editor) {
            const selection = editor.selection;
            const text = selection.isEmpty ? editor.document.getText() : editor.document.getText(selection);
            if (text.trim().length === 0) {
              webviewView.webview.postMessage({ 
                type: 'cli-output', 
                text: 'No content to explain. Please select some text or ensure the file has content.' 
              });
              webviewView.webview.postMessage({ type: 'processing', value: false });
              return;
            }
            
            const fileName = path.basename(editor.document.fileName);
            const fileInfo = selection.isEmpty ? 
              `entire file: ${fileName}` : 
              `selection from ${fileName} (${selection.start.line + 1}:${selection.start.character + 1} to ${selection.end.line + 1}:${selection.end.character + 1})`;
            
            message.text = `/explain\nExplaining ${fileInfo}\n-----------------------------------------\n${text}`;
            
            webviewView.webview.postMessage({ type: 'processing', value: true });
            webviewView.webview.postMessage({ 
              type: 'cli-output', 
              text: `Explaining ${fileInfo}...\n` 
            });
          } else {
            webviewView.webview.postMessage({ 
              type: 'cli-output', 
              text: 'Please select a snippet or open a file to use the /explain command.' 
            });
            webviewView.webview.postMessage({ type: 'processing', value: false });
            return;
          }
        }
        
        try {
          if (!this.cliProcess) {
            this.startCliProcess(cliPath);
          }

          this.outputChannel.appendLine(`Sending to CLI: ${message.text}`);
          webviewView.webview.postMessage({ type: 'processing', value: true });
          this.cliProcess.stdin.write(`${message.text}\n`);
        } catch (error) {
          const errorMessage = (error as Error).message;
          this.outputChannel.appendLine(`Exception: ${errorMessage}`);
          webviewView.webview.postMessage({ type: 'cli-output', text: `Exception: ${errorMessage}` });
          webviewView.webview.postMessage({ type: 'processing', value: false });
          
          // Try to recover by restarting the CLI process
          this.stopCliProcess();
          this.startCliProcess(cliPath);
        }
      }
    });

    // Handle cleanup when the webview is disposed
    webviewView.onDidDispose(() => {
      this.stopCliProcess();
    });
  }

  private getWebviewContent(): string {
    const config = vscode.workspace.getConfiguration('assistant');
    const theme = config.get<string>('theme') || 'default';
    return `
      <html>
        <head>
          <meta charset="UTF-8">
          <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
          <style>
            body { 
              font-family: var(--vscode-font-family);
              font-size: var(--vscode-font-size);
              margin: 0;
              padding: 0;
              display: flex;
              flex-direction: column;
              height: 100vh;
              background: var(--vscode-editor-background);
              color: var(--vscode-editor-foreground);
            }
            .container {
              flex: 1;
              display: flex;
              flex-direction: column;
              min-height: 0;
            }
            #messages {
              flex: 1 1 auto;
              overflow-y: auto;
              padding: 8px;
              min-height: 0;
            }
            #inputContainer {
              flex: 0 0 auto;
              display: flex;
              padding: 8px;
              border-top: 1px solid var(--vscode-editorGroup-border);
            }
            #inputBox {
              flex: 1;
              margin-right: 8px;
              resize: none;
              background: var(--vscode-input-background);
              border: 1px solid var(--vscode-input-border);
              color: var(--vscode-input-foreground);
              padding: 4px 8px;
              font-family: inherit;
            }
            #inputBox:focus {
              outline: 1px solid var(--vscode-focusBorder);
              border-color: var(--vscode-focusBorder);
            }
            #sendBtn {
              background: var(--vscode-button-background);
              color: var(--vscode-button-foreground);
              border: none;
              padding: 4px 12px;
              cursor: pointer;
            }
            #sendBtn:hover {
              background: var(--vscode-button-hoverBackground);
            }

            .chat-message {
              margin: 4px 0;
              clear: both;
            }
            .message-content-wrapper {
              padding: 8px;
              position: relative;
            }
            .message-header {
              font-weight: 500;
              margin-bottom: 8px;
              color: var(--vscode-foreground);
              border-bottom: 1px solid var(--vscode-editorGroup-border);
              padding-bottom: 4px;
              display: flex;
              justify-content: space-between;
              align-items: center;
            }
            .message-content {
              white-space: pre-wrap;
              font-family: var(--vscode-font-family);
            }
            /* Make Markdown content more compact */
            .message-content p {
              margin: 0.15em 0;
            }
            .message-content h1,
            .message-content h2,
            .message-content h3,
            .message-content h4,
            .message-content h5,
            .message-content h6 {
              margin: 0.3em 0 0.15em 0;
            }
            .message-content ul,
            .message-content ol {
              margin: 0.15em 0;
              padding-left: 1.2em;
            }
            .message-content li {
              margin: 0;
            }
            .message-content pre {
              margin: 0.3em 0;
            }
            .message-content blockquote {
              margin: 0.15em 0;
              padding-left: 0.6em;
            }
            .message-content pre code {
              padding: 0.3em;
            }
            .message-content table {
              margin: 0.3em 0;
              border-spacing: 0;
              border-collapse: collapse;
            }
            .message-content td,
            .message-content th {
              padding: 0.2em 0.4em;
            }
            .toggle-markdown {
              background: transparent;
              color: var(--vscode-button-secondaryForeground);
              border: none;
              padding: 2px;
              font-size: 14px;
              cursor: pointer;
              opacity: 0.8;
              display: none;
              margin-left: 8px;
            }
            .has-markdown .toggle-markdown {
              display: inline-flex;
              align-items: center;
            }
            .toggle-markdown:hover {
              opacity: 1;
            }
            
            /* Progress indicator that will be shown in the message area instead of a separate status area */
            .progress-indicator {
              margin-top: 8px;
              width: 100%;
              height: 2px;
              background: var(--vscode-editor-background);
              position: relative;
              overflow: hidden;
            }
            .progress-indicator::before {
              content: '';
              position: absolute;
              top: 0;
              left: -50%;
              height: 100%;
              width: 50%;
              background: linear-gradient(90deg, transparent, var(--vscode-progressBar-background), transparent);
              animation: progress 2s infinite;
            }
            .thinking-message {
              margin-top: 4px;
              font-size: 0.9em;
              opacity: 0.8;
            }
            @keyframes progress {
              0% { transform: translateX(0); }
              50% { transform: translateX(300%); }
              100% { transform: translateX(0); }
            }
          </style>
        </head>
        <body class="theme-${theme}">
          <div class="container">
            <div id="messages">
              <div id="outputArea"></div>
            </div>
            <div id="inputContainer">
              <textarea id="inputBox" rows="3" placeholder="Type your message..."></textarea>
              <button id="sendBtn">Send</button>
            </div>
          </div>
          <script>
            const vscode = acquireVsCodeApi();
            const inputBox = document.getElementById('inputBox');
            const sendBtn = document.getElementById('sendBtn');
            const outputArea = document.getElementById('outputArea');
            let progressIndicator = null;

            marked.setOptions({
              gfm: true,
              breaks: true,
              tables: true
            });

            function setProcessing(processing) {
              sendBtn.disabled = processing;
              inputBox.disabled = processing;
              
              if (processing) {
                // Create and show progress indicator within the messages area
                if (!progressIndicator) {
                  const indicatorDiv = document.createElement('div');
                  indicatorDiv.className = 'chat-message';
                  
                  const wrapper = document.createElement('div');
                  wrapper.className = 'message-content-wrapper';
                  
                  const header = document.createElement('div');
                  header.className = 'message-header';
                  
                  const title = document.createElement('span');
                  title.textContent = 'Assistant';
                  header.appendChild(title);
                  
                  const content = document.createElement('div');
                  content.className = 'message-content';
                  
                  // Add the progress indicator
                  const indicator = document.createElement('div');
                  indicator.className = 'progress-indicator';
                  
                  // Add the thinking message
                  const thinking = document.createElement('div');
                  thinking.className = 'thinking-message';
                  thinking.textContent = 'Assistant is thinking...';
                  
                  content.appendChild(thinking);
                  content.appendChild(indicator);
                  
                  wrapper.appendChild(header);
                  wrapper.appendChild(content);
                  indicatorDiv.appendChild(wrapper);
                  
                  outputArea.appendChild(indicatorDiv);
                  progressIndicator = indicatorDiv;
                  
                  // Scroll to the bottom to show the progress indicator
                  outputArea.parentElement.scrollTop = outputArea.parentElement.scrollHeight;
                }
              } else {
                // Remove progress indicator when processing is complete
                if (progressIndicator && progressIndicator.parentNode) {
                  progressIndicator.parentNode.removeChild(progressIndicator);
                  progressIndicator = null;
                }
                
                // Set focus back to input when processing is complete
                setTimeout(() => inputBox.focus(), 0);
              }
            }

            function createChatMessage(text, isUser = false) {
              const messageDiv = document.createElement('div');
              messageDiv.className = 'chat-message';
              
              const wrapper = document.createElement('div');
              wrapper.className = 'message-content-wrapper';
              
              const header = document.createElement('div');
              header.className = 'message-header';
              
              const title = document.createElement('span');
              title.textContent = isUser ? 'You' : 'Assistant';
              header.appendChild(title);
              
              const content = document.createElement('div');
              content.className = 'message-content';
              
              // Only show toggle for Assistant messages
              if (!isUser) {
                wrapper.classList.add('has-markdown');
                const toggleBtn = document.createElement('button');
                toggleBtn.className = 'toggle-markdown';
                toggleBtn.innerHTML = '📄'; // Document icon for text view
                toggleBtn.title = 'View as Markdown';
                toggleBtn.onclick = () => {
                  if (toggleBtn.innerHTML === '📄') {
                    content.innerHTML = marked.parse(text);
                    toggleBtn.innerHTML = '📝'; // Markdown icon
                    toggleBtn.title = 'View as Text';
                  } else {
                    content.textContent = text;
                    toggleBtn.innerHTML = '📄';
                    toggleBtn.title = 'View as Markdown';
                  }
                };
                header.appendChild(toggleBtn);
              }
              
              // Initially render as plain text
              content.textContent = text;
              
              wrapper.appendChild(header);
              wrapper.appendChild(content);
              messageDiv.appendChild(wrapper);
              return messageDiv;
            }

            sendBtn.addEventListener('click', () => {
              const message = inputBox.value;
              if (!message.trim()) return;
              
              outputArea.appendChild(createChatMessage(message, true));
              outputArea.parentElement.scrollTop = outputArea.parentElement.scrollHeight;
              
              setProcessing(true);
              vscode.postMessage({ type: 'sendMessage', text: message });
              inputBox.value = '';
              // Focus is handled by setProcessing when it's set to false
            });

            inputBox.addEventListener('keydown', (event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                sendBtn.click();
              }
            });

            // Focus input box on initial load
            window.addEventListener('load', () => {
              setTimeout(() => inputBox.focus(), 100);
            });

            window.addEventListener('message', event => {
              const message = event.data;
              
              if (message.type === 'cli-output') {
                outputArea.appendChild(createChatMessage(message.text));
                outputArea.parentElement.scrollTop = outputArea.parentElement.scrollHeight;
              } else if (message.type === 'processing') {
                setProcessing(message.value);
              } else if (message.type === 'newSession') {
                outputArea.innerHTML = '';
                if (message.message) {
                  outputArea.appendChild(createChatMessage(message.message));
                }
                setProcessing(false);
              }
            });
          </script>
        </body>
      </html>
    `;
  }
}

export function deactivate() {}