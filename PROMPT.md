# Prompt

You are a detail-oriented AI assistant helping with the Manorrock Assistant project. Analyze the task description below and generate a detailed task file and store it in task-{ID}.md using the following structured approach:

# Task: {ID} - {DESCRIPTION}

## Analysis Requirements
1. Project Structure:
   - Maven multi-module project (root)
   - IntelliJ plugin (intellij/) - uses Gradle build system with Kotlin
   - Eclipse plugin (eclipse/) - uses Maven and Java
   - NetBeans plugin (netbeans/) - uses Maven and Java
   - Desktop implementation (desktop/) - uses Maven and Java/JavaFX
   - Shared module (shared/) - contains common code and APIs used by all modules
   - CLI implementation (cli/) - uses Maven and Java
   - VSCode extension (vscode/) - uses TypeScript/JavaScript
   - Command module (command/) - contains non-UI command implementations used across modules

2. Component Analysis:
   - Identify affected modules in multi-module project
   - Analyze impacts on each plugin/implementation
   - List all files that need modification (with validated paths)
   - Document shared functionality requirements
   - Specify platform-specific variations
   - Define interface requirements
   - Document common test scenarios
   - Identify cross-platform command patterns for consistency
   - Analyze existing implementations of similar features across platforms
   - Identify module dependencies and update requirements

3. Implementation Requirements:
   - File-by-file changes with validated paths relative to project root
   - Cross-implementation consistency requirements
   - Command syntax standardization
   - Interface specifications
   - Shared code requirements
   - Platform-specific adaptations
   - Consider editor-specific APIs when implementing IDE features
   - Handle platform-specific build system requirements (Maven vs Gradle)
   - Update Maven/Gradle dependencies as needed for module changes

4. Documentation Requirements:
   - Update README.md where applicable
   - Document module structure changes when refactoring

5. Testing Requirements:
   - Do NOT include test cases in the task file

6. Task Management:
   - Remove task if fully completed task from ROADMAP.md

7. Implementation Plan:
   - Step-by-step breakdown
      - 4 levels of detail
      - Checklist format
   - Include all necessary steps for implementation
   - Review existing implementations (prioritize similar features in other platforms)
   - Document interface requirements
   - Include validation steps
   - Utilize and update existing code if available
   - Note platform-specific considerations (e.g., Kotlin vs Java, different APIs)
   - Address build system considerations (e.g., Maven vs Gradle)
   - Include package refactoring steps when moving code between modules

## Handling Removal Tasks

When implementing tasks that involve removing components:

1. Document all affected files and configurations
2. Verify no remaining references exist after removal
3. Ensure builds succeed after component removal
4. Update documentation to remove references to removed components
5. Validate CI/CD pipeline continues to work

## Command Implementation Guidelines

When implementing commands across different platforms:
1. Maintain consistent command syntax across all implementations
2. Ensure similar behavior for equivalent commands on different platforms
3. Reuse shared code where possible
4. Account for platform-specific editor APIs for text manipulation
5. Consider different programming languages (Java vs Kotlin) when adapting implementations
6. Follow standard prompting patterns for text processing commands
7. Implement appropriate error handling for each platform

## Cross-Platform Feature Implementation

When implementing features that exist in multiple IDE plugins:
1. Study existing implementations in other platforms first
2. Maintain consistent UI/UX patterns across platforms
3. Consider platform constraints and capabilities
4. Follow platform-specific best practices for editor interaction
5. Use appropriate error handling for each platform
6. Ensure command help text is consistent across platforms
7. Handle platform-specific build issues proactively

## Build System Considerations

When implementing changes that affect build processes:
1. Consider the different build systems (Maven for most modules, Gradle for IntelliJ)
2. Validate inter-module dependencies work across build systems
3. Test builds in isolation before integration
4. Use appropriate platform-specific build commands and configurations
5. Handle dependency resolution carefully between modules
6. Update module dependencies when refactoring code across modules

## Module Refactoring Guidelines

When moving code between modules:
1. Analyze all dependencies before moving code
2. Update package declarations consistently
3. Modify import statements in all affected files
4. Update build configuration files (pom.xml, build.gradle.kts)
5. Maintain backward compatibility where possible
6. Validate builds after each significant change
7. Consider transitive dependencies and their impacts

All file paths MUST:
- Exist in the project
- Be relative to the project root
- Be validated before inclusion

## After completing the task 
- You MUST update the PROMPT.md file with additional guidance that would help for future tasks 
- You MUST integrated the additional guidance in a concise manner and NOT just append it at the end
- You MUST remove any guidance that is no longer relevant
- You MUST ensure that the guidance is logically and methodically incorporated into the PROMPT.md file
- You MUST ensure that the guidance is clear and easy to understand
- You MUST ensure that the guidance is consistent with the overall structure and tone of the document
- You MUST ensure that the guidance is applicable to ALL future tasks

## Task Details

Task ID: {ID}
Task Description: {DESCRIPTION}
