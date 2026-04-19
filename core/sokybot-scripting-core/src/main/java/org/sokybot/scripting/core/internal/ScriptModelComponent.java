package org.sokybot.scripting.core.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.gameevents.events.teleport.TeleportCompleteEvent;
import org.sokybot.gameevents.events.teleport.TeleportResponseEvent;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.scripting.api.IScriptExecutionSnapshot;
import org.sokybot.scripting.api.IScriptModel;
import org.sokybot.scripting.api.IScriptParser;
import org.sokybot.scripting.api.ITravelScript;
import org.sokybot.scripting.api.ScriptExecutionSnapshot;
import org.sokybot.scripting.api.ScriptPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Reactive overlay keyed by machine full name; pairs with {@link FilesystemScriptRegistry} catalogue data.
 */
@Component(service = IScriptModel.class, immediate = true)
public final class ScriptModelComponent implements IScriptModel {

    private static final Logger log = LoggerFactory.getLogger(ScriptModelComponent.class);

    private static final long LOAD_SCREEN_STALE_AFTER_MS = 30_000L;
    private static final long TICK_MS = 250L;

    private final Map<String, MachineScriptState> stateByMachine = new ConcurrentHashMap<>();
    private final List<Disposable> subscriptions = new ArrayList<>();

    private final AtomicReference<FilesystemScriptRegistry> registry = new AtomicReference<>();

    private ScheduledExecutorService tickExecutor;
    private volatile ScheduledFuture<?> tickFuture;

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Reference
    private IScriptParser scriptParser;

