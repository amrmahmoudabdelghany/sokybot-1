package org.sokybot.http.server.api;

import reactor.core.publisher.Mono;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

/**
 * Functional interface for handling WebSocket connections.
 * Bundles implement this to handle WebSocket traffic at registered paths.
 */
@FunctionalInterface
public interface IWebSocketHandler {

    /**
     * Handle an incoming WebSocket connection.
     * Called when a client connects to the registered path.
     * 
     * @param inbound  the Reactor Netty WebsocketInbound
     * @param outbound the Reactor Netty WebsocketOutbound
     * @return a Mono<Void> that completes when the connection is handled
     */
    Mono<Void> handle(WebsocketInbound inbound, WebsocketOutbound outbound);
}
