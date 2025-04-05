package com.manorrock.assistant.cli.issue;

/**
 * Exception thrown when there is an issue parsing the input text into a structured issue.
 * <p>
 * This exception is used to indicate errors during the parsing process,
 * such as missing required fields or malformed input.
 * </p>
 * 
 * @author Manorrock Assistant
 */
public class IssueParsingException extends Exception {
    
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new IssueParsingException with a null message.
     */
    public IssueParsingException() {
        super();
    }

    /**
     * Constructs a new IssueParsingException with the specified message.
     *
     * @param message The error message
     */
    public IssueParsingException(String message) {
        super(message);
    }

    /**
     * Constructs a new IssueParsingException with the specified message and cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public IssueParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new IssueParsingException with the specified cause.
     *
     * @param cause The cause of the exception
     */
    public IssueParsingException(Throwable cause) {
        super(cause);
    }
}