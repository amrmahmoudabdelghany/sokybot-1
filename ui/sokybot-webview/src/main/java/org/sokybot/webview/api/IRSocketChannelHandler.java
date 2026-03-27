package org.sokybot.webview.api;

import reactor.core.publisher.Flux;

/**
 * Handler interface for RSocket request-channel operations.
 */
public interface IRSocketChannelHandler {

    String CHANNEL_PROPERTY = "rsocket.channel";

    String getChannelName();

    Flux<Object> handleChannel(RSocketRequest initialRequest, Flux<RSocketRequest> inbound);

    default String getDescription() {
        return "Channel handler for " + getChannelName();
    }
}
