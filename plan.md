# Manorrock Assistant Test Plan for CLI with TestContainers

## Overview

This document outlines a comprehensive plan for testing the Manorrock Assistant CLI using TestContainers and Docker. The approach will create a sandboxed environment to test CLI commands, tools, prompt interactions with LLMs, and the upcoming agentic framework.

## Design Goals

1. Create isolated testing environments using Docker containers
2. Enable reproducible testing of CLI interactions across different environments
3. Test interactions with various LLM backends (Ollama, OpenAI, Azure OpenAI)
4. Validate tool execution in sandboxed environments
5. Test and validate the agentic framework's behavior
6. Capture and analyze prompt/response pairs
7. Support regression testing and continuous integration

## Implementation Overview

The testing framework will use TestContainers to:
1. Launch Docker containers with specific configurations
2. Execute CLI commands within these containers
3. Capture outputs, logs, and state changes
4. Validate behaviors against expected outcomes
5. Record interactions for audit and improvement

## Technologies

- **TestContainers**: Java library for Docker container-based testing
- **Docker**: Containerization platform for creating isolated environments
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework for simulating LLM responses
- **Wiremock**: For mocking API responses from LLMs
- **AssertJ**: Fluent assertions library
- **Slf4j/Logback**: Logging

## Detailed Implementation Plan

### Phase 1: Core Testing Infrastructure

1. **TestContainers Setup**
   - Create a BaseContainerTest class
   - Define container configurations for different test scenarios
   - Set up volume mounting for logs and artifacts

2. **Test Fixtures**
   - Develop fixtures for common CLI commands
   - Create test data generators for prompts
   - Set up mock LLM response generators

3. **Output Capture and Validation**
   - Implement CLI output capture
   - Create assertion utilities specific to Assistant output
   - Develop prompt/response capture mechanisms

### Phase 2: CLI Command Testing

1. **Basic CLI Commands**
   - Test help command
   - Test tool listing
   - Test configuration commands

2. **Tool Command Testing**
   - Test tool execution within container
   - Validate tool discovery
   - Test tool parameter validation

3. **LLM Configuration Testing**
   - Test LLM command with various backends
   - Validate configuration persistence
   - Test configuration error handling

### Phase 3: LLM Integration Testing

1. **Mock LLM Backends**
   - Create Docker images with mock LLM services
   - Implement Ollama mock container
   - Configure response templates for scenarios

2. **Prompt/Response Testing**
   - Test prompt formatting
   - Validate response parsing
   - Test conversation context maintenance

3. **Error Case Testing**
   - Test network failures
   - Test malformed responses
   - Test timeouts and retries

### Phase 4: Tool Integration Testing

1. **File System Tools**
   - Test read/write operations in container
   - Validate permission handling
   - Test path resolution

2. **Process Execution Tools**
   - Test command execution
   - Validate output capture
   - Test environment variable handling

3. **Network Tools**
   - Test connectivity
   - Validate URL handling
   - Test API interactions

### Phase 5: Agentic Framework Testing

1. **Agent Creation**
   - Test agent initialization
   - Validate configuration
   - Test lifecycle management

2. **Tool Selection**
   - Test tool discovery by agents
   - Validate appropriate tool selection
   - Test parameter mapping

3. **Task Execution**
   - Test task planning
   - Validate subtask creation
   - Test execution monitoring

4. **Recovery and Error Handling**
   - Test agent recovery from failures
   - Validate retry strategies
   - Test graceful degradation

### Phase 6: Comprehensive Scenario Testing

1. **End-to-End Flows**
   - Project analysis scenarios
   - Code generation scenarios
   - Q&A session scenarios

2. **Multi-step Operations**
   - Test complex task sequences
   - Validate state maintenance
   - Test context carrying

3. **Performance and Stress Testing**
   - Test with large prompts
   - Validate memory management
   - Test concurrent operations

## Test Container Types

1. **Basic CLI Container**
   - Java runtime
   - CLI dependencies
   - No external services

2. **Ollama Integration Container**
   - Java runtime
   - CLI dependencies
   - Ollama service with test models

3. **Development Environment Container**
   - Java runtime
   - CLI dependencies
   - Development tools (git, build tools)
   - Sample project structures

4. **MultiAgent Container**
   - Java runtime
   - CLI dependencies
   - Multiple agent configurations
   - Inter-agent communication setup

## Testing Matrix

| Test Type | Container | Scenario | Success Criteria |
|-----------|-----------|----------|-----------------|
| CLI Basic | Basic CLI | Help command | Correct output |
| CLI Config | Basic CLI | LLM config | Config persisted |
| Tool Execution | Basic CLI | File operations | Expected results |
| LLM Integration | Ollama | Simple prompt | Valid response |
| Agent | MultiAgent | Task planning | Correct plan generation |
| E2E | Development | Project analysis | Accurate analysis |

## Requirements

### Functional Requirements

1. **CLI Command Testing**
   - All CLI commands must execute successfully in container
   - Command output must match expected patterns
   - Error handling must provide useful messages

2. **Tool Integration**
   - Tools must function in containerized environment
   - File system interactions must be sandboxed
   - Process execution must be secured

