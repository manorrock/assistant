package com.manorrock.assistant.cli.issue;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Issue model class.
 */
class IssueTest {

    @Test
    void testCreateValidIssue() {
        // Create a basic valid issue
        Issue issue = Issue.builder()
                .title("Implement feature X")
                .description("Add a new feature X to improve performance")
                .addAcceptanceCriterion("Feature X improves performance by at least 10%")
                .addAcceptanceCriterion("Feature X is properly documented")
                .addLabel("performance")
                .addLabel("feature")
                .priority(Priority.HIGH)
                .build();
        
        // Verify each property
        assertEquals("Implement feature X", issue.getTitle());
        assertEquals("Add a new feature X to improve performance", issue.getDescription());
        assertEquals(2, issue.getAcceptanceCriteria().size());
        assertEquals("Feature X improves performance by at least 10%", issue.getAcceptanceCriteria().get(0));
        assertEquals("Feature X is properly documented", issue.getAcceptanceCriteria().get(1));
        assertEquals(2, issue.getLabels().size());
        assertTrue(issue.getLabels().contains("performance"));
        assertTrue(issue.getLabels().contains("feature"));
        assertEquals(Priority.HIGH, issue.getPriority());
    }
    
    @Test
    void testMissingTitle() {
        // Should throw exception when title is missing
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            Issue.builder()
                .description("A description")
                .build();
        });
        
        assertTrue(exception.getMessage().contains("title is required"));
    }
    
    @Test
    void testMissingDescription() {
        // Should throw exception when description is missing
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            Issue.builder()
                .title("A title")
                .build();
        });
        
        assertTrue(exception.getMessage().contains("description is required"));
    }
    
    @Test
    void testDefaultPriority() {
        // Should use MEDIUM as default priority
        Issue issue = Issue.builder()
                .title("A title")
                .description("A description")
                .build();
        
        assertEquals(Priority.MEDIUM, issue.getPriority());
    }
    
    @Test
    void testBulkAddCriteria() {
        // Test adding a collection of criteria
        List<String> criteria = Arrays.asList(
            "Criterion 1",
            "Criterion 2",
            "Criterion 3"
        );
        
        Issue issue = Issue.builder()
                .title("A title")
                .description("A description")
                .acceptanceCriteria(criteria)
                .build();
        
        assertEquals(3, issue.getAcceptanceCriteria().size());
        assertEquals(criteria, issue.getAcceptanceCriteria());
    }
    
    @Test
    void testBulkAddLabels() {
        // Test adding a collection of labels
        Set<String> labels = new HashSet<>(Arrays.asList(
            "label1",
            "label2"
        ));
        
        Issue issue = Issue.builder()
                .title("A title")
                .description("A description")
                .labels(labels)
                .build();
        
        assertEquals(2, issue.getLabels().size());
        assertEquals(labels, issue.getLabels());
    }
    
    @Test
    void testEqualsAndHashCode() {
        // Create two issues with the same property values
        Issue issue1 = Issue.builder()
                .title("Title")
                .description("Description")
                .addAcceptanceCriterion("Criterion")
                .addLabel("label")
                .priority(Priority.HIGH)
                .build();
        
        Issue issue2 = Issue.builder()
                .title("Title")
                .description("Description")
                .addAcceptanceCriterion("Criterion")
                .addLabel("label")
                .priority(Priority.HIGH)
                .build();
        
        // Should be equal and have the same hash code
        assertEquals(issue1, issue2);
        assertEquals(issue1.hashCode(), issue2.hashCode());
        
        // Create an issue with different property value
        Issue issue3 = Issue.builder()
                .title("Different Title")
                .description("Description")
                .addAcceptanceCriterion("Criterion")
                .addLabel("label")
                .priority(Priority.HIGH)
                .build();
        
        // Should not be equal
        assertNotEquals(issue1, issue3);
    }
    
    @Test
    void testFilterNullAndBlankValues() {
        // Test that null and blank values are filtered out
        List<String> criteria = Arrays.asList(
            "Valid criterion",
            null,
            "",
            "  "
        );
        
        Set<String> labels = new HashSet<>(Arrays.asList(
            "valid-label",
            null,
            "",
            "  "
        ));
        
        Issue issue = Issue.builder()
                .title("A title")
                .description("A description")
                .acceptanceCriteria(criteria)
                .labels(labels)
                .build();
        
        // Only the valid values should be included
        assertEquals(1, issue.getAcceptanceCriteria().size());
        assertEquals("Valid criterion", issue.getAcceptanceCriteria().get(0));
        
        assertEquals(1, issue.getLabels().size());
        assertTrue(issue.getLabels().contains("valid-label"));
    }
}