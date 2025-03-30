# Manorrock Assistant Roadmap

> **Disclaimer:** This roadmap was generated with assistance from an LLM model. The content is subject to change without notice and is provided "as is" without warranty of any kind, either expressed or implied, including fitness for a particular purpose. The roadmap represents potential development directions that may evolve based on project needs, technological advancements, and feedback from the community.

# Next release
- [x] Add unit test for commands
- [x] Refactor to remove ToolExecutionException
- [x] Add /clear command to VSCode extension
- [x] Introduce a core module to make reuse of the assistant easier
- [x] Add /help command to CoreAssistant
- [x] Add /explain command to CoreAssistant
- [x] Add /source command to CoreAssistant
- [x] Add /ollama command to CoreAssistant
- [x] Add /session command to CoreAssistant (wit new sub command)
- [x] Add /tool command to CoreAssistant

# Backlog
- [ ] Add /session save/restor sub commands
- [ ] Add /session autosave sub command
- [ ] Route old CLI implementation through --old and deprecate it
- [ ] Cleanup NetBeans plugin
- [ ] Add /agent command (with list)
- [ ] Add /agent active sub command
- [ ] Add /agent deactivate sub command
- [ ] Refactor all code in CLI except for the main and call method into a separate class called          
      CLIAssistant with setters and getters for instance variables and make the CLI call method
      call the process method of the CLIAssistant class. 
- [ ] Refactor to push up input and output handling all the way to the CLIProcessor process method
- [ ] Refactor to get rid of the com.manorrock.assistant.core package altogether
- [ ] Create a `file_create` tool with the following requirements:
  - It must be able to create a new file or directory at the specified path
  - The path can include nested directories, and the tool should be able to create all necessary parent    
    directories if they do not exist
  - If a file already exists at the specified path, the tool should overwrite if requested
  - The tool should return an object with a "status" property indicating whether the operation was 
    successful, along with any relevant error messages.
- [ ] Add unit tests for the shell_execution tool
   - Add various tests for the command parameter
   - Add various tests for the shell parameter
   - Add various tests for the working_directory parameter
   - Add various tests for the environmentVariables parameter
   - Add various tests for the timeoutSeconds parameter
- [ ] Add a /plan create command to create a structured plan for a given PROMPT
- [ ] Add a /plan execute command to execute a given plan
- [ ] Add command category system for organizing and grouping related commands
- [ ] Add a Spring Boot application to expose the Assistant as an MCP server
- [ ] Add WebSearchTool (DuckDuckGo)

# Next+2 release
- [ ] Add a DatabaseTool to connect to a database and execute SQL commands
- [ ] Add a Bing search tool (WebSearchTool)
- [ ] Add a WorkflowTool to create, manage and execute workflows using a simple DSL structured as JSON
- [ ] Update IntelliJ gradle build to use the version from the top-levvel pom.xml file
- [ ] Add /assistant command that will execute a prompt by:
      - Creating workflow
      - Executing the workflow
      - Returning the result of the workflow (prompt)
- [ ] Support the ability to select Yes or No for a given tool request invocation
- [ ] Refactor to rename impl module to remote and adjust the package names accordingly
- [ ] Add a MCPTool to access any MCP server and execute tools exposed by the server
- [ ] Refactor to rename shared module to impl and adjust the package names accordingly
- [ ] Add a job to the build workflow to remove the SNAPSHOT release 
      prior to running any of the platform specific jobs that will 
      upload their SNAPSHOT artifacts

# Next+3 release
- [ ] Add a JSON input / output mode to the CLI
- [ ] Refactor to rename mobile module to phone and adjust the package names accordingly
- [ ] Add Google support to WebSearchTool

# Backlog

- [ ] Create a CommandExecutionContext class to pass state and parameters to commands
- [ ] Incorporate https://github.com/stephanj/MCPJavaFileSystem as a tool
- [ ] Create a unified error code system for all command operations
- [ ] Add internationalization support for error messages
- [ ] Create response internationalization framework for message localization
- [ ] Implement response compression for network transfers
- [ ] Add response signature/verification for secure commands
- [ ] Create @Required validation annotation
- [ ] Create @Range validation annotation
- [ ] Create @Pattern validation annotation
- [ ] Create @Enumerated validation annotation
- [ ] Implement validation statistics collection
- [ ] Implement validation error pattern analysis
- [ ] Implement validation performance metrics
- [ ] Add configuration versioning and migration support for backward compatibility
- [ ] Add support for multiple configuration profiles (personal, work, etc.)
- [ ] Implement configuration performance monitoring
- [ ] Create message compression options
- [ ] Implement message encryption system
- [ ] Add message signing capabilities
- [ ] Implement state versioning and migration system
- [ ] Add state compression for storage optimization
- [ ] Implement state encryption for sensitive data
- [ ] Create state performance metrics collection
- [ ] Implement error reporting and analytics system
- [ ] Implement error translation system for internationalization
- [ ] Implement error rate monitoring and throttling
- [ ] Add error pattern detection for proactive issue resolution
- [ ] Remove unused code
- [ ] Change release workflow to only keep the last 3 release and the SNAPSHOT artifacts
- [ ] Refactor NetBeans plugin to include a simple Main class
      that will be used to run the plugin in a standalone mode
