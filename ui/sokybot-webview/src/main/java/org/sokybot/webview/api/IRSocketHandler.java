package org.sokybot.webview.api;

import reactor.core.publisher.Mono;

/**
 * Handler interface for RSocket request-response operations.
 * 
 * Implementations should be registered as OSGi services with the 
 * "rsocket.method" property specifying the method name they handle.
 * 
 * Example:
 * <pre>
 * {@code
 * @Component(property = IRSocketHandler.METHOD_PROPERTY + "=character.state")
 * public class CharacterStateHandler implements IRSocketHandler {
 *     @Override
 *     public String getMethod() {
 *         return "character.state";
 *     }
 *     
 *     @Override
 *     public Mono<RSocketResponse> handle(RSocketRequest request) {
 *         // Handle the request
 *         return Mono.just(RSocketResponse.success(data));
 *     }
 * }
 * }
 * </pre>
 */
public interface IRSocketHandler {
    
    /**
     * OSGi service property key for the method name.
     */
    String METHOD_PROPERTY = "rsocket.method";
    
    /**
     * Get the method name this handler responds to.
     * This should match the "method" field in the RSocketRequest.
     * 
     * @return the method name (e.g., "character.state", "machine.start")
     */
    String getMethod();
    
    /**
     * Handle an RSocket request.
     * 
     * @param request the incoming request
     * @return a Mono containing the response
     */
    Mono<RSocketResponse> handle(RSocketRequest request);
    
    /**
     * Get a description of this handler for documentation/debugging.
     * 
     * @return handler description
     */
    default String getDescription() {
        return "Handler for " + getMethod();
    }
}
