package org.sokybot.machinepages.api;

import java.util.Map;
import org.sokybot.runtime.IMachineContext;
import reactor.core.publisher.Flux;

/**
 * Interface for scripted UI pages loaded from Groovy.
 */
public interface IScriptedPage {

    /**
     * Initialize the page with machine context.
     * 
     * @param context the machine context
     */
    void init(IMachineContext context);

    /**
     * Get the display title for the page.
     */
    String getTitle();

    /**
     * Get the icon name (Lucide) for the page.
     */
    String getIcon();

    /**
     * Get the UI schema for the page.
     */
    Map<String, Object> getSchema();

    /**
     * Get the initial state for the page.
     */
    Map<String, Object> getInitialState();

    /**
     * Handle a user action from the UI.
     * 
     * @param action the action name
     * @param data   the action data
     * @return response data and state updates
     */
    Map<String, Object> handleAction(String action, Map<String, Object> data);

    /**
     * Handle a data stream request.
     * 
     * @param streamId the stream identifier
     * @param params   stream parameters
     * @return a Flux of data updates
     */
    Flux<Map<String, Object>> streamData(String streamId, Map<String, Object> params);

    /**
     * Shutdown the page and release resources.
     */
    default void shutdown() {
    }
}
