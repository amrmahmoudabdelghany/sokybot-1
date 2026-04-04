package org.sokybot.webview.handler;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.webview.api.IRSocketFireAndForgetHandler;
import org.sokybot.webview.api.RSocketRequest;

import reactor.core.publisher.Mono;

/**
 * Best-effort client telemetry on the RSocket connection (does not block UI).
 */
@Component(service = IRSocketFireAndForgetHandler.class, property = {
        IRSocketFireAndForgetHandler.METHOD_PROPERTY + "=webview.client.connected",
        IRSocketFireAndForgetHandler.METHOD_PROPERTY + "=webview.client.visibility"
})
public class WebviewClientFireAndForgetHandler implements IRSocketFireAndForgetHandler {

    private static final Logger log = LoggerFactory.getLogger(WebviewClientFireAndForgetHandler.class);

    @Override
    public String[] getMethods() {
        return new String[] { "webview.client.connected", "webview.client.visibility" };
    }

    @Override
    public Mono<Void> handleFireAndForget(RSocketRequest request) {
        String method = request.getMethod();
        if ("webview.client.connected".equals(method)) {
            String build = request.getString("uiBuildId", "");
            log.debug("Webview client connected uiBuildId={}", build);
            return Mono.empty();
        }
        if ("webview.client.visibility".equals(method)) {
            String state = request.getString("visibility", "");
            log.debug("Webview client visibility state={}", state);
            return Mono.empty();
        }
        return Mono.empty();
    }
}
