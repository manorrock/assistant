# Sample Test Issues

This directory contains sample test issues for testing the automated issue implementation workflow. The issues are organized by complexity level and demonstrate different formats and types of acceptance criteria.

## Simple Issues

### [login-button.md](simple/login-button.md)
A basic markdown-formatted feature request for adding a login button to a homepage.

**Expected parsing behavior:**
- Title: "Add Login Button"
- Description: Single paragraph description
- Acceptance Criteria: 3 simple bullet points
- Priority: HIGH
- Labels: ui, user-management

**Implementation considerations:**
- Straightforward UI component addition
- All acceptance criteria should be easily verifiable

### [input-validation.txt](simple/input-validation.txt)
A labeled-format issue for form validation with checkbox-style acceptance criteria.

**Expected parsing behavior:**
- Title: "Add Input Validation"
- Description: Single paragraph
- Acceptance Criteria: 5 items with checkbox notation
- Priority: MEDIUM
- Labels/Tags: form-validation, user-experience, security

**Implementation considerations:**
- Front-end validation logic
- Note that one criterion is already marked as completed

## Medium Complexity Issues

### [user-authentication.md](medium/user-authentication.md)
A service implementation with numbered acceptance criteria for user authentication.

**Expected parsing behavior:**
- Title: "Implement User Authentication Service"
- Description: Multi-sentence description of authentication service
- Acceptance Criteria: 8 numbered items covering different aspects
- Priority: HIGH
- Labels: security, user-management, backend

**Implementation considerations:**
- Backend service implementation
- Security-critical feature requiring careful implementation
- Multiple interconnected functions (login, logout, token management)

### [data-export.txt](medium/data-export.txt)
A feature implementation with nested and mixed format acceptance criteria.

**Expected parsing behavior:**
- Title: "Data Export Functionality"
- Description: Multi-sentence description
- Acceptance Criteria: Mixed format with nested bullets 
- Priority: MEDIUM
- Labels: user-feature, data-management

**Implementation considerations:**
- Multiple export formats to implement
- Background processing requirements
- UI and service components

## Complex Issues

### [collaborative-editing.md](complex/collaborative-editing.md)
A sophisticated feature with technical requirements and architectural considerations.

**Expected parsing behavior:**
- Title: "Implement Real-time Collaborative Editing"
- Description: Complex feature description
- Technical Requirements: 5 specific requirements
- Acceptance Criteria: 9 detailed criteria with checkbox format
- Implementation Notes: Microservice architecture details
- Priority: HIGH
- Labels: collaboration, real-time, performance-critical, architecture

**Implementation considerations:**
- Real-time systems architecture
- Multiple services working together
- Performance requirements
- Conflict resolution algorithms

### [memory-leak.txt](complex/memory-leak.txt)
A complex bug fix with stack traces and detailed reproduction steps.

**Expected parsing behavior:**
- Title: "Fix Memory Leak in Long-Running Background Tasks"
- Description: Detailed bug description
- Analysis: Technical analysis of the issue
- Stack Trace: Technical error details
- Acceptance Criteria: 6 specific verification points
- Reproduction Steps: Detailed test steps
- Priority: CRITICAL
- Labels: bug, memory-leak, performance, production-issue

**Implementation considerations:**
- Bug fix rather than feature implementation
- Requires understanding of resource management
- Testing and verification are critical
- Performance monitoring implementation

## Using These Test Issues

These test issues can be used to:

1. Verify the parser correctly extracts structured information from different formats
2. Test the implementation command with varying complexity levels
3. Verify the acceptance criteria verification system works with different criteria types
4. Benchmark implementation quality and performance across issue types
5. Identify any limitations in handling specific issue formats or complexity levels

To run tests with these issues:

```bash
# Test with a simple issue
/implement-issue file src/test/resources/sample-issues/simple/login-button.md

# Test with a complex issue
/implement-issue file src/test/resources/sample-issues/complex/collaborative-editing.md

# Test with a specific output directory
/implement-issue file src/test/resources/sample-issues/medium/data-export.txt output_dir=target/test-output/data-export

# Test with verification enabled
/implement-issue file src/test/resources/sample-issues/simple/input-validation.txt verify_implementation=true
```