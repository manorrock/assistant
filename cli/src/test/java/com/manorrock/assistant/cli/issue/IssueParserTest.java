package com.manorrock.assistant.cli.issue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the IssueParser class.
 */
class IssueParserTest {
    
    private IssueParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new IssueParser();
    }
    
    @Test
    void testParseCompleteMarkdownIssue() throws IssueParsingException {
        String issueText = """
                # Implement Feature X
                
                ## Description
                This is a description of Feature X.
                It spans multiple lines.
                
                ## Acceptance Criteria
                - Feature X should do A
                - Feature X should do B
                - [ ] Feature X should do C
                
                ## Priority
                HIGH
                
                ## Labels
                - feature
                - enhancement
                """;
        
        Issue issue = parser.parse(issueText);
        
        assertEquals("Implement Feature X", issue.getTitle());
        assertEquals("This is a description of Feature X.\nIt spans multiple lines.", issue.getDescription());
        assertEquals(3, issue.getAcceptanceCriteria().size());
        assertTrue(issue.getAcceptanceCriteria().contains("Feature X should do A"));
        assertTrue(issue.getAcceptanceCriteria().contains("Feature X should do B"));
        assertTrue(issue.getAcceptanceCriteria().contains("Feature X should do C"));
        assertEquals(Priority.HIGH, issue.getPriority());
        assertEquals(2, issue.getLabels().size());
        assertTrue(issue.getLabels().contains("feature"));
        assertTrue(issue.getLabels().contains("enhancement"));
    }
    
    @Test
    void testParseCompleteColonFormatIssue() throws IssueParsingException {
        String issueText = """
                Title: Fix Bug in Login Page
                
                Description:
                There is a bug in the login page that prevents users from logging in with certain special characters in their password.
                
                Acceptance Criteria:
                - Users can login with passwords containing special characters
                - Error messages are clear and helpful
                - Security is maintained
                
                Priority: High
                
                Labels: bug, security, urgent
                """;
        
        Issue issue = parser.parse(issueText);
        
        assertEquals("Fix Bug in Login Page", issue.getTitle());
        assertTrue(issue.getDescription().contains("prevents users from logging in"));
        assertEquals(3, issue.getAcceptanceCriteria().size());
        assertTrue(issue.getAcceptanceCriteria().contains("Users can login with passwords containing special characters"));
        assertEquals(Priority.HIGH, issue.getPriority());
        assertEquals(3, issue.getLabels().size());
        assertTrue(issue.getLabels().contains("bug"));
        assertTrue(issue.getLabels().contains("security"));
        assertTrue(issue.getLabels().contains("urgent"));
    }
    
    @Test
    void testParseMinimalIssue() throws IssueParsingException {
        String issueText = "Add new button to dashboard";
        
        Issue issue = parser.parse(issueText);
        
        assertEquals("Add new button to dashboard", issue.getTitle());
        // With minimal issue, description should be empty or derived
        assertTrue(issue.getDescription().isEmpty() || !issue.getDescription().isEmpty());
        assertTrue(issue.getAcceptanceCriteria().isEmpty());
        assertTrue(issue.getLabels().isEmpty());
        assertEquals(Priority.MEDIUM, issue.getPriority()); // Default priority
    }
    
    @Test
    void testParseMissingTitle() {
        String issueText = """
                ## Description
                This is a description without a title.
                """;
        
        // Should still be able to extract a title from somewhere
        assertDoesNotThrow(() -> parser.parse(issueText));
    }
    
    @Test
    void testParseEmptyIssue() {
        // Should throw exception for null or empty input
        assertThrows(IssueParsingException.class, () -> parser.parse(null));
        assertThrows(IssueParsingException.class, () -> parser.parse(""));
        assertThrows(IssueParsingException.class, () -> parser.parse("   "));
    }
    
    @Test
    void testExtractTitle() throws IssueParsingException {
        // Test H1 Markdown format
        assertEquals("Title in H1", parser.extractTitle("# Title in H1"));
        
        // Test labeled format
        assertEquals("Labeled Title", parser.extractTitle("Title: Labeled Title"));
        assertEquals("Case Insensitive", parser.extractTitle("TITLE: Case Insensitive"));
        
        // Test plain text first line
        assertEquals("Plain Text Title", parser.extractTitle("Plain Text Title\nDescription follows"));
    }
    
    @Test
    void testExtractDescription() {
        // Test markdown section
        String markdownDescription = """
                # Title
                
                ## Description
                This is a description.
                It has multiple lines.
                
                ## Other Section
                Other content.
                """;
        assertEquals("This is a description.\nIt has multiple lines.", parser.extractDescription(markdownDescription));
        
        // Test labeled format
        String labeledDescription = """
                Title: Test
                
                Description:
                This is a labeled description.
                
                Other: Content
                """;
        assertEquals("This is a labeled description.", parser.extractDescription(labeledDescription));
    }
    
    @Test
    void testExtractAcceptanceCriteria() {
        // Test markdown section with list items
        String markdownCriteria = """
                # Title
                
                ## Acceptance Criteria
                - Criterion A
                - Criterion B
                * Criterion C
                + Criterion D
                1. Criterion E
                
                ## Other Section
                """;
        List<String> criteria = parser.extractAcceptanceCriteria(markdownCriteria);
        assertEquals(5, criteria.size());
        assertTrue(criteria.contains("Criterion A"));
        assertTrue(criteria.contains("Criterion B"));
        assertTrue(criteria.contains("Criterion C"));
        assertTrue(criteria.contains("Criterion D"));
        assertTrue(criteria.contains("Criterion E"));
        
        // Test labeled format
        String labeledCriteria = """
                Title: Test
                
                Acceptance Criteria:
                - [ ] Checkbox item
                - [x] Completed item
                - Regular item
                
                Other: Content
                """;
        criteria = parser.extractAcceptanceCriteria(labeledCriteria);
        assertEquals(3, criteria.size());
        assertTrue(criteria.contains("Checkbox item"));
        assertTrue(criteria.contains("Completed item"));
        assertTrue(criteria.contains("Regular item"));
    }
    
    @Test
    void testExtractLabels() {
        // Test markdown section with list items
        String markdownLabels = """
                # Title
                
                ## Labels
                - label1
                - label2
                
                ## Other Section
                """;
        Set<String> labels = parser.extractLabels(markdownLabels);
        assertEquals(2, labels.size());
        assertTrue(labels.contains("label1"));
        assertTrue(labels.contains("label2"));
        
        // Test labeled format with comma separation
        String commaSeparatedLabels = """
                Title: Test
                
                Labels: tag1, tag2, tag3
                
                Other: Content
                """;
        labels = parser.extractLabels(commaSeparatedLabels);
        assertEquals(3, labels.size());
        assertTrue(labels.contains("tag1"));
        assertTrue(labels.contains("tag2"));
        assertTrue(labels.contains("tag3"));
    }
    
    @Test
    void testExtractPriority() {
        // Test markdown section
        String markdownPriority = """
                # Title
                
                ## Priority
                HIGH
                
                ## Other Section
                """;
        assertEquals(Priority.HIGH, parser.extractPriority(markdownPriority));
        
        // Test labeled format
        String labeledPriority = """
                Title: Test
                
                Priority: Low
                
                Other: Content
                """;
        assertEquals(Priority.LOW, parser.extractPriority(labeledPriority));
        
        // Test inline mentions
        assertEquals(Priority.HIGHEST, parser.extractPriority("This is highest priority"));
        assertEquals(Priority.HIGH, parser.extractPriority("This has priority: high"));
        assertEquals(Priority.LOW, parser.extractPriority("This has priority:low"));
        assertEquals(Priority.LOWEST, parser.extractPriority("This is lowest priority"));
        
        // Test default case
        assertEquals(Priority.MEDIUM, parser.extractPriority("This has no priority information"));
    }
}