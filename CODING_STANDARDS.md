# Coding Standards

This document outlines the coding standards for the Assistant project.

## Code Formatting

### Indentation
- Use 4 spaces for indentation
- Do not use tabs

### Line Length
- Keep lines to a maximum of 100 characters
- For longer strings or URLs that cannot be broken, exceeding this limit is acceptable

### Naming Conventions
- Use camelCase for method names and variables
- Use PascalCase for class names
- Use UPPER_SNAKE_CASE for constants
- Package names should be all lowercase

### File Organization
- One top-level class per file
- Files should be named after the main class they contain
- Related classes and interfaces should be grouped in the same package

### Imports
- Avoid wildcard imports (*)
- Group imports logically:
  1. Java core packages
  2. Third-party packages
  3. Project packages

### Comments
- Write clear and concise comments
- Use JavaDoc for public APIs
- Keep comments up to date with code changes

## Project Structure

1. Follow standard Maven project layout
2. Package naming convention: `com.manorrock.assistant.<module>`
3. Each implementation should be a separate module
4. Shared code goes in the `shared` module

## Documentation

1. All public APIs must have Javadoc
2. Include examples in README.md for each module
3. Document all commands in README.md

## Git Workflow

1. Squash commits before merging
2. Keep PRs focused and small

## Dependencies

1. Prefer stable, widely-used libraries
2. Document third-party license implications
3. Keep dependencies up to date
4. Minimize external dependencies
