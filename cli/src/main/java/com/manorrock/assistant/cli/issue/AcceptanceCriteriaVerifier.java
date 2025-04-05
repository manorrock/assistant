package com.manorrock.assistant.cli.issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AcceptanceCriteriaVerifier {
    private final Issue issue;

    public AcceptanceCriteriaVerifier(Issue issue) {
        this.issue = issue;
    }

    public VerificationReport verify(String implementation) {
        if (issue.getAcceptanceCriteria().isEmpty()) {
            return new VerificationReport(issue, List.of(), Map.of(), 
                List.of(), List.of(), List.of());
        }

        List<String> satisfiedCriteria = new ArrayList<>();
        List<String> unsatisfiedCriteria = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        for (String criterion : issue.getAcceptanceCriteria()) {
            if (termMatches(criterion, implementation)) {
                satisfiedCriteria.add(criterion);
            } else {
                unsatisfiedCriteria.add(criterion);
                suggestions.add("To satisfy \"" + criterion + "\": Implementation does not contain required elements");
            }
        }

        return new VerificationReport(issue, issue.getAcceptanceCriteria(), 
            Map.of(), satisfiedCriteria, unsatisfiedCriteria, suggestions);
    }

    private boolean termMatches(String term, String implementation) {
        if (implementation == null || implementation.isEmpty()) {
            return false;
        }

        // For button text requirements with quotes
        if (term.contains("'")) {
            int start = term.indexOf("'");
            int end = term.lastIndexOf("'");
            if (start != -1 && end != -1 && start < end) {
                String requiredText = term.substring(start + 1, end).toLowerCase();
                return implementation.toLowerCase().contains(requiredText);
            }
        }

        // Convert both to lowercase for comparison
        term = term.toLowerCase();
        implementation = implementation.toLowerCase();

        // Split term into words
        String[] words = term.split("\\s+");
        for (String word : words) {
            if (word.length() <= 3 || isCommonWord(word)) {
                continue;
            }
            if (!implementation.contains(word)) {
                return false;
            }
        }

        return true;
    }

    private boolean isCommonWord(String word) {
        return word.equals("the") || word.equals("and") || word.equals("or") ||
               word.equals("to") || word.equals("a") || word.equals("in") ||
               word.equals("of") || word.equals("with") || word.equals("for") ||
               word.equals("should") || word.equals("must") || word.equals("will");
    }

    public static class VerificationReport {
        private final Issue issue;
        private final List<String> points;
        private final Map<Integer, VerificationResult> results;
        private final List<String> satisfiedCriteria;
        private final List<String> unsatisfiedCriteria;
        private final List<String> suggestions;

        public VerificationReport(Issue issue, List<String> points, 
                                Map<Integer, VerificationResult> results,
                                List<String> satisfiedCriteria, 
                                List<String> unsatisfiedCriteria,
                                List<String> suggestions) {
            this.issue = issue;
            this.points = Collections.unmodifiableList(points);
            this.results = Collections.unmodifiableMap(results);
            this.satisfiedCriteria = Collections.unmodifiableList(satisfiedCriteria);
            this.unsatisfiedCriteria = Collections.unmodifiableList(unsatisfiedCriteria);
            this.suggestions = Collections.unmodifiableList(suggestions);
        }

        public List<String> getSatisfiedCriteria() {
            return satisfiedCriteria;
        }

        public List<String> getUnsatisfiedCriteria() {
            return unsatisfiedCriteria;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public String getSummary() {
            if (points.isEmpty()) {
                return "No acceptance criteria found to verify";
            }

            int total = points.size();
            int satisfied = satisfiedCriteria.size();
            int percentage = (satisfied * 100) / total;

            return String.format("%d criteria satisfied (%d%%)", satisfied, percentage);
        }

        public String format() {
            StringBuilder builder = new StringBuilder();
            builder.append("Verified ").append(points.size())
                   .append(" criteria for: ").append(issue.getTitle())
                   .append("\n\n");

            // Summary
            builder.append(getSummary()).append("\n\n");

            // Satisfied criteria
            if (!satisfiedCriteria.isEmpty()) {
                builder.append("Satisfied Criteria:\n");
                for (String criterion : satisfiedCriteria) {
                    builder.append("✓ ").append(criterion).append("\n");
                }
                builder.append("\n");
            }

            // Unsatisfied criteria
            if (!unsatisfiedCriteria.isEmpty()) {
                builder.append("Unsatisfied Criteria:\n");
                for (String criterion : unsatisfiedCriteria) {
                    builder.append("✗ ").append(criterion).append("\n");
                }
                builder.append("\n");
            }

            // Suggestions
            if (!suggestions.isEmpty()) {
                builder.append("Suggestions for Improvement:\n");
                for (String suggestion : suggestions) {
                    builder.append("- ").append(suggestion).append("\n");
                }
            }

            return builder.toString();
        }
    }

    public static class VerificationResult {
        private final boolean passed;
        private final String suggestion;

        public VerificationResult(boolean passed, String suggestion) {
            this.passed = passed;
            this.suggestion = suggestion;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getSuggestion() {
            return suggestion;
        }
    }
}