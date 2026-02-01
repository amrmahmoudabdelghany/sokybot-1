package org.sokybot.engine.api.scripting;

/**
 * Exception thrown when script execution fails.
 */
public class ScriptException extends RuntimeException {

    private final String errorType;
    private final int lineNumber;

    public ScriptException(String message) {
        super(message);
        this.errorType = "General";
        this.lineNumber = -1;
    }

    public ScriptException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = "Execution";
        this.lineNumber = -1;
    }

    public ScriptException(String message, String errorType, int lineNumber, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.lineNumber = lineNumber;
    }

    public String getErrorType() {
        return errorType;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
