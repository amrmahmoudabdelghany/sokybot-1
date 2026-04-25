package org.sokybot.webview.handler.social;

import java.util.Collections;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.behaviors.social.webhook.WebhookNotifierComponent;
import org.sokybot.social.api.SocialAlert;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * Dispatches a synthetic {@link SocialAlert} through {@link WebhookNotifierComponent} for connectivity tests.
 */
@Component(service = IRSocketHandler.class, property = IRSocketHandler.METHOD_PROPERTY + "=social.webhook.test")
public class WebhookTestHandler implements IRSocketHandler {

    private volatile WebhookNotifierComponent notifier;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void bindNotifier(WebhookNotifierComponent notifier) {
        this.notifier = notifier;
    }

    protected void unbindNotifier(WebhookNotifierComponent notifier) {
        if (this.notifier == notifier) {
            this.notifier = null;
        }
    }

    @Override
    public String[] getMethods() {
        return new String[] { "social.webhook.test" };
    }

    @Override
    public String getDescription() {
        return "POST a test webhook payload for the given machine (bypasses dedupe cooldown)";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        WebhookNotifierComponent n = notifier;
        if (n == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Webhook notifier not available"));
        }
        String machineId = request.getString("machineId");
        if (machineId == null || machineId.isBlank()) {
            return Mono.just(RSocketResponse.invalidParams("machineId is required"));
        }
        SocialAlert test = new SocialAlert(
                machineId.trim(),
                System.currentTimeMillis(),
                SocialAlert.Kind.CUSTOM,
                "Test from Sokybot",
                Collections.emptyMap());
        n.sendTestAlert(test);
        return Mono.just(RSocketResponse.success(Map.of("status", "dispatched", "machineId", machineId.trim())));
    }
}
