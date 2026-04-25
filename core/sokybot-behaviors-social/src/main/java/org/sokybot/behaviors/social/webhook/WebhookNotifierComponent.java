package org.sokybot.behaviors.social.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.social.api.ISocialModel;
import org.sokybot.social.api.SocialAlert;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

/**
 * Long-lived subscriber to {@link ISocialModel#observeAllAlerts()} that POSTs JSON payloads to configured sinks.
 */
@Component(immediate = true, service = WebhookNotifierComponent.class)
public class WebhookNotifierComponent {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifierComponent.class);

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Long> lastSentByKey = new ConcurrentHashMap<>();

    private volatile ISocialModel socialModel;
    private volatile ISettingsRegistry settingsRegistry;
    private volatile Disposable subscription;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void bindSocialModel(ISocialModel model) {
        this.socialModel = model;
        resubscribe();
    }

    protected void unbindSocialModel(ISocialModel model) {
        if (this.socialModel == model) {
            this.socialModel = null;
        }
        resubscribe();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void bindSettingsRegistry(ISettingsRegistry registry) {
        this.settingsRegistry = registry;
    }

    protected void unbindSettingsRegistry(ISettingsRegistry registry) {
        if (this.settingsRegistry == registry) {
            this.settingsRegistry = null;
        }
    }

    @Activate
    public void activate() {
        resubscribe();
    }

    @Deactivate
    public void deactivate() {
        Disposable s = subscription;
        subscription = null;
        if (s != null && !s.isDisposed()) {
            s.dispose();
        }
    }

    /**
     * Sends a test alert through configured sinks, skipping dedupe cooldown and global kind gating.
     */
    public void sendTestAlert(SocialAlert alert) {
        WebhookSettings cfg = resolveSettings(alert.getMachineId());
        if (cfg == null || !cfg.isEnabled()) {
            log.debug("Webhook test skipped: no settings or disabled for {}", alert.getMachineId());
            return;
        }
        List<WebhookSink> sinks = materializeSinks(cfg);
        for (WebhookSink sink : sinks) {
            postOne(sink, alert, cfg);
        }
    }

    private void resubscribe() {
        Disposable old = subscription;
        if (old != null && !old.isDisposed()) {
            old.dispose();
        }
        subscription = null;

        ISocialModel m = socialModel;
        if (m == null) {
            return;
        }

        subscription = m.observeAllAlerts()
                .onBackpressureBuffer(256)
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::dispatchAlert, err -> log.warn("Webhook stream error", err));
    }

    private void dispatchAlert(SocialAlert a) {
        try {
            WebhookSettings cfg = resolveSettings(a.getMachineId());
            if (cfg == null || !cfg.isEnabled()) {
                return;
            }
            if (a.getKind() == null) {
                return;
            }
            if (!cfg.getEnabledAlertKinds().contains(a.getKind().name())) {
                return;
            }
            if (cooldownTrips(a, cfg)) {
                return;
            }
            List<WebhookSink> sinks = materializeSinks(cfg);
            for (WebhookSink sink : sinks) {
                if (a.getKind() != null && !sink.allowsKind(a.getKind().name())) {
                    continue;
                }
                postOne(sink, a, cfg);
            }
        } catch (RuntimeException e) {
            log.warn("Webhook dispatchAlert error", e);
        }
    }

    private boolean cooldownTrips(SocialAlert a, WebhookSettings cfg) {
        String key = dedupeKey(a);
        long now = System.currentTimeMillis();
        long cool = Math.max(0L, cfg.getDedupeCooldownMs());
        Long last = lastSentByKey.get(key);
        if (last != null && cool > 0L && (now - last) < cool) {
            return true;
        }
        lastSentByKey.put(key, Long.valueOf(now));
        return false;
    }

    private static String dedupeKey(SocialAlert a) {
        String kind = a.getKind() != null ? a.getKind().name() : "";
        return a.getMachineId() + ":" + kind + ":" + a.getSubject();
    }

    private WebhookSettings resolveSettings(String machineFullName) {
        ISettingsRegistry reg = settingsRegistry;
        if (reg == null || machineFullName == null || machineFullName.isEmpty()) {
            return null;
        }
        String[] gm = splitMachine(machineFullName);
        try {
            ISettingsProvider<WebhookSettings> p = reg.getProvider(gm[0], gm[1], "webhooks", WebhookSettings.class);
            if (p == null) {
                return null;
            }
            return p.get();
        } catch (RuntimeException ex) {
            log.debug("No webhook settings for {}: {}", machineFullName, ex.toString());
            return null;
        }
    }

    private static String[] splitMachine(String fullName) {
        int dot = fullName.indexOf('.');
        if (dot <= 0 || dot == fullName.length() - 1) {
            return new String[] { "default", fullName };
        }
        return new String[] { fullName.substring(0, dot), fullName.substring(dot + 1) };
    }

    private static List<WebhookSink> materializeSinks(WebhookSettings cfg) {
        List<WebhookSink> out = new ArrayList<>();
        if (cfg.getSinks() == null) {
            return out;
        }
        for (WebhookSettings.WebhookSinkConfig c : cfg.getSinks()) {
            if (c == null || c.getUrl() == null || c.getUrl().trim().isEmpty()) {
                continue;
            }
            WebhookSink.Kind k = parseSinkKind(c.getKind());
            out.add(new WebhookSink(k, c.getUrl().trim(), c.getAuthToken(),
                    c.getKindFilter() != null ? c.getKindFilter() : Collections.emptySet()));
        }
        return out;
    }

    private static WebhookSink.Kind parseSinkKind(String raw) {
        if (raw == null) {
            return WebhookSink.Kind.GENERIC_JSON;
        }
        try {
            return WebhookSink.Kind.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return WebhookSink.Kind.GENERIC_JSON;
        }
    }

    private void postOne(WebhookSink sink, SocialAlert a, WebhookSettings cfg) {
        try {
            String body = bodyFor(sink.getKind(), a, sink.getAuthToken());
            HttpRequest req = HttpRequest.newBuilder(URI.create(sink.getUrl()))
                    .timeout(Duration.ofSeconds(Math.max(1, cfg.getRequestTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Sokybot/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            log.warn("Webhook POST failed: {}", sink.getUrl(), err);
                        } else if (resp != null && resp.statusCode() >= 400) {
                            log.warn("Webhook {} returned {}", sink.getUrl(), Integer.valueOf(resp.statusCode()));
                        }
                    });
        } catch (Exception e) {
            log.warn("Webhook dispatch error", e);
        }
    }

    private String bodyFor(WebhookSink.Kind sinkKind, SocialAlert a, String authToken) throws Exception {
        switch (sinkKind) {
            case DISCORD:
                return WebhookPayloadBuilder.discord(mapper, a);
            case TELEGRAM:
                return WebhookPayloadBuilder.telegram(mapper, a, authToken);
            case GENERIC_JSON:
            default:
                return WebhookPayloadBuilder.genericJson(mapper, a);
        }
    }
}
