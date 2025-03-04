"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.deactivate = exports.activate = void 0;
const vscode = __importStar(require("vscode"));
function activate(context) {
    const disposable = vscode.commands.registerCommand('assistant.showPanel', () => {
        const panel = vscode.window.createWebviewPanel('assistantPanel', 'Assistant', vscode.ViewColumn.One, { enableScripts: true });
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
        panel.webview.onDidReceiveMessage((message) => {
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
exports.activate = activate;
function getWebviewContent() {
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
function deactivate() { }
exports.deactivate = deactivate;
