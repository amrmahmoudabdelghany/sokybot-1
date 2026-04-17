package org.sokybot.webview.api;

import reactor.core.publisher.Mono;

/**
 * Handler interface for RSocket fire-and-forget operations.
 */
public interface IRSocketFireAndForgetHandler {

    String METHOD_PROPERTY = "rsocket.fireAndForget";
    String RANKING_PROPERTY = "rsocket.fireAndForget.ranking";

    String[] getMethods();

    Mono<Void> handleFireAndForget(RSocketRequest request);

    default int getRanking() {
        return 0;
    }

    default String getDescription() {
        String[] methods = getMethods();
        if (methods == null || methods.length == 0) {
            return "Fire-and-forget handler";
        } else if (methods.length == 1) {
            return "Fire-and-forget handler for " + methods[0];
        }
        return "Fire-and-forget handler for " + String.join(", ", methods);
    }
}
