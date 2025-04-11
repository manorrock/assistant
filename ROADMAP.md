# Manorrock Assistant Roadmap

> **Disclaimer:** This roadmap was generated with assistance from an LLM model. The content is subject to change without notice and is provided "as is" without warranty of any kind, either expressed or implied, including fitness for a particular purpose. The roadmap represents potential development directions that may evolve based on project needs, technological advancements, and feedback from the community.

# Next release
- [x] Implement proper markdown rendering for responses (Desktop)
- [x] Add support for explanation of compiler/build errors (Eclipse)

# Backlog

## CLI
- [ ] Add arm64 macOS CLI binary
- [ ] Add amd64 macOS CLI binary
- [ ] Add amd64 Windows CLI binary
- [ ] Add amd64 Linux CLI binary
- [ ] Add command history and recall functionality in interactive mode
- [ ] Add file completion support for file-based commands
- [ ] Implement configuration command for managing assistant settings
- [ ] Support multiple personalities/profiles via command line flags
- [ ] Implement plugin system for adding custom commands
- [ ] Add chat session persistence between CLI invocations

## Core
- [ ] Add memory/chat history with token management for context windows
- [ ] Implement RAG (Retrieval Augmented Generation) capabilities for local documents
- [ ] Implement model switching/fallback mechanism for reliability
- [ ] Create structured output parsers for JSON/YAML/XML responses
- [ ] Add content moderation for inputs and outputs
- [ ] Implement error handling and graceful degradation for LLM failures
- [ ] Develop prompt templates system for consistent interactions
- [ ] Add support for Claude and other LLM providers
- [ ] Create evaluation framework for comparing model outputs
- [ ] Implement agent framework with planning capabilities

## Desktop
- [ ] Add conversation export to PDF/HTML/text formats
- [ ] Implement syntax highlighting for code snippets
- [ ] Add file drag-and-drop support for document analysis
- [ ] Create settings panel for LLM configuration
- [ ] Add conversation history browser with search
- [ ] Implement keyboard shortcuts for common actions
- [ ] Add support for image input and analysis
- [ ] Implement split view mode for side-by-side content comparison
- [ ] Create Linux installer package

## Eclipse
- [ ] Implement syntax-aware code generation for Java files
- [ ] Add bug detection and fix suggestions for selected code
- [ ] Implement document refactoring capabilities with preview
- [ ] Add unit test generation for selected classes/methods
- [ ] Implement Javadoc generation and enhancement
- [ ] Add conversation history persistence between Eclipse sessions
- [ ] Implement proper markdown rendering in response area
- [ ] Add code snippet insertion directly into editor
- [ ] Implement settings page for plugin configuration

## IntelliJ
- [ ] Implement proper markdown rendering for chat responses
- [ ] Implement code generation for Kotlin and Java files
- [ ] Add automatic bug detection and fix suggestions
- [ ] Implement code refactoring capability with previews
- [ ] Add conversation history persistence and search
- [ ] Create settings UI for plugin and LLM configuration
- [ ] Add support for image input and analysis in tool window
- [ ] Implement unit test generation for selected classes
- [ ] Add drag-and-drop file support for document analysis
- [ ] Implement inline code actions for AI assistance

## NetBeans
- [ ] Implement proper markdown rendering for assistant responses
- [ ] Add code completion suggestions based on context
- [ ] Implement syntax-aware code generation for Java files
- [ ] Add conversation history persistence and search
- [ ] Create configuration panel for LLM settings
- [ ] Implement unit test generation for selected classes
- [ ] Add bug detection and fix suggestions for selected code
- [ ] Implement Javadoc generation and enhancement
- [ ] Add support for explaining build errors and warnings
- [ ] Implement custom themes for assistant window
- [ ] Add keyboard shortcut customization for assistant commands

## Phone
- [ ] Create native iOS assistant application
- [ ] Implement native Android assistant application
- [ ] Add voice input for mobile interaction
- [ ] Implement local model support for offline use
- [ ] Add conversation history sync across devices
- [ ] Create widget for quick access on mobile devices
- [ ] Implement share extension for processing content from other apps
- [ ] Add support for camera input for document analysis
- [ ] Implement battery-efficient background processing
- [ ] Add biometric authentication for secure conversations

## VSCode
- [ ] Add VSCode CLI
- [ ] Include VSCode CLI in extension
- [ ] Implement WebView-based markdown rendering with syntax highlighting
- [ ] Add conversation history persistence with search functionality
- [ ] Integrate with VSCode's existing chat interface
- [ ] Implement code snippet insertion directly from suggestions
- [ ] Add support for code refactoring with previews
- [ ] Create settings UI for LLM configuration
- [ ] Implement inline code actions in editor
- [ ] Add support for multi-file context in prompts
- [ ] Implement language-specific code generation
- [ ] Add integration with workspace symbol search
- [ ] Implement workspace indexing for better context awareness
