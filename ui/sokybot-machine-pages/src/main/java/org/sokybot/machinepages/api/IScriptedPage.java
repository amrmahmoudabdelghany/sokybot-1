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
     * Return the OSGi event topics this page wants to receive, scoped to
     * a specific machine. Implementations that also implement
     * {@link org.osgi.service.event.EventHandler} will have
     * {@code handleEvent} called for matching events.
     *
     * @param machineFullName the machine identifier (e.g. "group.machine")
     * @return event topic strings, or empty array for no subscriptions
     */
    default String[] getEventTopics(String machineFullName) {
        return new String[0];
    }

    /**
     * Shutdown the page and release resources.
     */
    default void shutdown() {
    }
}
