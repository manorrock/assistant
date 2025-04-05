package com.manorrock.assistant.cli.issue.verification;

/**
 * Represents the result of verifying a single acceptance criterion.
 * 
 * <p>
 * Contains information about whether the criterion passed or failed,
 * along with details about why and suggestions for improvement.
 * </p>
 * 
 * @author Manorrock Assistant
 */
public class VerificationResult {
    
    /**
     * The verification point being verified.
     */
    private final VerificationPoint verificationPoint;
    
    /**
     * Whether the criterion passed verification.
     */
    private boolean passed;
    
    /**
     * The reason for pass/fail status.
     */
    private String reason;
    
    /**
     * Suggestion for improvement if failed.
     */
    private String suggestion;
    
    /**
     * Constructs a new verification result.
     * 
     * @param verificationPoint The verification point
     */
    public VerificationResult(VerificationPoint verificationPoint) {
        this.verificationPoint = verificationPoint;
        this.passed = false;
        this.reason = "Not verified";
        this.suggestion = "";
    }
    
    /**
     * Gets the verification point.
     * 
     * @return The verification point
     */
    public VerificationPoint getVerificationPoint() {
        return verificationPoint;
    }
    
    /**
     * Checks if the criterion passed verification.
     * 
     * @return True if passed, false otherwise
     */
    public boolean isPassed() {
        return passed;
    }
    
    /**
     * Sets whether the criterion passed verification.
     * 
     * @param passed Whether the criterion passed
     */
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    
    /**
     * Gets the reason for pass/fail status.
     * 
     * @return The reason
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Sets the reason for pass/fail status.
     * 
     * @param reason The reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    /**
     * Gets the suggestion for improvement.
     * 
     * @return The suggestion
     */
    public String getSuggestion() {
        return suggestion;
    }
    
    /**
     * Sets the suggestion for improvement.
     * 
     * @param suggestion The suggestion
     */
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Criterion #").append(verificationPoint.getIndex() + 1)
               .append(" [").append(passed ? "PASSED" : "FAILED").append("]: ")
               .append(verificationPoint.getCriterion())
               .append("\nReason: ").append(reason);
        
        if (!passed && !suggestion.isEmpty()) {
            builder.append("\nSuggestion: ").append(suggestion);
        }
        
        return builder.toString();
    }
}