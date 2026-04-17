package org.sokybot.webview.api;

import reactor.core.publisher.Flux;

/**
 * Handler interface for RSocket request-channel operations.
 */
public interface IRSocketChannelHandler {

    String CHANNEL_PROPERTY = "rsocket.channel";
    String RANKING_PROPERTY = "rsocket.channel.ranking";

    String getChannelName();

    Flux<Object> handleChannel(RSocketRequest initialRequest, Flux<RSocketRequest> inbound);

    default int getRanking() {
        return 0;
    }

    default String getDescription() {
        return "Channel handler for " + getChannelName();
    }
}
