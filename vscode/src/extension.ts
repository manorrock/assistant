import * as vscode from 'vscode';
import { spawn } from 'child_process';
import * as os from 'os';
import * as path from 'path';

export function activate(context: vscode.ExtensionContext) {
  const disposable = vscode.commands.registerCommand('assistant.showPanel', () => {
    const panel = vscode.window.createWebviewPanel(
      'assistantPanel',
      'Assistant',
      vscode.ViewColumn.One,
      { enableScripts: true }
    );
    panel.webview.html = getWebviewContent();

    const outputChannel = vscode.window.createOutputChannel('Manorrock Assistant');

    // Get CLI path and endpoint from configuration or use defaults
    const config = vscode.workspace.getConfiguration('assistant');
    let cliPath = config.get<string>('cliPath') || path.join(os.homedir(), '.manorrock', 'assistant', 'cli.jar');
    const endpoint = config.get<string>('endpoint') || 'http://localhost:11434';

    // Replace ~ with os.homedir() in cliPath
    if (cliPath.startsWith('~') || cliPath.startsWith('%USERPROFILE%')) {
      cliPath = path.join(os.homedir(), cliPath.slice(cliPath.indexOf(path.sep) + 1));
    }

    // Handle messages from the webview
    panel.webview.onDidReceiveMessage((message: { type: string; text: string }) => {
      if (message.type === 'sendMessage') {
        try {
          outputChannel.appendLine(`Spawning process with CLI path: ${cliPath} and endpoint: ${endpoint}`);
          outputChannel.appendLine(`Input sent to process: ${message.text}`);

          // Launch CLI process for each request
          const cliProcess = spawn('java', [
            '-jar',
            cliPath,
            '-e',
            endpoint,
            '--stdin'
          ]);

          // Send the message to the CLI process
          cliProcess.stdin.write(`${message.text}\n`);
          cliProcess.stdin.end();

          // Handle CLI output
          let isFirstChunk = true;
          cliProcess.stdout.on('data', (data) => {
            const output = data.toString();
            outputChannel.appendLine(`Output received from process: ${output}`);
            if (isFirstChunk) {
              panel.webview.postMessage({ type: 'cli-output', text: `Assistant: ${output}` });
              isFirstChunk = false;
            } else {
              panel.webview.postMessage({ type: 'cli-output', text: output });
            }
          });

          // Handle CLI process termination
          cliProcess.on('close', (code) => {
            if (code !== 0) {
              const exitMessage = `CLI process exited with code ${code}`;
              outputChannel.appendLine(exitMessage);
              panel.webview.postMessage({ type: 'cli-output', text: exitMessage });
            }
          });

          // Handle CLI errors
          cliProcess.on('error', (error) => {
            const errorMessage = (error as Error).message;
            outputChannel.appendLine(`Error: ${errorMessage}`);
            panel.webview.postMessage({ type: 'cli-output', text: `Error: ${errorMessage}` });
          });
        } catch (error) {
          const errorMessage = (error as Error).message;
          outputChannel.appendLine(`Exception: ${errorMessage}`);
          panel.webview.postMessage({ type: 'cli-output', text: `Exception: ${errorMessage}` });
        }
      }
    });
  });

  context.subscriptions.push(disposable);
}

function getWebviewContent(): string {
  return `
  <html>
    <head>
      <meta charset="UTF-8">
      <style>
        body { font-family: 'Times New Roman'; margin: 0; padding: 0; }
        .container { display: flex; height: 100vh; }
        .left, .right { flex: 1; display: flex; flex-direction: column; padding: 10px; }
        textarea { width: 100%; flex: 1; margin-bottom: 10px; }
        button { margin-right: 10px; }
        .dropZone { border: 1px solid black; padding: 10px; text-align: center; }
      </style>
    </head>
    <body>
      <h1>Assistant</h1>
      <div id="messages"></div>
      <input id="inputBox" type="text" />
      <button id="sendBtn">Send</button>
      <script>
        const vscode = acquireVsCodeApi();
        document.getElementById('sendBtn').addEventListener('click', () => {
          const message = document.getElementById('inputBox').value;
          vscode.postMessage({ type: 'sendMessage', text: message });
        });

        window.addEventListener('message', event => {
          const { type, text } = event.data;
          if (type === 'cli-output') {
            const messagesDiv = document.getElementById('messages');
            messagesDiv.innerHTML += '<p>' + text + '</p>';
          }
        });
      </script>
    </body>
  </html>
  `;
}

export function deactivate() {}