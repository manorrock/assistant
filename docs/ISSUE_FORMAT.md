# Issue Format Documentation

This document outlines the recommended format for creating issues that will be processed by the Manorrock Assistant CLI. Following these guidelines will ensure that your issues are correctly parsed and implemented.

## Supported Issue Formats

The issue parser supports both Markdown-formatted and plain text issues. The following sections describe the expected structure and format for each component of an issue.

### Issue Title

The title should be concise and descriptive. It can be specified in one of these formats:

- As a Markdown H1 header: `# Your Issue Title`
- Using a labeled format: `Title: Your Issue Title`
- As the first line of the issue text

### Description

The description should provide details about the issue, including any relevant context or background information. It can be specified in these formats:

- As a Markdown section: 
  ```markdown
  ## Description
  Your detailed description here, which can span multiple lines.
  ```
- Using a labeled format:
  ```
  Description:
  Your detailed description here, which can span multiple lines.
  ```

### Acceptance Criteria

Acceptance criteria define the conditions that must be met for the issue to be considered complete. They can be specified in these formats:

- As a Markdown section with list items:
  ```markdown
  ## Acceptance Criteria
  - First criterion
  - Second criterion
  - Third criterion
  ```
- Using a labeled format with list items:
  ```
  Acceptance Criteria:
  - First criterion
  - Second criterion
  - Third criterion
  ```

The parser supports various list item formats:
- Using hyphens: `- Criterion`
- Using asterisks: `* Criterion`
- Using plus signs: `+ Criterion`
- Using numbered lists: `1. Criterion`
- Using checkboxes: `- [ ] Criterion` or `- [x] Criterion`

### Priority

The priority indicates the importance of the issue. It can be specified in these formats:

- As a Markdown section:
  ```markdown
  ## Priority
  HIGH
  ```
- Using a labeled format:
  ```
  Priority: HIGH
  ```

Supported priority values (case-insensitive):
- `HIGHEST` (also recognizes: `CRITICAL`, `P0`, `P1`, `1`)
- `HIGH` (also recognizes: `MAJOR`, `P2`, `2`)
- `MEDIUM` (also recognizes: `NORMAL`, `P3`, `3`) - This is the default if priority is not specified
- `LOW` (also recognizes: `MINOR`, `P4`, `4`)
- `LOWEST` (also recognizes: `TRIVIAL`, `P5`, `5`)

### Labels/Tags

Labels or tags help categorize the issue. They can be specified in these formats:

- As a Markdown section with list items:
  ```markdown
  ## Labels
  - label1
  - label2
  ```
- Using a labeled format with comma-separated values:
  ```
  Labels: label1, label2, label3
  ```
- Using a labeled format with list items:
  ```
  Tags:
  - tag1
  - tag2
  ```

## Complete Issue Examples

### Example 1: Markdown Format

```markdown
# Add User Authentication Feature

## Description
Implement a user authentication system that allows users to register, log in, and manage their accounts.

## Acceptance Criteria
- Users can register with email and password
- Users can log in with their credentials
- Users can reset their password
- Invalid login attempts are handled with appropriate error messages
- Users can log out

## Priority
HIGH

## Labels
- feature
- security
- user-management
```

### Example 2: Labeled Format

```
Title: Fix Performance Issue in Data Processing Module

Description:
The data processing module is experiencing significant slowdowns when handling large datasets (>10MB).
Initial profiling indicates that the bottleneck might be in the sorting algorithm.

Acceptance Criteria:
- [ ] Identify the specific cause of the performance issue
- [ ] Implement a more efficient algorithm or approach
- [ ] Processing time should be reduced by at least 50%
- [ ] Memory usage should not increase
- [ ] All existing functionality must continue to work correctly

Priority: HIGHEST

Labels: bug, performance, optimization
```

### Example 3: Minimal Format

```
Add Export to PDF Button

Implement a button that allows users to export the current document as a PDF file.
The PDF should maintain all formatting and styling from the original document.

- PDF exports should include all document content
- The button should be located in the main toolbar
- A progress indicator should be shown during export
- Error handling for failed exports

Priority: Medium
```

## Issue Template

Here's a template you can copy and customize for your own issues:

```markdown
# [Issue Title]

## Description
[Detailed description of the issue, including context and background information]

## Acceptance Criteria
- [First criterion]
- [Second criterion]
- [Third criterion]
- [Add more criteria as needed]

## Priority
[HIGHEST/HIGH/MEDIUM/LOW/LOWEST]

## Labels
- [label1]
- [label2]
- [Add more labels as needed]
```

## Best Practices

1. **Be Specific**: Provide clear and specific details in your issue description and acceptance criteria.
2. **Use Lists**: Format acceptance criteria as list items for better readability and parsing.
3. **Include Context**: Provide relevant context and background information in the description.
4. **Set Appropriate Priority**: Choose a priority level that accurately reflects the importance of the issue.
5. **Add Relevant Labels**: Use labels to categorize the issue and provide additional context.
6. **Keep it Structured**: Following a consistent structure makes issues easier to understand and process.

## Troubleshooting

If your issue is not being parsed correctly:

1. Ensure your issue follows one of the supported formats outlined above.
2. Check for correct syntax in Markdown sections (e.g., `##` for headings, proper list formatting).
3. Verify that section titles match the expected formats (e.g., "Description", "Acceptance Criteria").
4. Ensure there is a clear title at the beginning of the issue.
5. If using labeled format, ensure that labels are followed by a colon (e.g., "Priority:").