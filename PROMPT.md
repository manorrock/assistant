# Prompt

You are a detail-oriented AI assistant helping with the Manorrock Assistant project. Analyze the task description below and generate a detailed task file at task-{ID}.md using the following structured approach:

# Task: {ID} - {DESCRIPTION}

## Analysis Requirements
1. Project Structure:
   - Maven multi-module project (root)
   - IntelliJ plugin (gradle/)
   - Eclipse plugin (eclipse/)
   - CLI implementation (cli/)
   - VSCode extension (vscode/)

2. Component Analysis:
   - Identify affected modules in multi-module project
   - Analyze impacts on each plugin/implementation
   - List all files that need modification (with validated paths)
   - Document shared functionality requirements
   - Specify platform-specific variations
   - Define interface requirements
   - Document common test scenarios

3. Implementation Requirements:
   - File-by-file changes with validated paths relative to project root
   - Cross-implementation consistency requirements
   - Command syntax standardization
   - Interface specifications
   - Shared code requirements
   - Platform-specific adaptations

4. Documentation Requirements:
   - Update README.md where applicable

5. Testing Requirements:
   - Do NOT include test cases in the task file

6. Task Management:
   - Remove task if fully completed task from ROADMAP.md

7. Implementation Plan:
   - Step-by-step breakdown
      - 4 levels of detail
      - Checklist format
   - Include all necessary steps for implementation
   - Review existing implementations (prioritize CLI)
   - Document interface requirements
   - Include validation steps
   - Utilize and update existing code if available

## Handling Removal Tasks

When implementing tasks that involve removing components:

1. Document all affected files and configurations
2. Verify no remaining references exist after removal
3. Ensure builds succeed after component removal
4. Update documentation to remove references to removed components
5. Validate CI/CD pipeline continues to work

## Command Implementation Patterns

When implementing commands across different platforms:

1. Study the CLI implementation first as the reference
2. Maintain consistent behavior across all platforms
3. Follow platform-specific UI/UX patterns
4. Implement proper error handling and user feedback
5. For text-processing commands (like `/explain`):
   - Handle text selection appropriately
   - Provide clear feedback about what's being processed
   - Format output consistently

## VSCode-Specific Considerations

When working with the VSCode extension:

1. Commands should be implemented in the `onDidReceiveMessage` handler
2. Use the VSCode API for editor interaction (text selection, file paths)
3. Provide user feedback through the webview messaging system
4. Handle both cases: text selection and no selection
5. Follow the existing pattern of spawning CLI processes for command execution

All file paths MUST:
- Exist in the project
- Be relative to the project root
- Be validated before inclusion

After completing the task, you MUST to update the PROMPT.md file 
with additional guidance that would help for future tasks and
remove any guidance that is no longer relevant. Note that the
guidance should be logically and methodically be incorporated
into the PROMPT.md file and not just appended at the end.


