package org.sokybot.webview;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;

import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Live RSocket check against a running Sokybot HTTP/RSocket endpoint.
 * <p>
 * Disabled unless {@code -Dsokybot.rsocket.smoke.url=ws://host:port/rsocket} is set (e.g. CI after
 * deploy, or locally). Does not replace OSGi/Karaf integration tests.
 */
class RSocketWorkspaceSummarySmokeIT {

    @Test
    @EnabledIfSystemProperty(named = "sokybot.rsocket.smoke.url", matches = ".+")
    void workspaceSummaryReturnsResult() {
        String url = System.getProperty("sokybot.rsocket.smoke.url");
        RSocket rsocket = null;
        try {
            rsocket = RSocketConnector.create()
                    .connect(WebsocketClientTransport.create(URI.create(url)))
                    .block(Duration.ofSeconds(25));

            String body = rsocket
                    .requestResponse(
                            DefaultPayload.create("{\"method\":\"workspace.summary\",\"id\":\"smoke-1\"}"))
                    .map(p -> p.getDataUtf8())
                    .block(Duration.ofSeconds(20));

            assertNotNull(body);
            assertTrue(body.contains("\"result\""), body);
            assertFalse(body.contains("\"code\":-32601"), "METHOD_NOT_FOUND: " + body);
        } finally {
            if (rsocket != null) {
                rsocket.dispose();
            }
        }
    }
}
