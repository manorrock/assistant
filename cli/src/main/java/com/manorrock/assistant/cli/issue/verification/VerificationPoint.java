package com.manorrock.assistant.cli.issue.verification;

/**
 * Represents a point extracted from acceptance criteria for verification.
 * 
 * <p>
 * Each verification point corresponds to a single acceptance criterion and
 * contains metadata about its testability and verification status.
 * </p>
 * 
 * @author Manorrock Assistant
 */
public class VerificationPoint {
    
    /**
     * The index of the criterion in the original list.
     */
    private final int index;
    
    /**
     * The criterion text.
     */
    private final String criterion;
    
    /**
     * Whether the criterion is testable through automated means.
     */
    private boolean testable;
    
    /**
     * The reason why the criterion is testable or not.
     */
    private String testabilityReason;
    
    /**
     * Constructs a new verification point.
     * 
     * @param index The index of the criterion
     * @param criterion The criterion text
     */
    public VerificationPoint(int index, String criterion) {
        this.index = index;
        this.criterion = criterion;
        this.testable = false;
        this.testabilityReason = "Not automatically testable";
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
     * @return The criterion text
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
     * @param testable Whether the criterion is testable
     */
    public void setTestable(boolean testable) {
        this.testable = testable;
    }
    
    /**
     * Gets the reason for testability status.
     * 
     * @return The testability reason
     */
    public String getTestabilityReason() {
        return testabilityReason;
    }
    
    /**
     * Sets the reason for testability status.
     * 
     * @param testabilityReason The testability reason
     */
    public void setTestabilityReason(String testabilityReason) {
        this.testabilityReason = testabilityReason;
    }
    
    @Override
    public String toString() {
        return "Criterion #" + (index + 1) + ": " + criterion + 
                " (Testable: " + testable + ", Reason: " + testabilityReason + ")";
    }
}