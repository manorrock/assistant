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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.deactivate = exports.activate = void 0;
const vscode = __importStar(require("vscode")); // Add this import statement
const child_process_1 = require("child_process");
const os = __importStar(require("os"));
const path = __importStar(require("path"));
function activate(context) {
    console.log('Activating Manorrock Assistant extension'); // Add logging
    const provider = new AssistantViewProvider(context);
    context.subscriptions.push(vscode.window.registerWebviewViewProvider('assistantView', provider, {
        webviewOptions: { retainContextWhenHidden: true }
    }) // Ensure view ID matches
    );
    console.log('Webview provider registered'); // Add logging
    context.subscriptions.push(vscode.commands.registerCommand('assistant.show', () => __awaiter(this, void 0, void 0, function* () {
        yield vscode.commands.executeCommand('workbench.view.extension.assistantView');
        vscode.commands.executeCommand('vscode.moveViews', {
            viewIds: ['assistantView'],
            destinationId: 'workbench.view.extension.secondarySideBar',
            position: 'right'
        });
    })));
    vscode.commands.executeCommand('setContext', 'assistantView', true);
}
exports.activate = activate;
class AssistantViewProvider {
    constructor(context) {
        this.context = context;
    }
    resolveWebviewView(webviewView) {
        console.log('Resolving Webview View'); // Add logging
        webviewView.webview.options = { enableScripts: true };
        webviewView.webview.html = this.getWebviewContent();
        const outputChannel = vscode.window.createOutputChannel('Manorrock Assistant');
        const config = vscode.workspace.getConfiguration('assistant');
        let cliPath = config.get('cliPath') || path.join(os.homedir(), '.manorrock', 'assistant', 'cli.jar');
        if (cliPath.startsWith('~') || cliPath.startsWith('%USERPROFILE%')) {
            cliPath = path.join(os.homedir(), cliPath.slice(cliPath.indexOf(path.sep) + 1));
        }
        try {
            outputChannel.appendLine(`Spawning process with CLI path: ${cliPath} for /help request`);
            const cliProcess = (0, child_process_1.spawn)('java', ['-jar', cliPath, '--stdin']);
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
                const errorMessage = error.message;
                outputChannel.appendLine(`Error: ${errorMessage}`);
                webviewView.webview.postMessage({ type: 'cli-output', text: `Error: ${errorMessage}` });
            });
        }
        catch (error) {
            const errorMessage = error.message;
            outputChannel.appendLine(`Exception: ${errorMessage}`);
            webviewView.webview.postMessage({ type: 'cli-output', text: `Exception: ${errorMessage}` });
        }
        webviewView.webview.onDidReceiveMessage((message) => {
            if (message.type === 'sendMessage') {
                try {
                    outputChannel.appendLine(`Spawning process with CLI path: ${cliPath}`);
                    outputChannel.appendLine(`Input sent to process: ${message.text}`);
                    const cliProcess = (0, child_process_1.spawn)('java', ['-jar', cliPath, '--stdin']);
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
                        const errorMessage = error.message;
                        outputChannel.appendLine(`Error: ${errorMessage}`);
                        webviewView.webview.postMessage({ type: 'cli-output', text: `Error: ${errorMessage}` });
                    });
                }
                catch (error) {
                    const errorMessage = error.message;
                    outputChannel.appendLine(`Exception: ${errorMessage}`);
                    webviewView.webview.postMessage({ type: 'cli-output', text: `Exception: ${errorMessage}` });
                }
            }
        });
    }
    getWebviewContent() {
        const config = vscode.workspace.getConfiguration('assistant');
        const theme = config.get('theme') || 'default';
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
function deactivate() { }
exports.deactivate = deactivate;
