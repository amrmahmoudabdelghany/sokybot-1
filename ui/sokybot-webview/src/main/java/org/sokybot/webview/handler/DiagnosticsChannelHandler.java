package org.sokybot.webview.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.http.server.events.BridgeEvent;
import org.sokybot.http.server.events.IEventBridge;
import org.sokybot.webview.api.IRSocketChannelHandler;
import org.sokybot.webview.api.RSocketRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Bidirectional RSocket channel: filtered {@link IEventBridge} events to the UI, plus inbound
 * commands to change pattern, machine scope, or pause/resume.
 */
@Component(service = IRSocketChannelHandler.class, property = IRSocketChannelHandler.CHANNEL_PROPERTY
        + "=diagnostics.stream")
public class DiagnosticsChannelHandler implements IRSocketChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticsChannelHandler.class);

    public static final String CHANNEL_NAME = "diagnostics.stream";

    private volatile IEventBridge eventBridge;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventBridge(IEventBridge eventBridge) {
        this.eventBridge = eventBridge;
    }

    protected void unsetEventBridge(IEventBridge eventBridge) {
        this.eventBridge = null;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public String getDescription() {
        return "Live filtered event-bridge stream with interactive filter commands";
    }

    @Override
    public Flux<Object> handleChannel(RSocketRequest initialRequest, Flux<RSocketRequest> inbound) {
        final String initialPattern = initialRequest != null ? initialRequest.getString("pattern", "sokybot/**") : "sokybot/**";
        final String initialMachine = nullToEmpty(initialRequest != null ? initialRequest.getString("machineId") : null);

        final AtomicReference<String> patternRef = new AtomicReference<>(initialPattern);
        final AtomicReference<String> machineRef = new AtomicReference<>(initialMachine);
        final AtomicBoolean paused = new AtomicBoolean(false);
        final AtomicReference<IEventBridge.Subscription> subscriptionRef = new AtomicReference<>();
        final AtomicReference<Runnable> reconnectRef = new AtomicReference<>();

        Flux<Map<String, Object>> eventFlux = Flux.<Map<String, Object>>create(sink -> {
            Consumer<BridgeEvent> forward = ev -> {
                if (paused.get()) {
                    return;
                }
                String p = patternRef.get();
                if (p == null || p.isEmpty()) {
                    p = "**";
                }
                if (!ev.matchesTopic(p)) {
                    return;
                }
                String m = machineRef.get();
                if (m != null && !m.isEmpty()) {
                    if (ev.getMachineId() == null || !m.equals(ev.getMachineId())) {
                        return;
                    }
                }
                sink.next(toEventMap(ev));
            };

            Runnable reconnect = () -> {
                IEventBridge.Subscription old = subscriptionRef.getAndSet(null);
                if (old != null) {
                    old.unsubscribe();
                }
                IEventBridge bridge = DiagnosticsChannelHandler.this.eventBridge;
                if (bridge == null || paused.get()) {
                    return;
                }
                String p = patternRef.get();
                if (p == null || p.isEmpty()) {
                    p = "**";
                }
                String m = machineRef.get();
                IEventBridge.Subscription s = (m == null || m.isEmpty())
                        ? bridge.subscribe(p, forward)
                        : bridge.subscribe(m, p, forward);
                subscriptionRef.set(s);
            };

            reconnectRef.set(reconnect);

            if (this.eventBridge == null) {
                sink.next(Map.of(
                        "type", "ERROR",
                        "message", "event_bridge_unavailable"));
            } else {
                reconnect.run();
            }

            sink.onDispose(() -> {
                IEventBridge.Subscription s = subscriptionRef.getAndSet(null);
                if (s != null) {
                    s.unsubscribe();
                }
            });
        }).onBackpressureBuffer(512, dropped -> log.debug("Diagnostics channel dropped event under pressure"));

        Flux<Map<String, Object>> commandAcks = inbound.concatMap(req -> Mono.fromCallable(
                () -> applyCommand(req, patternRef, machineRef, paused, subscriptionRef, reconnectRef.get())));

        return eventFlux
                .mergeWith(commandAcks)
                .cast(Object.class)
                .doFinally(sig -> {
                    IEventBridge.Subscription s = subscriptionRef.getAndSet(null);
                    if (s != null) {
                        s.unsubscribe();
                    }
                    log.debug("Diagnostics channel closed ({})", sig);
                });
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private Map<String, Object> applyCommand(
            RSocketRequest req,
            AtomicReference<String> patternRef,
            AtomicReference<String> machineRef,
            AtomicBoolean paused,
            AtomicReference<IEventBridge.Subscription> subscriptionRef,
            Runnable reconnect) {
        Map<String, Object> params = req.getParams();
        String action = params != null && params.get("action") != null
                ? String.valueOf(params.get("action"))
                : "";
        switch (action) {
            case "setPattern": {
                String p = req.getString("pattern", "**");
                patternRef.set(p);
                if (reconnect != null) {
                    reconnect.run();
                }
                return ack("setPattern", true, Map.of("pattern", p));
            }
            case "setMachine": {
                String m = req.getString("machineId", "");
                machineRef.set(m);
                if (reconnect != null) {
                    reconnect.run();
                }
                return ack("setMachine", true, Map.of("machineId", m));
            }
            case "pause":
                paused.set(true);
                IEventBridge.Subscription s = subscriptionRef.getAndSet(null);
                if (s != null) {
                    s.unsubscribe();
                }
                return ack("pause", true, Map.of());
            case "resume":
                paused.set(false);
                if (reconnect != null) {
                    reconnect.run();
                }
                return ack("resume", true, Map.of());
            case "ping":
                return ack("ping", true, Map.of("ts", System.currentTimeMillis()));
            default:
                return Map.of(
                        "type", "diagnostics.command",
                        "action", action,
                        "ok", false,
                        "error", "unknown_action");
        }
    }

    private static Map<String, Object> ack(String action, boolean ok, Map<String, Object> extra) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "diagnostics.command");
        m.put("action", action);
        m.put("ok", ok);
        m.putAll(extra);
        return m;
    }

    private static Map<String, Object> toEventMap(BridgeEvent ev) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "bridge.event");
        m.put("topic", ev.getTopic());
        m.put("machineId", ev.getMachineId());
        m.put("timestamp", ev.getTimestamp().toEpochMilli());
        Object p = ev.getPayload();
        if (p instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> asMap = (Map<String, Object>) p;
            m.put("payload", asMap);
        } else {
            m.put("payload", p != null ? String.valueOf(p) : null);
        }
        return m;
    }
}
