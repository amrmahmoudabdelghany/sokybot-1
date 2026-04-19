package org.sokybot.session.projections.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.session.CaptchaChallengeEvent;
import org.sokybot.gameevents.events.session.CaptchaResolvedEvent;
import org.sokybot.gameevents.events.session.LoginPhaseEvent;
import org.sokybot.gameevents.events.session.SessionConnectedEvent;
import org.sokybot.gameevents.events.session.SessionDisconnectedEvent;
import org.sokybot.gameevents.events.session.SessionReconnectingEvent;
import org.sokybot.session.api.CaptchaPrompt;
import org.sokybot.session.api.ISessionModel;
import org.sokybot.session.api.ISessionSnapshot;
import org.sokybot.session.api.SessionPhase;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Reactive session state projection sourced from {@link IReactiveEventBus};
 * keyed by machine full name.
 * <p>
 * Follows the same pattern as {@code CombatModelComponent}: subscribes to
 * typed game events in {@code @Activate}, maintains per-machine mutable
 * state, and emits immutable {@link ISessionSnapshot} instances via
 * a per-machine {@link Sinks.Many}.
 */
@Component(service = ISessionModel.class, immediate = true)
public final class SessionModelComponent implements ISessionModel {

    private static final Logger log = LoggerFactory.getLogger(SessionModelComponent.class);

    private final Map<String, MachineSessionState> stateByMachine = new ConcurrentHashMap<>();
    private final List<Disposable> subscriptions = new ArrayList<>();

    /** Global sink for {@link #observeAll()} subscribers. */
    private final Sinks.Many<ISessionSnapshot> globalSink =
            Sinks.many().multicast().onBackpressureBuffer(64, false);

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
        subscriptions.add(reactiveEventBus.on(SessionConnectedEvent.class).subscribe(this::onSessionConnected));
        subscriptions.add(reactiveEventBus.on(SessionDisconnectedEvent.class).subscribe(this::onSessionDisconnected));
        subscriptions.add(reactiveEventBus.on(SessionReconnectingEvent.class).subscribe(this::onSessionReconnecting));
        subscriptions.add(reactiveEventBus.on(CaptchaChallengeEvent.class).subscribe(this::onCaptchaChallenge));
        subscriptions.add(reactiveEventBus.on(CaptchaResolvedEvent.class).subscribe(this::onCaptchaResolved));
        subscriptions.add(reactiveEventBus.on(LoginPhaseEvent.class).subscribe(this::onLoginPhaseEvent));
        log.debug("ISessionModel projection active");
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        for (MachineSessionState st : stateByMachine.values()) {
            st.snapshotSink.tryEmitComplete();
        }
        stateByMachine.clear();
        globalSink.tryEmitComplete();
    }

    // ─────────────────── ISessionModel contract ───────────────────

    @Override
    public Optional<ISessionSnapshot> snapshot(String machineId) {
        MachineSessionState state = stateByMachine.get(machineId);
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(new SessionSnapshotImpl(machineId, state));
    }

    @Override
    public Flux<ISessionSnapshot> observe(String machineId) {
        MachineSessionState state = getOrCreate(machineId);
        return state.snapshotSink.asFlux();
    }

    @Override
    public Flux<ISessionSnapshot> observeAll() {
        return globalSink.asFlux();
    }

    // ─────────────────── Event handlers ───────────────────

    private void onSessionConnected(SessionConnectedEvent event) {
        String machineId = event.getFullName();
        MachineSessionState state = getOrCreate(machineId);
        state.transitionTo(SessionPhase.CONNECTED);
        state.resetOnConnect();
        emitSnapshot(machineId, state);
        log.debug("Session connected: {}", machineId);
    }

    private void onSessionDisconnected(SessionDisconnectedEvent event) {
        String machineId = event.getFullName();
        MachineSessionState state = getOrCreate(machineId);
        state.transitionTo(SessionPhase.DISCONNECTED);
        state.lastDisconnectReason = event.getReason();
        emitSnapshot(machineId, state);
        log.debug("Session disconnected: {} (reason={})", machineId, event.getReason());
    }

    private void onSessionReconnecting(SessionReconnectingEvent event) {
        String machineId = event.getFullName();
        MachineSessionState state = getOrCreate(machineId);
        state.transitionTo(SessionPhase.RECONNECTING);
        state.reconnectAttempt = event.getAttempt();
        state.nextAttemptAtEpochMs = event.getNextAttemptAtMs();
        emitSnapshot(machineId, state);
        log.debug("Session reconnecting: {} (attempt={})", machineId, event.getAttempt());
    }

    private void onCaptchaChallenge(CaptchaChallengeEvent event) {
        String machineId = event.getFullName();
        MachineSessionState state = getOrCreate(machineId);
        state.transitionTo(SessionPhase.CAPTCHA_PENDING);
        byte[] imageData = event.getImageData();
        String hint = (imageData != null && imageData.length > 0) ? "IMAGE" : "PASSCODE";
        state.pendingCaptcha = new CaptchaPrompt(
                event.getCaptchaId(), imageData, event.getTimestamp(), hint);
        emitSnapshot(machineId, state);
        log.debug("Captcha challenge received: {} (id={})", machineId, event.getCaptchaId());
    }

    private void onCaptchaResolved(CaptchaResolvedEvent event) {
        String machineId = event.getFullName();
        MachineSessionState state = getOrCreate(machineId);
        state.pendingCaptcha = null;
        state.transitionTo(SessionPhase.AUTHENTICATING);
        emitSnapshot(machineId, state);
        log.debug("Captcha resolved: {}", machineId);
    }

    private void onLoginPhaseEvent(LoginPhaseEvent event) {
        if ("AUTHENTICATING".equals(event.getPhase())) {
            String machineId = event.getFullName();
            MachineSessionState state = getOrCreate(machineId);
            state.transitionTo(SessionPhase.AUTHENTICATING);
            emitSnapshot(machineId, state);
            log.debug("Login phase changed to AUTHENTICATING: {}", machineId);
        }
    }

    // ─────────────────── Helpers ───────────────────

    private MachineSessionState getOrCreate(String machineId) {
        return stateByMachine.computeIfAbsent(machineId, k -> new MachineSessionState());
    }

    private void emitSnapshot(String machineId, MachineSessionState state) {
        SessionSnapshotImpl snap = new SessionSnapshotImpl(machineId, state);
        state.snapshotSink.tryEmitNext(snap);
        globalSink.tryEmitNext(snap);
    }
}
