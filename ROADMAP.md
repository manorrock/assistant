# Manorrock Assistant Roadmap

> **Disclaimer:** This roadmap was generated with assistance from an LLM model. The content is subject to change without notice and is provided "as is" without warranty of any kind, either expressed or implied, including fitness for a particular purpose. The roadmap represents potential development directions that may evolve based on project needs, technological advancements, and feedback from the community.

# Next release

- [x] Add --no-banner to CLI
- [x] Remove deprecated methods from Command
- [x] Fix release workflow
- [ ] Refactor NetBeans plugin to use the CoreAssistant directly
- [ ] Refactor Eclipse plugin to use the CoreAssistant directly
- [ ] Refactor IntelliJ plugin to use the CoreAssistant directly
- [ ] Rework VSCode extension UI to make title not fixed, but rather part of the
      response area
- [ ] Refactor to move VSCode extension into `vscode/extension` directory
- [ ] Add VSCode CLI to `vscode/cli` directory
- [ ] Add /llm reset which resets the LLM and the memory

# Next+1 release

- [ ] Add LICENSE to VSCode extension

# Backlog

## Uncategorized items

- [ ] Add /llm reset model which resets the LLM model with updated properties
- [ ] Add /llm reset memory which resets the memory
- [ ] Update VSCode extension to pretty print code blocks
- [ ] Add configuration persistence
- [ ] Implement history persistence
- [ ] Add memory window controls

## Conversation Support
- [ ] Implement ConversationContext interface
- [ ] Add callback support for file attachment requests
- [ ] Add permission request/response system
- [ ] Add conversation timeout handling
- [ ] Add conversation metadata support (tags, categories)
- [ ] Add support for UI prompts and dialogs via callback
- [ ] Add conversation support in CoreAssistant with message history
- [ ] Add conversation pause/resume capability for async operations
- [ ] Add support for conversation branching and merging
- [ ] Add support for conversation context persistence
- [ ] Add support for conversation recovery after errors
- [ ] Add support for parallel conversations
- [ ] Add conversation state validation system
- [ ] Add conversation event system for monitoring

## Command Enhancements
- [ ] Add conversation awareness to Command interface
- [ ] Add command state persistence
- [ ] Add command execution pause/resume
- [ ] Add command cancellation support
- [ ] Add command progress reporting
- [ ] Add command timeout handling
- [ ] Add command retry policies
- [ ] Add command execution history

## LLM Enhancements
- [ ] Add conversation context support in CoreLlm
- [ ] Add support for conversation state in LLM prompts
- [ ] Add conversation memory management
- [ ] Add support for LLM context windowing
- [ ] Add support for dynamic temperature adjustment
- [ ] Add support for streaming responses
- [ ] Add support for response validation
- [ ] Add support for conversation summarization

## Tool System Improvements
- [ ] Add JSON schema validation for tool responses
- [ ] Add tool parameter default values
- [ ] Add tool result caching
- [ ] Add basic tool result transformation
- [ ] Add tool registry state validation
- [ ] Add tool execution metrics tracking
- [ ] Add tool cleanup on shutdown
- [ ] Add tool initialization status tracking
- [ ] Add tool execution timeout handling
- [ ] Add tool error recovery mechanism
- [ ] Add tool dependency validation
- [ ] Add basic tool metrics collection
- [ ] Add tool discovery and dynamic loading
- [ ] Add complex parameter type support (arrays, maps)

## Command System Enhancements
- [ ] Add input sanitization for command parameters
- [ ] Add command parameter type validation
- [ ] Add command alias support
- [ ] Add command execution error handling
- [ ] Add rate limiting for message processing
- [ ] Add basic message queue for async processing

## Configuration and State Management
- [ ] Add configuration file support (.manorrock/assistant/config.json)
- [ ] Add property validation in CoreLlm.setProperties
- [ ] Add model initialization error handling
- [ ] Add chat memory persistence
- [ ] Add basic retry mechanism for LLM calls
- [ ] Add response validation in CoreLlm
- [ ] Add message processing timeout
- [ ] Add basic message validation
- [ ] Add rate limiting for tool execution
- [ ] Add conversation state persistence
- [ ] Add conversation migration support
- [ ] Add conversation backup/restore
- [ ] Add conversation archiving
- [ ] Add conversation import/export
- [ ] Add conversation cleanup policies

## Logging and Monitoring
- [ ] Add basic logging for tool registration and execution
- [ ] Add basic logging for tool execution and errors
- [ ] Add tool execution metrics collection
- [ ] Add tool parameter validation helpers
- [ ] Add LLM manager state validation
- [ ] Add circular dependency detection for tools
- [ ] Add conversation event logging
- [ ] Add conversation performance metrics
- [ ] Add conversation error tracking
- [ ] Add conversation analytics
- [ ] Add conversation health monitoring

## External Integrations
- [ ] Add conversation export to third-party platforms
- [ ] Add conversation import from third-party platforms
- [ ] Add multi-channel conversation support
- [ ] Add Spring Boot REST API application
- [ ] Add web interface
- [ ] Implement MCP server functionality
- [ ] Create mobile applications (iPhone, Android)
- [ ] Create chat platform integrations (Discord, Slack, Teams)
- [ ] Add WebSearchTool (multiple providers)
- [ ] Add DatabaseTool
- [ ] Add WorkflowTool
- [ ] Add MCPTool
- [ ] Add GitHubTool
- [ ] Create tablet application (iPad, Android tablet)