- [ ] Remove all tests that are not related to the /help command
- [ ] Remove smoke-tests workflow
- [ ] Create a Spring Boot REST API application using the CLI version
- [ ] Update Spring Boot application to add a web interface
- [ ] Update Spring Boot application to expose itself as an MCP server
- [ ] Add an iPhone application to interact with the Spring Boot application using the REST API
- [ ] Add an Android phone application to interact with the Spring Boot application using the REST API
- [ ] Create a Discord bot integration
- [ ] Create a Slack bot integration
- [ ] Create a Microsoft Teams bot integration
- [ ] Create a Telegram bot integration
- [ ] Create a WhatsApp bot integration
- [ ] Create a Matrix bot integration
- [ ] Implement custom template configuration system
- [ ] Create template user interface for managing templates
- [ ] Add template import/export functionality
- [ ] Implement template version control
- [ ] Create template sharing functionality
- [ ] Add template categories and tagging system
- [ ] Implement template validation system
- [ ] Design plugin architecture and API specification
- [ ] Implement plugin loading and lifecycle management
- [ ] Create plugin discovery mechanism
- [ ] Develop plugin dependency resolution system
- [ ] Add plugin configuration framework
- [ ] Create plugin marketplace/repository
- [ ] Implement plugin security sandboxing
- [ ] Add plugin version compatibility checking
- [ ] Create plugin development documentation
- [ ] Implement plugin update mechanism
- [ ] Create profile configuration data structure
- [ ] Implement profile switching mechanism
- [ ] Add profile-specific settings persistence
- [ ] Develop profile management CLI commands
- [ ] Create profile templates for common use cases
- [ ] Implement profile import/export functionality
- [ ] Add profile validation mechanism
- [ ] Create profile conflict resolution system
- [ ] Implement profile-based permission model
- [ ] Add profile versioning and backward compatibility
- [ ] Implement profile change auditing
- [ ] Create profile documentation generator
- [ ] Implement Google Search API integration
- [ ] Implement DuckDuckGo API integration
- [ ] Create unified search result interface
- [ ] Add search provider selection logic
- [ ] Implement search result caching
- [ ] Add search analytics tracking
- [ ] Implement search rate limiting
- [ ] Add search result deduplication
- [ ] Design vector database schema for knowledge storage
- [ ] Implement vector database connection management
- [ ] Create vector embedding generation service
- [ ] Add vector similarity search functionality
- [ ] Implement knowledge chunk storage and retrieval
- [ ] Create vector database indexing optimization
- [ ] Add vector database backup and recovery system
- [ ] Implement vector database monitoring and metrics
- [ ] Create vector database maintenance utilities
- [ ] Add vector database query optimization system
- [ ] Create base APIRequestTool interface and implementation
- [ ] Add support for HTTP methods (GET, POST, PUT, DELETE, PATCH)
- [ ] Implement request header management system
- [ ] Add request body handling for different content types
- [ ] Implement response parsing for common formats (JSON, XML, Text)
- [ ] Add API authentication support (Basic, Bearer, OAuth)
- [ ] Implement API rate limiting and throttling
- [ ] Create API request caching mechanism
- [ ] Add API request retry logic with backoff
- [ ] Implement API response validation
- [ ] Add API request logging and monitoring
- [ ] Create API documentation generator
- [ ] Implement API versioning support
- [ ] Add API request timeout handling
- [ ] Create API error handling and recovery system
- [ ] Implement WebScraperTool configuration options for authentication
- [ ] Add capability to render JavaScript in web scraping operations
- [ ] Create structured data extraction patterns for common websites
- [ ] Implement web content caching system for scraped pages
- [ ] Add intelligent throttling for web scraping operations
- [ ] Create web scraping session management
- [ ] Add proxy support for web browsing operations
- [ ] Implement headless browser integration for complex web interactions
- [ ] Add cookie and session state management for web browsing
- [ ] Create visual selector tool for defining scraping targets
- [ ] Implement site-specific scraping rule templates
- [ ] Add content transformation pipelines for scraped data
- [ ] Implement robots.txt compliance checking for ethical scraping
- [ ] Create scraping monitoring and analytics dashboard
- [ ] Add support for handling captchas during web scraping
- [ ] Deliver a GitHubTool for GitHub integration

3. Secondary Tools Integration
   - [ ] Structured Data Extraction

4. Tertiary Tools Integration
   - [ ] Database Connectors
   - [ ] Image Generation/Processing
   - [ ] Audio Processing
   - [ ] Calendar Integration
   - [ ] Email Client
   - [ ] Note-taking Systems
   - [ ] Task Management
   - [ ] Document Generation

5. Documentation and Testing
   - [ ] Security audit implementation

6. Utility Tools Development
   - [ ] JSON data transformation tool
   - [ ] XML data transformation tool
   - [ ] Schema validation tool
   - [ ] Data format validation tool

9. Testing Infrastructure
   - [ ] Concurrent execution testing

## Validation Criteria

1. Tool Integration QA
   - [ ] Verify plugin performance under load
   - [ ] Monitor tool usage patterns
   - [ ] Track performance metrics
   - [ ] Gather user feedback

## Additional Considerations

1. System Design
   - [ ] Implement tool usage observation system
   - [ ] Create tool selection explanation mechanism
   - [ ] Add rate limiting for API-based tools

2. Deliverables
   - [ ] Track tool selection accuracy metrics

- [ ] Remove PROMPT.md
- [ ] Remove PLAN_PROMPT.md
- [ ] Remove plan.md
