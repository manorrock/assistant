# Task Expansion and Implementation Prompt Template

## Overview
This template ensures consistent handling of tasks across all Manorrock Assistant implementations:
- Maven multi-module project (root)
- IntelliJ plugin (gradle directory)
- Eclipse plugin (eclipse directory)
- CLI implementation (cli directory)
- VSCode extension (vscode directory)

## Task Expansion Format

```markdown
# Task: [Task ID] - [Task Name]

## Original Task Description
[Copy the original task description from plan.md or ROADMAP.md]

## Implementation Scope
### Affected Components
- [ ] Maven multi-module project
  - Specific modules: [list affected modules]
- [ ] IntelliJ plugin (gradle)
- [ ] Eclipse plugin (eclipse)
- [ ] CLI implementation (cli)
- [ ] VSCode extension (vscode)

### File Changes Required
[List all files that need to be modified or created, grouped by component]

### Cross-Implementation Requirements
- Identify shared functionality
- Note platform-specific variations
- Document interface requirements
- List common test scenarios

### Documentation Updates
- [ ] README.md command matrix
- [ ] Command documentation
- [ ] Implementation-specific docs
- [ ] API documentation

### Testing Requirements
- [ ] Unit tests per implementation
- [ ] Integration tests
- [ ] Cross-platform compatibility
- [ ] Performance benchmarks

## Task Completion Checklist
1. [ ] Implementation for each component
2. [ ] Documentation updates
3. [ ] Test coverage
4. [ ] Command matrix update
5. [ ] Remove task from:
   - [ ] plan.md
   - [ ] ROADMAP.md

## LLM Implementation Prompt
```
You are a detail-oriented AI assistant helping with the Manorrock Assistant project. Analyze the task description below and generate a detailed task file at plan/task-{ID}.md.

Important considerations:
1. The project has multiple implementations that need to stay in sync:
   - Maven multi-module project (root)
   - IntelliJ plugin (gradle/)
   - Eclipse plugin (eclipse/)
   - CLI implementation (cli/)
   - VSCode extension (vscode/)

2. For each task:
   - Analyze the codebase
   - Identify all affected files
   - Consider cross-implementation impacts
   - Include necessary documentation updates
   - List required test changes
   - Update the command matrix in README.md if needed
   - Remove task from plan.md when complete
   - Remove task from ROADMAP.md when complete
   - Remove any completed tasks from plan.md
   - Remove any completed tasks from ROADMAP.md
   - If there are no remaining tasks in plan.md remove plan.md
   - If there is an already existing implementation for the task, 
     you MUST include a link to the implementation in the task file.
   - If there is an already existing code for the task, you MUST
     use it and if it needs to be updated you can do that.

3. Generate a task file that includes:
   - Complete task breakdown
   - File-by-file changes needed
   - Implementation requirements
   - Testing requirements
   - Documentation updates
   - Dependencies
   - Step-by-step implementation plan
     - If the task is for a specific implementation look at the 
       other implementations, favoring the CLI implementation, for guidance,
       even if the specific implementation already has a partial implementation.
   - An LLM prompt for implementing the changes and you MUST make sure 
     all files that are mentioned that need to be updated MUST exist
     and they MUST include paths and they MUST be relative
     to the root of the project. The LLM prompt must be at the end of 
     the task file separate by a line and it MUST be in a code block.
     Make sure the prompt includes 
      - Remove task from plan.md when complete
      - Remove task from ROADMAP.md when complete
      - Remove any completed tasks from plan.md
      - Remove any completed tasks from ROADMAP.md
      - If there are no remaining tasks in plan.md remove plan.md
      - If there is an already existing implementation for the task, 
        you MUST include a link to the implementation in the task file.
       - If there is an already existing code for the task, you MUST
        use it and if it needs to be updated you can do that.

Be exhaustive in the analysis and breakdown of the task. The generated task file should contain sufficient detail for independent implementation. All files mentioned MUST include paths and they MUST be relative to the root of the project.

*Extremely important*
1. All files that are mentioned that need to be updated MUST exist
2. All files mentioned MUST include paths relative to the root of the project.
3. Aill new files that are created MUST include paths relative to the root of the project.

Task ID: {ID}
Task Description: {DESCRIPTION}
```

## Example Usage
```bash
# 1. Copy this template
# 2. Fill in the task details
# 3. Use the LLM prompt to generate implementation details
# 4. Create sub-tasks if needed
# 5. Track progress using the checklist
```
