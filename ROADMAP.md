# Manorrock Assistant Roadmap

> **Disclaimer:** This roadmap was generated with assistance from an LLM model. The content is subject to change without notice and is provided "as is" without warranty of any kind, either expressed or implied, including fitness for a particular purpose. The roadmap represents potential development directions that may evolve based on project needs, technological advancements, and feedback from the community.

# Next release
- [x] Refactor mobile directory to phone directory (Phone)
- [ ] Add VSCodeCLI (VSCode)
- [ ] Include VSCodeCLI in extension (VSCode)
- [ ] Add arm64 macOS CLI binary (CLI)
- [ ] Add amd64 macOS CLI binary (CLI)
- [ ] Optimize extensin download size (VSCode)
- [x] Add LICENSE to plugin (NetBeans)
- [x] Implement token usage tracking and analytics (Core)

# Backlog

## Core
- [ ] Implement streaming response support for real-time token generation (Core)
- [ ] Add memory/chat history with token management for context windows (Core)
- [ ] Implement RAG (Retrieval Augmented Generation) capabilities for local documents (Core)
- [ ] Implement model switching/fallback mechanism for reliability (Core)
- [ ] Create structured output parsers for JSON/YAML/XML responses (Core)
- [ ] Add content moderation for inputs and outputs (Core)
- [ ] Implement error handling and graceful degradation for LLM failures (Core)
- [ ] Develop prompt templates system for consistent interactions (Core)
- [ ] Add support for Claude and other LLM providers (Core)
- [ ] Create evaluation framework for comparing model outputs (Core)
- [ ] Implement agent framework with planning capabilities (Core)

## CLI
- [ ] Add amd64 Windows CLI binary (CLI)
- [ ] Add amd64 Linux CLI binary (CLI)
- [ ] Add command history and recall functionality in interactive mode (CLI)
- [ ] Add file completion support for file-based commands (CLI)
- [ ] Implement configuration command for managing assistant settings (CLI)
- [ ] Add conversation export functionality to text or JSON format (CLI)
- [ ] Support multiple personalities/profiles via command line flags (CLI)
- [ ] Implement plugin system for adding custom commands (CLI)
- [ ] Add chat session persistence between CLI invocations (CLI)
- [ ] Implement markdown rendering in terminal output (CLI)

## Desktop
- [ ] Add dark mode support with theme toggle (Desktop)
- [ ] Implement proper markdown rendering for responses (Desktop)
- [ ] Add conversation export to PDF/HTML/text formats (Desktop)
- [ ] Implement syntax highlighting for code snippets (Desktop)
- [ ] Add file drag-and-drop support for document analysis (Desktop)
- [ ] Create settings panel for LLM configuration (Desktop)
- [ ] Add conversation history browser with search (Desktop)
- [ ] Implement keyboard shortcuts for common actions (Desktop)
- [ ] Add support for image input and analysis (Desktop)
- [ ] Implement split view mode for side-by-side content comparison (Desktop)
- [ ] Create Linux installer package (Desktop)

## Eclipse
- [ ] Add code completion suggestions based on context and selection (Eclipse)
- [ ] Implement syntax-aware code generation for Java files (Eclipse)
- [ ] Add bug detection and fix suggestions for selected code (Eclipse)
- [ ] Implement document refactoring capabilities with preview (Eclipse)
- [ ] Add unit test generation for selected classes/methods (Eclipse)
- [ ] Implement Javadoc generation and enhancement (Eclipse)
- [ ] Add conversation history persistence between Eclipse sessions (Eclipse)
- [ ] Implement proper markdown rendering in response area (Eclipse)
- [ ] Add code snippet insertion directly into editor (Eclipse)
- [ ] Implement settings page for plugin configuration (Eclipse)
- [ ] Add support for explanation of compiler/build errors (Eclipse)

## IntelliJ
- [ ] Implement proper markdown rendering for chat responses (IntelliJ)
- [ ] Add code completion suggestions based on selection context (IntelliJ)
- [ ] Implement code generation for Kotlin and Java files (IntelliJ)
- [ ] Add automatic bug detection and fix suggestions (IntelliJ)
- [ ] Implement code refactoring capability with previews (IntelliJ)
- [ ] Add conversation history persistence and search (IntelliJ)
- [ ] Create settings UI for plugin and LLM configuration (IntelliJ)
- [ ] Add support for image input and analysis in tool window (IntelliJ)
- [ ] Implement unit test generation for selected classes (IntelliJ)
- [ ] Add drag-and-drop file support for document analysis (IntelliJ)
- [ ] Implement inline code actions for AI assistance (IntelliJ)

## NetBeans
- [ ] Implement proper markdown rendering for assistant responses (NetBeans)
- [ ] Add code completion suggestions based on context (NetBeans)
- [ ] Implement syntax-aware code generation for Java files (NetBeans)
- [ ] Add conversation history persistence and search (NetBeans)
- [ ] Create configuration panel for LLM settings (NetBeans)
- [ ] Implement unit test generation for selected classes (NetBeans)
- [ ] Add bug detection and fix suggestions for selected code (NetBeans)
- [ ] Implement Javadoc generation and enhancement (NetBeans)
- [ ] Add support for explaining build errors and warnings (NetBeans)
- [ ] Implement custom themes for assistant window (NetBeans)
- [ ] Add keyboard shortcut customization for assistant commands (NetBeans)

## VSCode
- [ ] Implement WebView-based markdown rendering with syntax highlighting (VSCode)
- [ ] Add conversation history persistence with search functionality (VSCode)
- [ ] Integrate with VSCode's existing chat interface (VSCode)
- [ ] Implement code snippet insertion directly from suggestions (VSCode)
- [ ] Add support for code refactoring with previews (VSCode)
- [ ] Create settings UI for LLM configuration (VSCode)
- [ ] Implement inline code actions in editor (VSCode)
- [ ] Add support for multi-file context in prompts (VSCode)
- [ ] Implement language-specific code generation (VSCode)
- [ ] Add integration with workspace symbol search (VSCode)
- [ ] Implement workspace indexing for better context awareness (VSCode)

## Phone
- [ ] Create native iOS assistant application (Phone)
- [ ] Implement native Android assistant application (Phone)
- [ ] Add voice input for mobile interaction (Phone)
- [ ] Implement local model support for offline use (Phone)
- [ ] Add conversation history sync across devices (Phone)
- [ ] Create widget for quick access on mobile devices (Phone)
- [ ] Implement share extension for processing content from other apps (Phone)
- [ ] Add support for camera input for document analysis (Phone)
- [ ] Implement battery-efficient background processing (Phone)
- [ ] Add biometric authentication for secure conversations (Phone)
