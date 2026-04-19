package org.sokybot.scripting.api;

/**
 * Thrown when a travel script source fails structural validation.
 */
public class ScriptParseException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int sourceLine;

    public ScriptParseException(int sourceLine, String message) {
        super(message);
        this.sourceLine = sourceLine;
    }

    public ScriptParseException(int sourceLine, String message, Throwable cause) {
        super(message, cause);
        this.sourceLine = sourceLine;
    }

    /** 1-based line number in the source where the error occurred, or 0 if unknown. */
    public int getSourceLine() {
        return sourceLine;
    }
}
