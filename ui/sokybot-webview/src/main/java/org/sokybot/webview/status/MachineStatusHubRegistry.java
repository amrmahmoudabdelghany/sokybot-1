package org.sokybot.webview.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.commons.osgi.ServiceHandle;
import org.sokybot.commons.topic.Topic;
import org.sokybot.http.server.events.BridgeEvent;
import org.sokybot.http.server.events.IEventBridge;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.webview.util.MachineResolver;

import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Per-machine status hubs (replay + live from EventBridge) and a single wildcard fan-out for
 * all-machine subscribers. Hubs are tied to machine lifecycle (OSGi context events), not RSocket refcount.
 */
@Component(immediate = true, service = { MachineStatusHubRegistry.class, EventHandler.class }, property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED,
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED
})
public class MachineStatusHubRegistry implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(MachineStatusHubRegistry.class);

    static final int PER_CONNECTION_BUFFER = 100;
    private static final long HEARTBEAT_SECONDS = 10L;

    private final Object hubLock = new Object();
    private final Map<String, MachineStatusHub> hubs = new ConcurrentHashMap<>();

    private final List<Sinks.Many<Map<String, Object>>> wildcardClients = new CopyOnWriteArrayList<>();
    private final Object wildcardLock = new Object();
    private volatile IEventBridge.Subscription wildcardBridgeSubscription;

    private final ServiceHandle<IEventBridge> eventBridge = ServiceHandle.create();
    private volatile ISokybotContext sokybotContext;

    private ScheduledExecutorService scheduler;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetEventBridge")
    protected void setEventBridge(IEventBridge eventBridge) {
        this.eventBridge.bind(eventBridge);
        tryBootstrap();
    }

    protected void unsetEventBridge(IEventBridge bridge) {
        this.eventBridge.unbind(bridge);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
        tryBootstrap();
    }

    protected void unsetSokybotContext(ISokybotContext ctx) {
        this.sokybotContext = null;
    }

    @Activate
    protected void activate() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "machine-status-hub");
            t.setDaemon(true);
            return t;
        });
    }

    @Deactivate
    protected void deactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        synchronized (wildcardLock) {
            if (wildcardBridgeSubscription != null) {
                wildcardBridgeSubscription.unsubscribe();
                wildcardBridgeSubscription = null;
            }
        }
        for (Sinks.Many<Map<String, Object>> c : wildcardClients) {
            c.tryEmitComplete();
        }
        wildcardClients.clear();
        for (MachineStatusHub hub : hubs.values()) {
            hub.forceShutdownQuiet();
        }
        hubs.clear();
    }

    private void tryBootstrap() {
        IEventBridge bridge = this.eventBridge.tryGet().orElse(null);
        ISokybotContext ctx = this.sokybotContext;
        if (bridge == null || ctx == null) {
            return;
        }
        for (IGroupContext g : ctx.getGroups()) {
            for (IMachineContext m : g.getMachines()) {
                getOrCreateHub(m.fullName());
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (event == null) {
            return;
        }
        String topic = event.getTopic();
        String fullName = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
        if (fullName == null || fullName.isEmpty()) {
            return;
        }
        if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED.equals(topic)) {
            getOrCreateHub(fullName);
        } else if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED.equals(topic)) {
            destroyHub(fullName);
        }
    }

    /**
     * Hot flux of status maps for one machine; multiple RSocket subscribers share the same hub replay.
     */
    public Flux<Map<String, Object>> machineStatusFlux(String fullName) {
        MachineStatusHub hub = getOrCreateHub(fullName);
        if (hub == null) {
            return Flux.empty();
        }
        return hub.asFlux()
                .onBackpressureBuffer(PER_CONNECTION_BUFFER, BufferOverflowStrategy.DROP_OLDEST);
    }

    /**
     * Wildcard stream: one EventBridge subscription fans out to this connection's sink.
     */
    public Flux<Map<String, Object>> wildcardStatusFlux() {
        IEventBridge bridge = this.eventBridge.tryGet().orElse(null);
        if (bridge == null) {
            return Flux.empty();
        }
        Sinks.Many<Map<String, Object>> clientSink = Sinks.many().multicast()
                .onBackpressureBuffer(PER_CONNECTION_BUFFER, false);
        synchronized (wildcardLock) {
            if (wildcardBridgeSubscription == null) {
                wildcardBridgeSubscription = bridge.subscribe(Topic.parse("sokybot.network.**"), this::onWildcardBridgeEvent);
            }
        }
        wildcardClients.add(clientSink);
        return clientSink.asFlux()
                .doFinally(sig -> {
                    wildcardClients.remove(clientSink);
                    clientSink.tryEmitComplete();
                    maybeStopWildcardBridge();
                });
    }

    private void onWildcardBridgeEvent(BridgeEvent<Object> event) {
        Map<String, Object> payload = MachineStatusPayloadHelper.fromBridgeEvent(event);
        if (payload.isEmpty()) {
            return;
        }
        for (Sinks.Many<Map<String, Object>> client : wildcardClients) {
            Sinks.EmitResult r = client.tryEmitNext(new LinkedHashMap<>(payload));
            if (r.isFailure()) {
                log.trace("Wildcard client sink emit failed: {}", r);
            }
        }
    }

    private void maybeStopWildcardBridge() {
        synchronized (wildcardLock) {
            if (wildcardClients.isEmpty() && wildcardBridgeSubscription != null) {
                wildcardBridgeSubscription.unsubscribe();
                wildcardBridgeSubscription = null;
            }
        }
    }

    public Optional<IMachineContext> resolveMachine(String machineId) {
        return MachineResolver.resolve(sokybotContext, null, machineId);
    }

    public MachineStatusHub getOrCreateHub(String fullName) {
        IEventBridge bridge = this.eventBridge.tryGet().orElse(null);
        if (bridge == null || fullName == null || fullName.isEmpty()) {
            return null;
        }
        MachineStatusHub existing = hubs.get(fullName);
        if (existing != null && !existing.isShutdown()) {
            return existing;
        }
        if (!resolveMachine(fullName).isPresent()) {
            return null;
        }
        synchronized (hubLock) {
            existing = hubs.get(fullName);
            if (existing != null && !existing.isShutdown()) {
                return existing;
            }
            if (!resolveMachine(fullName).isPresent()) {
                return null;
            }
            MachineStatusHub hub = new MachineStatusHub(fullName);
            hub.start(bridge, scheduler);
            hubs.put(fullName, hub);
            return hub;
        }
    }

    private void destroyHub(String fullName) {
        MachineStatusHub hub = hubs.remove(fullName);
        if (hub != null) {
            hub.shutdownRemoved();
        }
    }

    final class MachineStatusHub {
        private final String fullName;
        private final Sinks.Many<Map<String, Object>> sink = Sinks.many().replay().latest();
        private final AtomicBoolean shutdown = new AtomicBoolean(false);
        private volatile IEventBridge.Subscription bridgeSub;
        private volatile ScheduledFuture<?> heartbeatFuture;

        MachineStatusHub(String fullName) {
            this.fullName = fullName;
        }

        boolean isShutdown() {
            return shutdown.get();
        }

        void start(IEventBridge bridge, ScheduledExecutorService sched) {
            bridgeSub = bridge.subscribe(fullName, Topic.parse("sokybot.network.**"), this::onBridgeEvent);
            if (sched != null && !sched.isShutdown()) {
                heartbeatFuture = sched.scheduleAtFixedRate(this::emitHeartbeat, HEARTBEAT_SECONDS,
                        HEARTBEAT_SECONDS, TimeUnit.SECONDS);
            }
        }

        private void onBridgeEvent(BridgeEvent<Object> event) {
            if (shutdown.get()) {
                return;
            }
            Map<String, Object> payload = MachineStatusPayloadHelper.fromBridgeEvent(event);
            if (!payload.isEmpty()) {
                sink.tryEmitNext(payload);
            }
        }

        private void emitHeartbeat() {
            if (shutdown.get()) {
                return;
            }
            Map<String, Object> hb = Map.of(
                    "type", "heartbeat",
                    "machineId", fullName,
                    "timestamp", System.currentTimeMillis());
            sink.tryEmitNext(hb);
        }

        Flux<Map<String, Object>> asFlux() {
            return sink.asFlux();
        }

        void shutdownRemoved() {
            if (!shutdown.compareAndSet(false, true)) {
                return;
            }
            if (heartbeatFuture != null) {
                heartbeatFuture.cancel(false);
            }
            if (bridgeSub != null) {
                bridgeSub.unsubscribe();
            }
            Map<String, Object> terminal = Map.of(
                    "type", "MACHINE_REMOVED",
                    "machineId", fullName,
                    "timestamp", System.currentTimeMillis());
            sink.tryEmitNext(terminal);
            sink.tryEmitComplete();
        }

        void forceShutdownQuiet() {
            if (!shutdown.compareAndSet(false, true)) {
                return;
            }
            if (heartbeatFuture != null) {
                heartbeatFuture.cancel(false);
            }
            if (bridgeSub != null) {
                bridgeSub.unsubscribe();
            }
            sink.tryEmitComplete();
        }
    }
}
