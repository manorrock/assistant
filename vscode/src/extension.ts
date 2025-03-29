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

  let cliPath = config.get<string>('assistant.cliPath') || path.join(os.homedir(), '.manorrock', 'assistant', 'cli.jar');
  if (cliPath.startsWith('~') || cliPath.startsWith('%USERPROFILE%')) {
    cliPath = path.join(os.homedir(), cliPath.slice(cliPath.indexOf(path.sep) + 1));
  }

  try {
    const cliProcess = spawn(getJavaPath(), ['-jar', cliPath, '--stdin']);
    cliProcess.stdin.write(`/llmEndpoint ${endpoint}\n`);
    cliProcess.stdin.end();

    cliProcess.on('error', (error) => {
      vscode.window.showErrorMessage(`Failed to set LLM endpoint: ${error.message}`);
    });
  } catch (error) {
    vscode.window.showErrorMessage(`Failed to launch CLI process: ${(error as Error).message}`);
  }
}

class AssistantViewProvider implements vscode.WebviewViewProvider {
  constructor(private readonly context: vscode.ExtensionContext) {}

  private _view: vscode.WebviewView | undefined;

  resolveWebviewView(webviewView: vscode.WebviewView) {
    this._view = webviewView;
    console.log('Resolving Webview View'); // Add logging
    webviewView.webview.options = { enableScripts: true };
    webviewView.webview.html = this.getWebviewContent();

    const outputChannel = vscode.window.createOutputChannel('Manorrock Assistant');
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
    } else {
      // Show ready message if CLI exists
      webviewView.webview.postMessage({
        type: 'cli-output',
        text: 'Ready to answer! Use /help for help'
      });
    }

