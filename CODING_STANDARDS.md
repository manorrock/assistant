# Coding Standards and Guidelines

## Java Code Style

1. Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
2. Use 4 spaces for indentation
3. Maximum line length: 120 characters
4. Use explicit scoping for all class members

## Project Structure

1. Follow standard Maven project layout
2. Package naming convention: `com.manorrock.assistant.<module>`
3. Each implementation should be a separate module
4. Shared code goes in the `shared` module

## Documentation

1. All public APIs must have Javadoc
2. Include examples in README.md for each module
3. Keep CHANGELOG.md up to date
4. Document all commands in README.md

## Testing

1. Unit tests required for all new features
2. Maintain minimum 80% code coverage for new code
3. Integration tests for cross-module features
4. Performance tests for critical paths

## Git Workflow

1. Squash commits before merging
2. Keep PRs focused and small

## Dependencies

1. Prefer stable, widely-used libraries
2. Document third-party license implications
3. Keep dependencies up to date
4. Minimize external dependencies
