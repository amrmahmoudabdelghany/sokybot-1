package org.sokybot.engine.api.workflow;

/**
 * Exception thrown during workflow execution (guard evaluation or action execution).
 * Engine handles these exceptions according to error handling policy.
 */
public class WorkflowException extends RuntimeException {
    
    public WorkflowException(String message) {
        super(message);
    }
    
    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
