package org.sokybot.packetsniffer.api;

import java.util.Map;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;

/**
 * Contract for the packet sniffer UI backend for one machine.
 * Used by scripted pages (PacketSniffer, PacketAnalyzer) to delegate state, actions, and streams.
 */
public interface IPacketSnifferPage {

    /**
     * Return current state for initial render or refresh (replaces handleSchemaRequest semantics).
     */
    Map<String, Object> getState();

    /**
     * Handle a user action from the UI.
     *
     * @param action the action name
     * @param data   the action data
     * @return response map with success, state updates, etc.
     */
    Map<String, Object> handleAction(String action, Map<String, Object> data);

    /**
     * Stream of real-time packet updates.
     */
    Flux<Map<String, Object>> streamPackets(Map<String, Object> params);

    /**
     * Stream of statistics updates (e.g. packet count, tracer count).
     */
    Flux<Map<String, Object>> streamStatistics(Map<String, Object> params);

    /**
     * Register a listener for state changes (e.g. tab switch, packet count) so the frontend can be pushed updates.
     */
    void addStateChangeListener(Consumer<Map<String, Object>> listener);

    /**
     * Return analyzer state for the Packet Analyzer scripted page; empty if analyzer is not open.
     */
    Map<String, Object> getAnalyzerState();
}
