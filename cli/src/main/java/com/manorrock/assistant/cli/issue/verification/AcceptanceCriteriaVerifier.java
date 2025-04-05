package com.manorrock.assistant.cli.issue.verification;

import com.manorrock.assistant.cli.issue.Issue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Verifies implementations against acceptance criteria.
 * 
 * <p>
 * This class provides functionality to:
 * - Extract verification points from acceptance criteria
 * - Compare implementation against criteria
 * - Generate verification reports
 * - Suggest improvements for failed criteria
 * </p>
 *
 * @author Manorrock Assistant
 */
public class AcceptanceCriteriaVerifier {
    
    /**
     * Pattern to recognize common measurable criteria (contains numbers/percentages)
     */
    private static final Pattern MEASURABLE_PATTERN = 
            Pattern.compile("(\\d+)\\s*%|\\b(at least|maximum|minimum|less than|more than|under|over)\\s+(\\d+)");
    
    /**
     * Pattern to recognize testable functionality keywords
     */
    private static final Pattern TESTABLE_PATTERN = 
            Pattern.compile("\\b(validates|verifies|checks|tests|confirms|ensures|must|should|shall)\\b", 
                    Pattern.CASE_INSENSITIVE);
    
    /**
     * Common color names for matching
     */
    private static final String[] COLORS = {
        "blue", "red", "green", "yellow", "black", "white", "gray", "purple", "orange", "pink"
    };
    
    /**
     * The issue being verified.
     */
    private final Issue issue;
    
