import * as vscode from 'vscode';
import { spawn } from 'child_process';

export function activate(context: vscode.ExtensionContext) {
  const disposable = vscode.commands.registerCommand('assistant.showPanel', () => {
    const panel = vscode.window.createWebviewPanel(
      'assistantPanel',
      'Assistant',
      vscode.ViewColumn.One,
      { enableScripts: true }
    );
    panel.webview.html = getWebviewContent();

    // Launch CLI process
    // const cliProcess = spawn('java', [
    //   '-cp',
    //   'path/to/your/classpath',
    //   'com.example.CLIController',
    // ]);

    // Handle CLI output
    // cliProcess.stdout.on('data', (data) => {
    //   panel.webview.postMessage({ type: 'cli-output', text: data.toString() });
    // });

    // Handle messages from the webview
    panel.webview.onDidReceiveMessage((message: { type: string; text: string }) => {
      if (message.type === 'sendMessage') {
        // cliProcess.stdin.write(`${message.text}\n`);
        // Simulate response
        setTimeout(() => {
          panel.webview.postMessage({ type: 'cli-output', text: 'Simulated response: ' + message.text });
        }, 500);
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