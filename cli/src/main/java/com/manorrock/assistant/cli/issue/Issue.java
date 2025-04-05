package com.manorrock.assistant.cli.issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a structured issue containing title, description, acceptance criteria, and metadata.
 * This model class provides a standardized structure for working with issue data in the CLI.
 * 
 * <p>
 * The {@code Issue} model supports validation to ensure essential data is present.
 * It also provides a builder pattern for convenient and readable object creation.
 * </p>
 * 
 * @author Manorrock Assistant
 */
public class Issue {
    
    /**
     * The issue title.
     */
    private final String title;
    
    /**
     * The issue description.
     */
    private final String description;
    
    /**
     * The list of acceptance criteria for the issue.
     */
    private final List<String> acceptanceCriteria;
    
    /**
     * The set of labels or tags associated with the issue.
     */
    private final Set<String> labels;
    
    /**
     * The priority of the issue.
     */
    private final Priority priority;
    
    /**
     * Private constructor used by the builder.
     * 
     * @param builder The builder instance to use for construction
     */
    private Issue(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.acceptanceCriteria = Collections.unmodifiableList(new ArrayList<>(builder.acceptanceCriteria));
        this.labels = Collections.unmodifiableSet(new HashSet<>(builder.labels));
        this.priority = builder.priority;
    }
    
    /**
     * Gets the issue title.
     * 
     * @return The issue title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the issue description.
     * 
     * @return The issue description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the list of acceptance criteria.
     * 
     * @return An unmodifiable list of acceptance criteria
     */
    public List<String> getAcceptanceCriteria() {
        return acceptanceCriteria;
    }
    
    /**
     * Gets the set of labels/tags.
     * 
     * @return An unmodifiable set of labels/tags
     */
    public Set<String> getLabels() {
        return labels;
    }
    
    /**
     * Gets the priority of the issue.
     * 
     * @return The issue priority
     */
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Builder class for creating Issue instances.
     */
    public static class Builder {
        private String title;
        private String description;
        private List<String> acceptanceCriteria = new ArrayList<>();
        private Set<String> labels = new HashSet<>();
        private Priority priority = Priority.MEDIUM; // Default priority
        
        /**
         * Sets the issue title.
         * 
         * @param title The issue title
         * @return This builder instance
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * Sets the issue description.
         * 
         * @param description The issue description
         * @return This builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Adds a single acceptance criterion to the list.
         * 
         * @param criterion The acceptance criterion to add
         * @return This builder instance
         */
        public Builder addAcceptanceCriterion(String criterion) {
            if (criterion != null && !criterion.isBlank()) {
                this.acceptanceCriteria.add(criterion);
            }
            return this;
        }
        
        /**
         * Sets the list of acceptance criteria, replacing any existing criteria.
         * 
         * @param criteria The list of acceptance criteria
         * @return This builder instance
         */
        public Builder acceptanceCriteria(List<String> criteria) {
            this.acceptanceCriteria.clear();
            if (criteria != null) {
                criteria.stream()
                    .filter(c -> c != null && !c.isBlank())
                    .forEach(this.acceptanceCriteria::add);
            }
            return this;
        }
        
        /**
         * Adds a single label/tag to the set.
         * 
         * @param label The label/tag to add
         * @return This builder instance
         */
        public Builder addLabel(String label) {
            if (label != null && !label.isBlank()) {
                this.labels.add(label);
            }
            return this;
        }
        
        /**
         * Sets the set of labels/tags, replacing any existing labels.
         * 
         * @param labels The set of labels/tags
         * @return This builder instance
         */
        public Builder labels(Set<String> labels) {
            this.labels.clear();
            if (labels != null) {
                labels.stream()
                    .filter(l -> l != null && !l.isBlank())
                    .forEach(this.labels::add);
            }
            return this;
        }
        
        /**
         * Sets the issue priority.
         * 
         * @param priority The issue priority
         * @return This builder instance
         */
        public Builder priority(Priority priority) {
            this.priority = priority != null ? priority : Priority.MEDIUM;
            return this;
        }
        
        /**
         * Builds and returns a new Issue instance.
         * 
         * @return A new Issue instance
         * @throws IllegalStateException if required fields are missing
         */
        public Issue build() {
            validate();
            return new Issue(this);
        }
        
        /**
         * Validates that the required fields are not null or empty.
         * 
         * @throws IllegalStateException if validation fails
         */
        private void validate() {
            if (title == null || title.isBlank()) {
                throw new IllegalStateException("Issue title is required");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalStateException("Issue description is required");
            }
        }
    }
    
    /**
     * Creates and returns a new Builder instance.
     * 
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Issue issue = (Issue) o;
        return Objects.equals(title, issue.title) &&
               Objects.equals(description, issue.description) &&
               Objects.equals(acceptanceCriteria, issue.acceptanceCriteria) &&
               Objects.equals(labels, issue.labels) &&
               priority == issue.priority;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(title, description, acceptanceCriteria, labels, priority);
    }
    
    @Override
    public String toString() {
        return "Issue{" +
               "title='" + title + '\'' +
               ", description='" + (description != null ? description.substring(0, Math.min(description.length(), 50)) + "..." : "null") + '\'' +
               ", acceptanceCriteria=" + acceptanceCriteria +
               ", labels=" + labels +
               ", priority=" + priority +
               '}';
    }
}