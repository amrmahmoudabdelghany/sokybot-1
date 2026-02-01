package org.sokybot.commons.error;

import java.util.List;

import reactor.core.publisher.Flux;

/**
 * Central registry for capturing and querying application errors.
 * 
 * Implementations store recent errors in memory and provide a reactive
 * stream for real-time error notifications.
 * 
 * Example usage:
 * 
 * <pre>
 * {@code @Reference
 * private IErrorRegistry errorRegistry;
 * 
 * public void doOperation() {
 *     try {
 *         // operation
 *     } catch (Exception e) {
 *         errorRegistry.recordError(ErrorInfo.fromException(
 *                 ErrorCategory.ENGINE, "MyComponent", e));
 *     }
 * }
 * }
 * </pre>
 */
public interface IErrorRegistry {

    /**
     * Record an error in the registry.
     * 
     * @param error the error information to record
     */
    void recordError(ErrorInfo error);

    /**
     * Record an error from an exception.
     * Convenience method that creates ErrorInfo from the exception.
     * 
     * @param category  error category
     * @param source    source component name
     * @param exception the exception that occurred
     */
    default void recordError(ErrorCategory category, String source, Throwable exception) {
        recordError(ErrorInfo.fromException(category, source, exception));
    }

    /**
     * Record an error from an exception with machine context.
     * 
     * @param category  error category
     * @param source    source component name
     * @param machineId machine identifier for context
     * @param exception the exception that occurred
     */
    default void recordError(ErrorCategory category, String source, String machineId, Throwable exception) {
        recordError(ErrorInfo.fromException(category, source, machineId, exception));
    }

    /**
     * Get recent errors, most recent first.
     * 
     * @param limit maximum number of errors to return
     * @return list of recent errors
     */
    List<ErrorInfo> getRecentErrors(int limit);

    /**
     * Get a reactive stream of errors.
     * New subscribers will receive errors as they occur.
     * 
     * @return Flux of error information
     */
    Flux<ErrorInfo> getErrorStream();

    /**
     * Clear all stored errors.
     */
    void clearErrors();

    /**
     * Get the count of stored errors.
     * 
     * @return number of stored errors
     */
    int getErrorCount();
}