3. **LLM Integration**
   - Support for multiple LLM backends
   - Proper handling of API keys and endpoints
   - Graceful handling of LLM failures

4. **Agent Framework**
   - Agent lifecycle management
   - Tool selection and execution
   - Task planning and tracking

### Non-Functional Requirements

1. **Performance**
   - Test execution time under 10 minutes
   - Resource usage within container limits
   - Graceful handling of resource constraints

2. **Security**
   - No exposure of sensitive data
   - Proper isolation of test environments
   - Secure handling of API credentials

3. **Maintainability**
   - Well-documented test cases
   - Reusable test fixtures
   - Clear failure reports

## Exit Criteria

1. All test cases pass in CI environment
2. Code coverage exceeds 80% for critical paths
3. Performance benchmarks meet targets
4. All supported platforms validated
5. Security review completed

## Progress Tracking Sheet

| Phase | Component | Status | Owner | Completion | Issues |
|-------|-----------|--------|-------|------------|--------|
| 1 | BaseContainerTest | Not Started | | 0% | |
| 1 | Test Fixtures | Not Started | | 0% | |
| 1 | Output Capture | Not Started | | 0% | |
| 2 | Basic CLI Commands | Not Started | | 0% | |
| 2 | Tool Commands | Not Started | | 0% | |
| 2 | LLM Config | Not Started | | 0% | |
| 3 | Mock LLM Backends | Not Started | | 0% | |
| 3 | Prompt/Response | Not Started | | 0% | |
| 3 | Error Cases | Not Started | | 0% | |
| 4 | File System Tools | Not Started | | 0% | |
| 4 | Process Execution | Not Started | | 0% | |
| 4 | Network Tools | Not Started | | 0% | |
| 5 | Agent Creation | Not Started | | 0% | |
| 5 | Tool Selection | Not Started | | 0% | |
| 5 | Task Execution | Not Started | | 0% | |
| 5 | Recovery | Not Started | | 0% | |
| 6 | E2E Flows | Not Started | | 0% | |
| 6 | Multi-step Ops | Not Started | | 0% | |
| 6 | Performance | Not Started | | 0% | |

## Grading Scale

### Implementation Completeness
1. **A (90-100%)**: All planned test components implemented, high code coverage
2. **B (80-89%)**: Most test components implemented, good code coverage
3. **C (70-79%)**: Core test components implemented, acceptable coverage
4. **D (60-69%)**: Basic test structure implemented, limited coverage
5. **F (0-59%)**: Incomplete implementation, major gaps

### Test Effectiveness
1. **A (90-100%)**: Tests catch all known issues, good edge case coverage
2. **B (80-89%)**: Tests catch most issues, some edge cases covered
3. **C (70-79%)**: Tests catch common issues, few edge cases
4. **D (60-69%)**: Tests catch obvious issues only
5. **F (0-59%)**: Tests miss significant issues

### Documentation
1. **A (90-100%)**: Comprehensive, clear, maintainable docs
2. **B (80-89%)**: Good documentation, minor gaps
3. **C (70-79%)**: Basic documentation, some unclear areas
4. **D (60-69%)**: Minimal documentation
5. **F (0-59%)**: Poor or missing documentation

## Implementation Details

### TestContainers Configuration

```java
@Testcontainers
public class CliContainerTest {
    @Container
    public static GenericContainer<?> cliContainer = new GenericContainer<>("manorrock-assistant-cli:test")
        .withCommand("sleep infinity")
        .withFileSystemBind("./test-fixtures", "/fixtures", BindMode.READ_ONLY)
        .withFileSystemBind("./test-output", "/output", BindMode.READ_WRITE);
        
    @Test
    void testBasicCliCommand() {
        ExecResult result = cliContainer.execInContainer("assistant-cli", "--help");
        assertThat(result.getStdout()).contains("Usage: assistant-cli");
    }
}
```

### Mock LLM Server

```java
@Testcontainers
public class LlmIntegrationTest {
    @Container
    private static final GenericContainer<?> mockLlmServer = new GenericContainer<>("wiremock/wiremock")
        .withExposedPorts(8080)
        .withFileSystemBind("./mock-responses", "/home/wiremock", BindMode.READ_WRITE);
        
    @Container
    public static GenericContainer<?> cliContainer = new GenericContainer<>("manorrock-assistant-cli:test")
        .withCommand("sleep infinity")
        .withEnv("LLM_ENDPOINT", "http://mockLlm:8080")
        .withNetwork(Network.SHARED);
}
```

### Agent Testing

```java
public class AgentTest {
    @Test
    void testAgentToolSelection() {
        // Set up agent with task
        ExecResult result = cliContainer.execInContainer(
            "assistant-cli", 
            "--agent", "code-analyzer", 
            "--task", "analyze this project structure"
        );
        
        // Verify tool selection
        assertThat(result.getStdout()).contains("Using tool: project_structure_analysis");
    }
}
```

## Next Steps

1. Set up testing repository structure
2. Create Docker images for testing environments
3. Implement base TestContainers configuration
4. Develop first basic CLI tests
5. Create mock LLM server
6. Implement test fixtures for common scenarios