    webviewView.webview.onDidReceiveMessage(async (message: { type: string; text: string }) => {
      if (message.type === 'sendMessage') {
        // Handle /clear command to clear the UI only (without resetting CLI state)
        if (message.text.trim() === '/clear') {
          // Clear the UI
          webviewView.webview.postMessage({
            type: 'newSession',
            message: ''
          });
          return;
        }
        
        // Handle /new command by first clearing UI, then sending to CLI
        if (message.text.trim() === '/new') {
          // Clear the UI
          webviewView.webview.postMessage({
            type: 'newSession',
            message: 'Started a new chat session.'
          });
          
          // Also dispatch to CLI so it resets its state
          try {
            outputChannel.appendLine(`Sending /new command to CLI`);
            const cliProcess = spawn(getJavaPath(), ['-jar', cliPath, '--stdin']);
            cliProcess.stdin.write(`/new\n`);
            cliProcess.stdin.end();
            
            cliProcess.stdout.on('data', (data) => {
              const output = data.toString();
              outputChannel.appendLine(`CLI response to /new: ${output}`);
            });
          } catch (error) {
            outputChannel.appendLine(`Error sending /new to CLI: ${(error as Error).message}`);
          }
          return;
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
              return;
            }
            
            const fileName = path.basename(editor.document.fileName);
            const fileInfo = selection.isEmpty ? 
              `entire file: ${fileName}` : 
              `selection from ${fileName} (${selection.start.line + 1}:${selection.start.character + 1} to ${selection.end.line + 1}:${selection.end.character + 1})`;
            
            const prompt = `/explain\nExplaining ${fileInfo}\n-----------------------------------------\n${text}`;
            message.text = prompt;
            
            // Let the user know what's being explained
            webviewView.webview.postMessage({ 
              type: 'cli-output', 
              text: `Explaining ${fileInfo}...\n` 
            });
          } else {
            webviewView.webview.postMessage({ 
              type: 'cli-output', 
              text: 'Please select a snippet or open a file to use the /explain command.' 
            });
            return;
          }
        }
        
        try {
          outputChannel.appendLine(`Spawning process with CLI path: ${cliPath}`);
          outputChannel.appendLine(`Input sent to process: ${message.text}`);
          const cliProcess = spawn(getJavaPath(), ['-jar', cliPath, '--stdin']);
          cliProcess.stdin.write(`${message.text}\n`);
          cliProcess.stdin.end();

          cliProcess.stdout.on('data', (data) => {
            const output = data.toString();
            outputChannel.appendLine(`Output received from process: ${output}`);
            webviewView.webview.postMessage({ type: 'cli-output', text: output });
          });

          cliProcess.on('close', (code) => {
            if (code !== 0) {
              const exitMessage = `CLI process exited with code ${code}`;
              outputChannel.appendLine(exitMessage);
              webviewView.webview.postMessage({ type: 'cli-output', text: exitMessage });
            }
            webviewView.webview.postMessage({ type: 'process-complete' });
          });

          cliProcess.on('error', (error) => {
            const errorMessage = (error as Error).message;
            outputChannel.appendLine(`Error: ${errorMessage}`);
            webviewView.webview.postMessage({ type: 'cli-output', text: `Error: ${errorMessage}` });
          });
        } catch (error) {
          const errorMessage = (error as Error).message;
          outputChannel.appendLine(`Exception: ${errorMessage}`);
          webviewView.webview.postMessage({ type: 'cli-output', text: `Exception: ${errorMessage}` });
        }
      }
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
              font-family: var(--vscode-font-family, 'Segoe WPC', 'Segoe UI', sans-serif); 
              font-size: var(--vscode-font-size, 13px); 
              margin: 0; 
              padding: 0; 
              display: flex; 
              flex-direction: column; 
              height: 100vh; 
              background-color: var(--vscode-editor-background); 
              color: var(--vscode-editor-foreground);
            }
            .container { 
              flex: 1; 
              display: flex; 
              flex-direction: column; 
              min-height: 0; /* Crucial for nested flex scrolling */
            }
            #messages { 
              flex: 1 1 auto;
              overflow-y: auto;
              padding: 10px;
              min-height: 0; /* Allows proper scrolling */
            }
            #inputContainer { 
              flex: 0 0 auto; /* Prevents shrinking/growing */
              display: flex; 
              padding: 10px; 
              border-top: 1px solid var(--vscode-editorGroup-border);
              background: var(--vscode-editor-background);
            }
            #inputBox { 
              flex: 1; 
              margin-right: 10px;
              resize: none;
              background-color: var(--vscode-input-background);
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
              background-color: var(--vscode-button-background);
              color: var(--vscode-button-foreground);
              border: none;
              padding: 4px 12px;
              cursor: pointer;
            }
            #sendBtn:hover {
              background-color: var(--vscode-button-hoverBackground);
            }
            #outputParagraph { white-space: pre-wrap; }

            /* Markdown table styles */
            .markdown-body table {
              border-collapse: collapse;
              width: 100%;
              margin: 1em 0;
            }
            .markdown-body table th,
            .markdown-body table td {
              border: 1px solid var(--vscode-editorGroup-border);
              padding: 6px 13px;
            }
            .markdown-body table tr {
              background-color: var(--vscode-editor-background);
              border-top: 1px solid var(--vscode-editorGroup-border);
            }
            .markdown-body table tr:nth-child(2n) {
              background-color: var(--vscode-list-hoverBackground);
            }
            .markdown-body table thead tr {
              background-color: var(--vscode-editor-lineHighlightBackground);
            }

            #statusArea {
              padding: 8px;
              border-top: 1px solid var(--vscode-editorGroup-border);
              background: var(--vscode-editor-background);
              max-height: 0;
              overflow: hidden;
              transition: max-height 0.3s ease, padding 0.3s ease;
              display: none;
            }
            
            #statusArea.active {
              max-height: 40px;
              padding: 8px;
              display: block;
            }

            .progress {
              width: 100%;
              height: 2px;
              background: var(--vscode-editor-background);
              position: relative;
              overflow: hidden;
            }

            .progress::before {
              content: '';
              position: absolute;
              top: 0;
              left: -50%;
              height: 100%;
              width: 50%;
              background: linear-gradient(
                90deg,
                transparent 0%,
                var(--vscode-progressBar-background) 50%,
                transparent 100%
              );
              animation: cylonScan 2s infinite ease-in-out;
            }
            
            @keyframes cylonScan {
              0% { transform: translateX(0); }
              50% { transform: translateX(300%); }
              100% { transform: translateX(0); }
            }

            .typing {
              margin: 8px 0;
              color: var(--vscode-descriptionForeground);
              font-style: italic;
            }
          </style>
        </head>
        <body class="theme-${theme}">
          <h1>Manorrock Assistant</h1>
          <div class="container">
            <div id="messages">
              <div id="outputArea" class="markdown-body"></div>
            </div>
            <div id="statusArea">
              <div class="progress"></div>
              <div id="statusText">Assistant is thinking...</div>
            </div>
            <div id="inputContainer">
              <textarea id="inputBox" rows="3"></textarea> <!-- Changed input to textarea -->
              <button id="sendBtn">Send</button>
            </div>
          </div>
          <script>
            const vscode = acquireVsCodeApi();
            const inputBox = document.getElementById('inputBox');
            const sendBtn = document.getElementById('sendBtn');
            const outputArea = document.getElementById('outputArea');
            const statusArea = document.getElementById('statusArea');

            function setProcessing(processing) {
              statusArea.classList.toggle('active', processing);
              sendBtn.disabled = processing;
              inputBox.disabled = processing;
            }
            
            marked.setOptions({
              gfm: true, // GitHub Flavored Markdown
              breaks: true,
              tables: true // Enable table parsing
            });

            sendBtn.addEventListener('click', () => {
              const message = inputBox.value;
              if (!message.trim()) return;
              
              setProcessing(true);
              vscode.postMessage({ type: 'sendMessage', text: message });
              inputBox.value = '';
            });

            inputBox.addEventListener('keydown', (event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                sendBtn.click();
              }
            });

            window.addEventListener('message', event => {
              const message = event.data;
              
              if (message.type === 'cli-output') {
                const content = marked.parse(message.text);
                outputArea.insertAdjacentHTML('beforeend', content);
                outputArea.parentElement.scrollTop = outputArea.parentElement.scrollHeight;
              } else if (message.type === 'process-complete') {
                setProcessing(false);
              } else if (message.type === 'newSession') {
                outputArea.innerHTML = marked.parse(message.message || '');
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