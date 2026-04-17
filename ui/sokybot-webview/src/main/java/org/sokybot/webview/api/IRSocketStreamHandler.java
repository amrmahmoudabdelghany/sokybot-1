package org.sokybot.webview.api;

import reactor.core.publisher.Flux;

/**
 * Handler interface for RSocket request-stream operations.
 * 
 * Implementations should be registered as OSGi services with the 
 * "rsocket.stream" property specifying the stream name they handle.
 * 
 * Example:
 * <pre>
 * {@code
 * @Component(property = IRSocketStreamHandler.STREAM_PROPERTY + "=game.events")
 * public class GameEventsStreamHandler implements IRSocketStreamHandler {
 *     @Override
 *     public String getStreamName() {
 *         return "game.events";
 *     }
 *     
 *     @Override
 *     public Flux<Object> handleStream(RSocketRequest request) {
 *         return eventFlux.map(e -> e);
 *     }
 * }
 * }
 * </pre>
 */
public interface IRSocketStreamHandler {
    
    /**
     * OSGi service property key for the stream name.
     */
    String STREAM_PROPERTY = "rsocket.stream";
    String RANKING_PROPERTY = "rsocket.stream.ranking";
    
    /**
     * Get the stream name this handler responds to.
     * 
     * @return the stream name (e.g., "game.events", "character.updates")
     */
    String getStreamName();
    
    /**
     * Handle an RSocket stream request.
     * 
     * @param request the incoming request with optional parameters
     * @return a Flux of data objects to stream to the client
     */
    Flux<Object> handleStream(RSocketRequest request);

    default int getRanking() {
        return 0;
    }
    
    /**
     * Get a description of this stream handler.
     * 
     * @return handler description
     */
    default String getDescription() {
        return "Stream handler for " + getStreamName();
    }
}
