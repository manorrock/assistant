import * as vscode from 'vscode'; // Add this import statement
import { spawn } from 'child_process';
import * as os from 'os';
import * as path from 'path';

export function activate(context: vscode.ExtensionContext) {
  console.log('Activating Manorrock Assistant extension'); // Add logging
  const provider = new AssistantViewProvider(context);
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

class AssistantViewProvider implements vscode.WebviewViewProvider {
  constructor(private readonly context: vscode.ExtensionContext) {}

  resolveWebviewView(webviewView: vscode.WebviewView) {
    console.log('Resolving Webview View'); // Add logging
    webviewView.webview.options = { enableScripts: true };
    webviewView.webview.html = this.getWebviewContent();

    const outputChannel = vscode.window.createOutputChannel('Manorrock Assistant');
    const config = vscode.workspace.getConfiguration('assistant');
    let cliPath = config.get<string>('cliPath') || path.join(os.homedir(), '.manorrock', 'assistant', 'cli.jar');

    if (cliPath.startsWith('~') || cliPath.startsWith('%USERPROFILE%')) {
      cliPath = path.join(os.homedir(), cliPath.slice(cliPath.indexOf(path.sep) + 1));
    }

    try {
      outputChannel.appendLine(`Spawning process with CLI path: ${cliPath} for /help request`);
      const cliProcess = spawn('java', ['-jar', cliPath, '--stdin']);
      cliProcess.stdin.write(`/help\n`);
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

    webviewView.webview.onDidReceiveMessage(async (message: { type: string; text: string }) => {
      if (message.type === 'sendMessage') {
        if (message.text.startsWith('/explain')) {
          const editor = vscode.window.activeTextEditor;
          if (editor) {
            const selection = editor.selection;
            const text = selection.isEmpty ? editor.document.getText() : editor.document.getText(selection);
            const prompt = `Please explain the content below the line\n-----------------------------------------\n${text}`;
            message.text = prompt;
          } else {
            vscode.window.showInformationMessage('Please select a snippet or open a file.');
            return;
          }
        }
        try {
          outputChannel.appendLine(`Spawning process with CLI path: ${cliPath}`);
          outputChannel.appendLine(`Input sent to process: ${message.text}`);
          const cliProcess = spawn('java', ['-jar', cliPath, '--stdin']);
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
            .container { flex: 1; display: flex; flex-direction: column; }
            #messages { flex: 1; overflow-y: auto; padding: 10px; }
            #inputContainer { display: flex; padding: 10px; border-top: 1px solid var(--vscode-editorGroup-border); }
            #inputBox { flex: 1; margin-right: 10px; }
            #outputParagraph { white-space: pre-wrap; } /* Ensure text wraps */
          </style>
        </head>
        <body class="theme-${theme}">
          <h1>Manorrock Assistant</h1>
          <div class="container">
            <div id="messages"><p id="outputParagraph"></p></div>
            <div id="inputContainer">
              <textarea id="inputBox" rows="3"></textarea> <!-- Changed input to textarea -->
              <button id="sendBtn">Send</button>
            </div>
          </div>
          <script>
            const vscode = acquireVsCodeApi();
            const inputBox = document.getElementById('inputBox');
            const sendBtn = document.getElementById('sendBtn');

            sendBtn.addEventListener('click', () => {
              const message = inputBox.value;
              console.log('Sending message:', message); // Add logging
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
              const { type, text } = event.data;
              console.log('Message received:', type, text); // Add logging
              if (type === 'cli-output') {
                const outputParagraph = document.getElementById('outputParagraph');
                outputParagraph.textContent += text;
              }
            });
          </script>
        </body>
      </html>
    `;
  }
}

export function deactivate() {}