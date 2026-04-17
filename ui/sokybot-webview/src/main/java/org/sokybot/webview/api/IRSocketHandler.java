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
 *     public String[] getMethods() {
 *         return new String[] { "character.state" };
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
    String RANKING_PROPERTY = "rsocket.method.ranking";
    
    /**
     * Get the method names this handler responds to.
     * 
     * @return array of method names (e.g., ["character.state"] or ["machine.start", "machine.stop"])
     */
    String[] getMethods();
    
    /**
     * Handle an RSocket request.
     * 
     * @param request the incoming request
     * @return a Mono containing the response
     */
    Mono<RSocketResponse> handle(RSocketRequest request);

    default int getRanking() {
        return 0;
    }
    
    /**
     * Get a description of this handler for documentation/debugging.
     * 
     * @return handler description
     */
    default String getDescription() {
        String[] methods = getMethods();
        if (methods == null || methods.length == 0) {
            return "Handler";
        } else if (methods.length == 1) {
            return "Handler for " + methods[0];
        } else {
            return "Handler for " + String.join(", ", methods);
        }
    }
}