    @Activate
    void activate() {
        FilesystemScriptRegistry reg = new FilesystemScriptRegistry(scriptParser);
        registry.set(reg);
        reg.start();

        tickExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "script-model-tick");
            t.setDaemon(true);
            return t;
        });
        tickFuture = tickExecutor.scheduleAtFixedRate(this::onTick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);

        subscriptions.add(reactiveEventBus.on(TeleportResponseEvent.class).subscribe(this::onTeleportResponse));
        subscriptions.add(reactiveEventBus.on(TeleportCompleteEvent.class).subscribe(this::onTeleportComplete));
        subscriptions.add(reactiveEventBus.on(EntityMovementEvent.class).subscribe(this::onEntityMovement));
        subscriptions.add(reactiveEventBus.on(CharacterLoadedEvent.class).subscribe(this::onCharacterLoaded));

        log.debug("IScriptModel projection active; scripts dir {}", FilesystemScriptRegistry.resolveScriptDir());
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        if (tickExecutor != null) {
            tickExecutor.shutdownNow();
        }
        FilesystemScriptRegistry reg = registry.getAndSet(null);
        if (reg != null) {
            reg.stop();
        }
        stateByMachine.clear();
    }

    private void onTick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, MachineScriptState> e : stateByMachine.entrySet()) {
            MachineScriptState st = e.getValue();
            if (st.scriptId == null) {
                continue;
            }
            if (st.phase == ScriptPhase.SLEEPING && now >= st.sleepUntilEpochMs) {
                st.cursor = Math.addExact(st.cursor, 1);
                st.phase = ScriptPhase.IDLE;
                st.walkTarget = null;
                st.phaseStartedAtEpochMs = now;
                publishSnapshot(e.getKey(), st);
            } else if (st.phase == ScriptPhase.WAITING_LOAD_SCREEN
                    && now - st.phaseStartedAtEpochMs > LOAD_SCREEN_STALE_AFTER_MS) {
                st.phase = ScriptPhase.ERROR;
                st.lastError = "load-screen-timeout";
                st.phaseStartedAtEpochMs = now;
                publishSnapshot(e.getKey(), st);
            }
        }
    }

    private void onTeleportResponse(TeleportResponseEvent ev) {
        String key = normalize(ev.getFullName());
        if (key == null) {
            return;
        }
        MachineScriptState st = stateByMachine.get(key);
        if (st == null || st.scriptId == null) {
            return;
        }
        if (st.phase != ScriptPhase.WAITING_TELEPORT_ACK) {
            return;
        }
        if (!ev.isSuccess()) {
            st.phase = ScriptPhase.ERROR;
            st.lastError = "teleport-response-failed";
            st.phaseStartedAtEpochMs = System.currentTimeMillis();
            publishSnapshot(key, st);
            return;
        }
        st.phase = ScriptPhase.WAITING_LOAD_SCREEN;
        st.phaseStartedAtEpochMs = System.currentTimeMillis();
        publishSnapshot(key, st);
    }

    private void onTeleportComplete(TeleportCompleteEvent ev) {
        String key = normalize(ev.getFullName());
        if (key == null) {
            return;
        }
        MachineScriptState st = stateByMachine.get(key);
        if (st == null || st.scriptId == null) {
            return;
        }
        if (st.phase != ScriptPhase.WAITING_LOAD_SCREEN) {
            return;
        }
        long now = System.currentTimeMillis();
        st.cursor = Math.addExact(st.cursor, 1);
        st.phase = ScriptPhase.IDLE;
        st.phaseStartedAtEpochMs = now;
        st.walkTarget = null;
        publishSnapshot(key, st);
    }

    private void onCharacterLoaded(CharacterLoadedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineScriptState st = stateByMachine.get(key);
        if (st == null) {
            return;
        }
        st.selfEntityId = e.getUniqueId();
    }

    private void onEntityMovement(EntityMovementEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineScriptState st = stateByMachine.get(key);
        if (st == null || st.scriptId == null) {
            return;
        }
        if (st.phase != ScriptPhase.WALKING && st.phase != ScriptPhase.WAITING_PORTAL_ARRIVAL) {
            return;
        }
        if (st.walkTarget == null) {
            return;
        }
        Integer self = st.selfEntityId;
        if (self == null || e.getEntityId() != self.intValue()) {
            return;
        }
        GamePosition cp = e.getCurrentPosition();
        if (cp == null) {
            return;
        }
        WorldPoint here = new WorldPoint(cp.getX(), cp.getY(), cp.getZ());
        float tol = st.arrivalToleranceWorldUnits;
        if (st.walkTarget.distanceTo(here) > tol) {
            return;
        }
        long now = System.currentTimeMillis();
        st.cursor = Math.addExact(st.cursor, 1);
        st.phase = ScriptPhase.IDLE;
        st.walkTarget = null;
        st.phaseStartedAtEpochMs = now;
        publishSnapshot(key, st);
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private MachineScriptState stateFor(String key) {
        return stateByMachine.computeIfAbsent(key, k -> new MachineScriptState());
    }

    private void publishSnapshot(String key, MachineScriptState st) {
        if (key == null || st == null) {
            return;
        }
        IScriptExecutionSnapshot snap = buildSnapshot(st);
        st.sink.emitNext(snap, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private IScriptExecutionSnapshot buildSnapshot(MachineScriptState st) {
        return new ScriptExecutionSnapshot(st.scriptId, st.cursor, st.phase, st.phaseStartedAtEpochMs, st.lastError);
    }

    @Override
    public Optional<ITravelScript> findById(String id) {
        FilesystemScriptRegistry reg = registry.get();
        return reg == null ? Optional.empty() : reg.find(id);
    }

    @Override
    public Collection<ITravelScript> listAvailable() {
        FilesystemScriptRegistry reg = registry.get();
        return reg == null ? List.of() : reg.list();
    }

    @Override
    public Optional<IScriptExecutionSnapshot> snapshot(String machineId) {
        String key = normalize(machineId);
        if (key == null) {
            return Optional.empty();
        }
        MachineScriptState st = stateByMachine.get(key);
        if (st == null) {
            return Optional.empty();
        }
        return Optional.of(buildSnapshot(st));
    }

    @Override
    public Flux<IScriptExecutionSnapshot> observe(String machineId) {
        String key = normalize(machineId);
        if (key == null) {
            return Flux.empty();
        }
        MachineScriptState st = stateFor(key);
        Flux<IScriptExecutionSnapshot> tail = st.sink.asFlux().sample(Duration.ofMillis(50));
        Optional<IScriptExecutionSnapshot> seed = snapshot(machineId);
        if (seed.isPresent()) {
            return Flux.concat(Flux.just(seed.get()), tail);
        }
        return tail;
    }

    @Override
    public void reset(String machineId) {
        String key = normalize(machineId);
        if (key == null) {
            return;
        }
        stateByMachine.remove(key);
    }

    @Override
    public void bindActiveScript(String machineId, String scriptId, float arrivalToleranceWorldUnits) {
        String key = normalize(machineId);
        if (key == null) {
            return;
        }
        MachineScriptState st = stateFor(key);
        boolean changed = !Objects.equals(st.scriptId, scriptId)
                || Float.compare(st.arrivalToleranceWorldUnits, arrivalToleranceWorldUnits) != 0;
        if (changed) {
            st.scriptId = scriptId;
            st.arrivalToleranceWorldUnits = arrivalToleranceWorldUnits;
            st.cursor = 0;
            st.phase = ScriptPhase.IDLE;
            st.lastError = null;
            st.walkTarget = null;
            long now = System.currentTimeMillis();
            st.phaseStartedAtEpochMs = now;
            publishSnapshot(key, st);
        }
    }

    @Override
    public void onTeleportStepIssued(String machineId) {
        String key = normalize(machineId);
        if (key == null) {
            return;
        }
        MachineScriptState st = stateFor(key);
        if (st.scriptId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        st.phase = ScriptPhase.WAITING_TELEPORT_ACK;
        st.phaseStartedAtEpochMs = now;
        st.teleportRequestedAtEpochMs = now;
        st.walkTarget = null;
        publishSnapshot(key, st);
    }

    @Override
    public void onWalkProbeBound(String machineId, WorldPoint target, boolean portalArrival) {
        String key = normalize(machineId);
        if (key == null || target == null) {
            return;
        }
        MachineScriptState st = stateFor(key);
        if (st.scriptId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        st.walkTarget = target;
        st.portalStyleArrival = portalArrival;
        st.phase = portalArrival ? ScriptPhase.WAITING_PORTAL_ARRIVAL : ScriptPhase.WALKING;
        st.phaseStartedAtEpochMs = now;
        publishSnapshot(key, st);
    }

    @Override
    public void onWaitScheduled(String machineId, long waitMillis) {
        String key = normalize(machineId);
        if (key == null) {
            return;
        }
        MachineScriptState st = stateFor(key);
        if (st.scriptId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        st.phase = ScriptPhase.SLEEPING;
        st.phaseStartedAtEpochMs = now;
        st.sleepUntilEpochMs = now + Math.max(0L, waitMillis);
        publishSnapshot(key, st);
    }

    @Override
    public void onLogLineExecuted(String machineId) {
        String key = normalize(machineId);
        if (key == null) {
            return;
        }
        MachineScriptState st = stateFor(key);
        if (st.scriptId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        st.cursor = Math.addExact(st.cursor, 1);
        st.phase = ScriptPhase.IDLE;
        st.phaseStartedAtEpochMs = now;
        publishSnapshot(key, st);
    }
}