    /**
     * Creates a new acceptance criteria verifier.
     * 
     * @param issue The issue to verify
     */
    public AcceptanceCriteriaVerifier(Issue issue) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue must not be null");
        }
        this.issue = issue;
    }
    
    /**
     * Verifies an implementation against the acceptance criteria.
     * 
     * @param implementation The implementation to verify
     * @return A verification report
     */
    public VerificationReport verify(String implementation) {
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation must not be null");
        }
        
        return verify(issue, implementation);
    }
    
    /**
     * Analyzes acceptance criteria to extract verification points.
     * 
     * @param criteria The list of acceptance criteria to analyze
     * @return A list of verification points extracted from the criteria
     */
    public List<VerificationPoint> extractVerificationPoints(List<String> criteria) {
        List<VerificationPoint> points = new ArrayList<>();
        
        for (int i = 0; i < criteria.size(); i++) {
            String criterion = criteria.get(i);
            VerificationPoint point = new VerificationPoint(i, criterion);
            
            // Analyze testability
            if (MEASURABLE_PATTERN.matcher(criterion).find()) {
                point.setTestable(true);
                point.setTestabilityReason("Contains measurable metrics");
            } else if (TESTABLE_PATTERN.matcher(criterion).find()) {
                point.setTestable(true);
                point.setTestabilityReason("Contains testable functionality keywords");
            }
            
            points.add(point);
        }
        
        return points;
    }
    
    /**
     * Verifies an implementation against acceptance criteria.
     * 
     * @param issue The issue containing acceptance criteria
     * @param implementation The implementation content to verify
     * @return A verification report
     */
    public VerificationReport verify(Issue issue, String implementation) {
        if (issue == null || implementation == null) {
            throw new IllegalArgumentException("Issue and implementation must not be null");
        }
        
        List<String> criteria = issue.getAcceptanceCriteria();
        List<VerificationPoint> points = extractVerificationPoints(criteria);
        Map<Integer, VerificationResult> results = new HashMap<>();
        
        for (VerificationPoint point : points) {
            VerificationResult result = verifyPoint(point, implementation);
            results.put(point.getIndex(), result);
        }
        
        return new VerificationReport(issue, points, results);
    }
    
    /**
     * Verifies a single point against the implementation.
     * 
     * @param point The verification point
     * @param implementation The implementation content
     * @return The verification result
     */
    private VerificationResult verifyPoint(VerificationPoint point, String implementation) {
        String criterion = point.getCriterion();
        boolean passed = false;
        String suggestion = "";
        
        // Handle empty implementation
        if (implementation == null || implementation.trim().isEmpty()) {
            return new VerificationResult(false, "Implementation is empty");
        }
        
        String lowerImplementation = implementation.toLowerCase();
        
        // Extract key terms and meaningful phrases from the criterion
        List<String> keyTerms = extractKeyTerms(criterion);
        List<String> missingTerms = new ArrayList<>();
        
        // Check for exact quoted requirements
        String quotedText = extractQuotedText(criterion);
        if (!quotedText.isEmpty() && !implementation.toLowerCase().contains(quotedText.toLowerCase())) {
            return new VerificationResult(false, "Required text '" + quotedText + "' not found in implementation");
        }
        
        // Check each term
        for (String term : keyTerms) {
            if (!termMatches(term, lowerImplementation)) {
                missingTerms.add(term);
            }
        }
        
        passed = missingTerms.isEmpty();
        
        // Look for semantic contradictions
        if (passed && containsTermWithOpposite(criterion)) {
            for (String term : keyTerms) {
                if (hasOpposite(term) && containsOpposite(term, lowerImplementation)) {
                    passed = false;
                    suggestion = "Implementation contains opposite meaning of '" + term + "'";
                    break;
                }
            }
        }
        
        if (!passed && suggestion.isEmpty()) {
            suggestion = generateSuggestion(point, missingTerms);
        }
        
        return new VerificationResult(passed, suggestion);
    }
    
    /**
     * Extracts text within quotes from a criterion.
     * 
     * @param criterion The criterion to extract from
     * @return The quoted text, or empty string if none found
     */
    private String extractQuotedText(String criterion) {
        // Try single quotes first
        int start = criterion.indexOf("'");
        int end = criterion.lastIndexOf("'");
        
        if (start != -1 && end != -1 && start < end) {
            return criterion.substring(start + 1, end);
        }
        
        // Try double quotes
        start = criterion.indexOf("\"");
        end = criterion.lastIndexOf("\"");
        
        if (start != -1 && end != -1 && start < end) {
            return criterion.substring(start + 1, end);
        }
        
        return "";
    }
    
    /**
     * Checks if a term matches in the implementation, including variations.
     * 
     * @param term The term to check
     * @param implementation The implementation text
     * @return True if the term or a variation matches
     */
    private boolean termMatches(String term, String implementation) {
        term = term.toLowerCase();
        implementation = implementation.toLowerCase();

        // For exact quoted text requirements
        String quotedText = extractQuotedText(term);
        if (!quotedText.isEmpty()) {
            return implementation.contains(quotedText.toLowerCase());
        }

        // For color requirements
        if (containsColorName(term)) {
            for (String color : COLORS) {
                if (term.contains(color)) {
                    return implementation.contains(color);
                }
            }
        }

        // For general terms, try to match key parts
        String[] parts = term.split("\\s+");
        int matchCount = 0;
        for (String part : parts) {
            if (part.length() <= 3 || isCommonWord(part)) {
                continue;
            }
            if (implementation.contains(part)) {
                matchCount++;
            }
        }
        return matchCount > 0;
    }
    
    /**
     * Checks if a criterion contains any terms that have semantic opposites.
     * 
     * @param text The text to check
     * @return True if any term has known opposites
     */
    private boolean containsTermWithOpposite(String text) {
        String[] terms = text.toLowerCase().split("\\s+");
        for (String term : terms) {
            if (hasOpposite(term)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a term has semantic opposites that we can check for.
     * 
     * @param term The term to check
     * @return True if the term has known opposites, false otherwise
     */
    private boolean hasOpposite(String term) {
        term = term.toLowerCase();
        
        // Common antonym pairs in requirements
        return term.equals("enable") || term.equals("disable") ||
               term.equals("show") || term.equals("hide") ||
               term.equals("open") || term.equals("close") ||
               term.equals("start") || term.equals("stop") ||
               term.equals("add") || term.equals("remove") ||
               term.equals("create") || term.equals("delete") ||
               term.equals("increase") || term.equals("decrease") ||
               term.equals("allow") || term.equals("prevent") ||
               term.equals("accept") || term.equals("reject") ||
               term.equals("valid") || term.equals("invalid");
    }
    
    /**
     * Checks if the implementation contains an opposite term to the given term.
     * 
     * @param term The original term
     * @param implementation The implementation text to check
     * @return True if an opposite term is found, false otherwise
     */
    private boolean containsOpposite(String term, String implementation) {
        term = term.toLowerCase();
        implementation = implementation.toLowerCase();
        
        switch (term) {
            case "enable":
                return implementation.contains("disable");
            case "disable":
                return implementation.contains("enable");
            case "show":
                return implementation.contains("hide");
            case "hide":
                return implementation.contains("show");
            case "open":
                return implementation.contains("close");
            case "close":
                return implementation.contains("open");
            case "start":
                return implementation.contains("stop");
            case "stop":
                return implementation.contains("start");
            case "add":
                return implementation.contains("remove") || implementation.contains("delete");
            case "remove":
                return implementation.contains("add");
            case "create":
                return implementation.contains("delete") || implementation.contains("remove");
            case "delete":
                return implementation.contains("create") || implementation.contains("add");
            case "increase":
                return implementation.contains("decrease");
            case "decrease":
                return implementation.contains("increase");
            case "allow":
                return implementation.contains("prevent") || implementation.contains("block");
            case "prevent":
                return implementation.contains("allow") || implementation.contains("permit");
            case "accept":
                return implementation.contains("reject") || implementation.contains("deny");
            case "reject":
                return implementation.contains("accept");
            case "valid":
                return implementation.contains("invalid");
            case "invalid":
                return implementation.contains("valid");
            default:
                return false;
        }
    }
    
    /**
     * Extracts key terms from a criterion for matching.
     * 
     * @param criterion The criterion text
     * @return A list of key terms
     */
    private List<String> extractKeyTerms(String criterion) {
        List<String> terms = new ArrayList<>();
        
        // Split by common separators and filter out common words
        String[] words = criterion.split("\\s+|,|;|\\.|\\(|\\)");
        for (String word : words) {
            word = word.toLowerCase().trim();
            if (word.length() > 3 && !isCommonWord(word)) {
                terms.add(word);
            }
        }
        
        return terms;
    }
    
    /**
     * Checks if a word is a common word to be excluded from key terms.
     * 
     * @param word The word to check
     * @return True if it's a common word, false otherwise
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {"the", "and", "that", "have", "for", "not", "with", "you", "this", "but"};
        for (String commonWord : commonWords) {
            if (commonWord.equals(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Generates a suggestion for a failed criterion.
     * 
     * @param point The verification point that failed
     * @param missingTerms List of key terms missing from the implementation
     * @return A suggestion string
     */
    private String generateSuggestion(VerificationPoint point, List<String> missingTerms) {
        StringBuilder suggestion = new StringBuilder();
        
        if (point.isTestable()) {
            suggestion.append("This criterion is testable (").append(point.getTestabilityReason()).append("). ");
            suggestion.append("Consider implementing an automated test to verify this requirement.");
        }
        
        if (!missingTerms.isEmpty()) {
            if (suggestion.length() > 0) {
                suggestion.append(" ");
            }
            suggestion.append("Missing key elements in implementation: ");
            suggestion.append(String.join(", ", missingTerms));
            suggestion.append(".");
        }
        
        if (suggestion.length() == 0) {
            suggestion.append("Review the implementation to ensure it addresses this criterion.");
        }
        
        return suggestion.toString();
    }
    
    /**
     * Checks if the text contains any color names.
     * 
     * @param text The text to check
     * @return True if a color name is found, false otherwise
     */
    private boolean containsColorName(String text) {
        text = text.toLowerCase();
        for (String color : COLORS) {
            if (text.contains(color)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Represents a report on verification of implementation against acceptance criteria.
     */
    public static class VerificationReport {
        private final Issue issue;
        private final List<VerificationPoint> points;
        private final Map<Integer, VerificationResult> results;
        private final List<String> satisfiedCriteria;
        private final List<String> unsatisfiedCriteria;
        private final List<String> suggestions;
        
        /**
         * Creates a new verification report.
         * 
         * @param issue The issue being verified
         * @param points The verification points
         * @param results The verification results
         */
        public VerificationReport(Issue issue, List<VerificationPoint> points, Map<Integer, VerificationResult> results) {
            this.issue = issue;
            this.points = Collections.unmodifiableList(points);
            this.results = Collections.unmodifiableMap(results);
            
            // Extract satisfied and unsatisfied criteria
            List<String> satisfied = new ArrayList<>();
            List<String> unsatisfied = new ArrayList<>();
            List<String> suggestionList = new ArrayList<>();
            
            for (VerificationPoint point : points) {
                VerificationResult result = results.get(point.getIndex());
                if (result != null) {
                    if (result.isPassed()) {
                        satisfied.add(point.getCriterion());
                    } else {
                        unsatisfied.add(point.getCriterion());
                        if (!result.getSuggestion().isEmpty()) {
                            suggestionList.add("To satisfy \"" + point.getCriterion() + "\": " + result.getSuggestion());
                        }
                    }
                }
            }
            
            this.satisfiedCriteria = Collections.unmodifiableList(satisfied);
            this.unsatisfiedCriteria = Collections.unmodifiableList(unsatisfied);
            this.suggestions = Collections.unmodifiableList(suggestionList);
        }
        
        /**
         * Gets the issue being verified.
         * 
         * @return The issue
         */
        public Issue getIssue() {
            return issue;
        }
        
        /**
         * Gets the verification points.
         * 
         * @return The verification points
         */
        public List<VerificationPoint> getPoints() {
            return points;
        }
        
        /**
         * Gets the verification results.
         * 
         * @return The verification results
         */
        public Map<Integer, VerificationResult> getResults() {
            return results;
        }
        
        /**
         * Gets the list of satisfied criteria.
         * 
         * @return The satisfied criteria
         */
        public List<String> getSatisfiedCriteria() {
            return satisfiedCriteria;
        }
        
        /**
         * Gets the list of unsatisfied criteria.
         * 
         * @return The unsatisfied criteria
         */
        public List<String> getUnsatisfiedCriteria() {
            return unsatisfiedCriteria;
        }
        
        /**
         * Gets the list of suggestions for unsatisfied criteria.
         * 
         * @return The suggestions
         */
        public List<String> getSuggestions() {
            return suggestions;
        }
        
        /**
         * Gets a summary of the verification results.
         * 
         * @return The summary
         */
        public String getSummary() {
            if (points.isEmpty()) {
                return "No acceptance criteria found to verify";
            }
            
            int total = points.size();
            int satisfied = satisfiedCriteria.size();
            int percentage = total > 0 ? (satisfied * 100) / total : 0;
            
            return String.format("%d criteria satisfied (%d%%)", satisfied, percentage);
        }
        
        /**
         * Formats the verification report for display.
         * 
         * @return Formatted report
         */
        public String format() {
            StringBuilder builder = new StringBuilder();
            
            builder.append("Verified ").append(points.size())
                   .append(" acceptance criteria for issue: ").append(issue.getTitle()).append("\n");

            // Summary line showing satisfied vs total
            int total = points.size();
            int satisfied = satisfiedCriteria.size();
            int percentage = total > 0 ? (satisfied * 100) / total : 0;
            builder.append(String.format("✓ %d criteria satisfied (%d%%)\n", satisfied, percentage));
            if (!unsatisfiedCriteria.isEmpty()) {
                builder.append(String.format("✗ %d criteria not satisfied\n", unsatisfiedCriteria.size()));
            }
            builder.append("\n");
            
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
                    builder.append(suggestion).append("\n");
                }
                builder.append("\n");
            }
            
            // Checklist
            builder.append("\n### Acceptance Criteria Checklist\n\n");
            for (VerificationPoint point : points) {
                VerificationResult result = results.get(point.getIndex());
                String checkbox = (result != null && result.isPassed()) ? "[x]" : "[ ]";
                builder.append("- ").append(checkbox).append(" ").append(point.getCriterion()).append("\n");
            }
            
            return builder.toString();
        }
    }
    
    /**
     * Represents a point to verify from acceptance criteria.
     */
    public static class VerificationPoint {
        private final int index;
        private final String criterion;
        private boolean testable;
        private String testabilityReason;
        
        /**
         * Creates a new verification point.
         * 
         * @param index The index of the criterion in the original list
         * @param criterion The criterion text
         */
        public VerificationPoint(int index, String criterion) {
            this.index = index;
            this.criterion = criterion;
            this.testable = false;
            this.testabilityReason = "";
        }
        
        /**
         * Gets the index of the criterion.
         * 
         * @return The index
         */
        public int getIndex() {
            return index;
        }
        
        /**
         * Gets the criterion text.
         * 
         * @return The criterion
         */
        public String getCriterion() {
            return criterion;
        }
        
        /**
         * Checks if the criterion is testable.
         * 
         * @return True if testable, false otherwise
         */
        public boolean isTestable() {
            return testable;
        }
        
        /**
         * Sets whether the criterion is testable.
         * 
         * @param testable True if testable, false otherwise
         */
        public void setTestable(boolean testable) {
            this.testable = testable;
        }
        
        /**
         * Gets the reason for testability assessment.
         * 
         * @return The testability reason
         */
        public String getTestabilityReason() {
            return testabilityReason;
        }
        
        /**
         * Sets the reason for testability assessment.
         * 
         * @param testabilityReason The testability reason
         */
        public void setTestabilityReason(String testabilityReason) {
            this.testabilityReason = testabilityReason;
        }
    }
    
    /**
     * Represents the result of verifying a criterion.
     */
    public static class VerificationResult {
        private final boolean passed;
        private final String suggestion;
        
        /**
         * Creates a new verification result.
         * 
         * @param passed True if the criterion passed verification, false otherwise
         * @param suggestion Suggestion for improvement if failed
         */
        public VerificationResult(boolean passed, String suggestion) {
            this.passed = passed;
            this.suggestion = suggestion;
        }
        
        /**
         * Checks if the verification passed.
         * 
         * @return True if passed, false otherwise
         */
        public boolean isPassed() {
            return passed;
        }
        
        /**
         * Gets the suggestion for improvement.
         * 
         * @return The suggestion
         */
        public String getSuggestion() {
            return suggestion;
        }
    }
}