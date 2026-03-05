package org.sokybot.logging.api;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

/**
 * Central streaming API for Sokybot logs.
 *
 * This service exposes three logical scopes:
 * <ul>
 *     <li>Machine-level logs (MACHINE + related GROUP entries)</li>
 *     <li>Group-level logs (GROUP + optionally SYSTEM entries)</li>
 *     <li>System-level logs (SYSTEM entries only)</li>
 * </ul>
 *
 * The concrete log record shape follows the spec:
 *
 * <pre>
 * {
 *   "id": "uuid-string",
 *   "timestamp": "2026-03-02T10:00:00.000Z",
 *   "level": "INFO | WARN | ERROR | DEBUG | TRACE",
 *   "category": "SYSTEM | GROUP | MACHINE",
 *   "source": "bundle-name or class-name",
 *   "thread": "thread-name",
 *   "message": "The log message content",
 *   "stackTrace": "Optional stack trace string for errors",
 *   "metadata": {
 *     "clientId": "...",
 *     "scriptName": "...",
 *     "feature": "...",
 *     "groupName": "..."
 *   }
 * }
 * </pre>
 */
public interface ILogStreamService {

    /**
     * Stream log entries for a specific machine.
     *
     * @param machineFullName fully qualified machine name (e.g. "G1.TestBot")
     * @param levelFilter     "ALL" or a single level (INFO/WARN/ERROR/DEBUG/TRACE)
     * @param featureFilter   "ALL" or a feature key (e.g. "training", "healing")
     * @param maxEntries      maximum number of buffered entries to include in the initial snapshot
     * @return Flux of maps following the log-entry spec
     */
    Flux<Map<String, Object>> getStream(String machineFullName,
                                        String levelFilter,
                                        String featureFilter,
                                        int maxEntries);

    /**
     * Stream log entries for a specific group (game).
     */
    Flux<Map<String, Object>> getStreamForGroup(String groupName,
                                                String levelFilter,
                                                int maxEntries);

    /**
     * Stream system-level log entries only.
     */
    Flux<Map<String, Object>> getStreamForSystem(String levelFilter, int maxEntries);

    /**
     * Optional helper for a one-shot snapshot (no streaming) at machine scope.
     */
    List<Map<String, Object>> getRecentEntries(String machineFullName,
                                               String levelFilter,
                                               String featureFilter,
                                               int limit);

    /**
     * Optional helper for a one-shot snapshot at group scope.
     */
    List<Map<String, Object>> getRecentEntriesForGroup(String groupName,
                                                       String levelFilter,
                                                       int limit);

    /**
     * Optional helper for a one-shot snapshot at system scope.
     */
    List<Map<String, Object>> getRecentEntriesForSystem(String levelFilter, int limit);
}

