package org.sokybot.behaviors.logistics.quartermaster;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.logistics.IQuartermasterCoordinator;
import org.sokybot.behaviors.logistics.internal.settings.quartermaster.QuartermasterSettings;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.quartermaster.QuartermasterBlackboardKeys;
import org.sokybot.swarm.api.quartermaster.SwarmStorageLockGrantedEvent;
import org.sokybot.swarm.api.quartermaster.SwarmStorageLockRequestEvent;
import org.sokybot.swarm.api.quartermaster.SwarmStorageReleaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;

/**
 * Epic #15: JVM-wide storage mutex and handoff to the local quartermaster for post-dump sort.
 */
@Component(immediate = true, service = IQuartermasterCoordinator.class)
public final class QuartermasterCoordinator implements IQuartermasterCoordinator {

    private static final Logger log = LoggerFactory.getLogger(QuartermasterCoordinator.class);

    private static final String QM_SCOPE = "quartermaster";

    /** Sort phase safety TTL multiplier when extending expiry in {@link LockStatus#SORTING}. */
    private static final long SORT_PHASE_TTL_MULT = 10L;

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference
    private ISokybotContext sokybotContext;

    private final Map<String, LockState> lockMap = new ConcurrentHashMap<>();
    /** Remembers which swarm group issued a grant so release can target the right QM. */
    private final Map<String, String> sessionToSwarmGroup = new ConcurrentHashMap<>();

    private volatile Disposable.Composite disposables;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmEventBus;
        if (bus == null) {
            log.warn("QuartermasterCoordinator: ISwarmEventBus unavailable");
            return;
        }
        Disposable.Composite c = Disposables.composite();
        this.disposables = c;

        c.add(bus.observe(SwarmStorageLockRequestEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "QuartermasterCoordinator lock-request stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onLockRequest));

        c.add(bus.observe(SwarmStorageReleaseEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "QuartermasterCoordinator release stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onRelease));

        c.add(Flux.interval(Duration.ofSeconds(5))
                .onErrorContinue((err, trigger) -> log.warn(
                        "QuartermasterCoordinator janitor stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(tick -> janitorSweep()));

        log.info("QuartermasterCoordinator activated");
    }

    @Deactivate
    void deactivate() {
        Disposable.Composite c = this.disposables;
        this.disposables = null;
        if (c != null && !c.isDisposed()) {
            c.dispose();
        }
        lockMap.clear();
        sessionToSwarmGroup.clear();
        log.info("QuartermasterCoordinator deactivated");
    }

    private void janitorSweep() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, LockState> e : lockMap.entrySet()) {
                LockState state = e.getValue();
                if (state == null) {
                    continue;
                }
                long exp = state.getExpiresAtMs();
                if (exp <= 0L || now < exp) {
                    continue;
                }
                synchronized (state) {
                    if (state.getExpiresAtMs() <= 0L || now < state.getExpiresAtMs()) {
                        continue;
                    }
                    String sid = e.getKey();
                    resetSession(sid, state);
                    lockMap.remove(sid);
                }
            }
        } catch (RuntimeException ex) {
            log.debug("QuartermasterCoordinator janitor: {}", ex.getMessage());
        }
    }

    private void resetSession(String storageSessionId, LockState state) {
        state.setStatus(LockStatus.IDLE);
        state.setGrantedToken("");
        state.setExpiresAtMs(0L);
        sessionToSwarmGroup.remove(storageSessionId);
        StorageGrantLedger.clearSession(storageSessionId);
    }

    @Override
    public void resetVault(String swarmGroupId) {
        try {
            if (swarmGroupId == null || swarmGroupId.trim().isEmpty()) {
                return;
            }
            String g = swarmGroupId.trim();
            List<String> sessions = new ArrayList<>();
            for (Map.Entry<String, String> e : sessionToSwarmGroup.entrySet()) {
                if (e != null && g.equals(e.getValue())) {
                    sessions.add(e.getKey());
                }
            }
            for (String sid : sessions) {
                LockState ls = lockMap.remove(sid);
                if (ls != null) {
                    synchronized (ls) {
                        ls.setStatus(LockStatus.IDLE);
                        ls.setGrantedToken("");
                        ls.setExpiresAtMs(0L);
                    }
                }
                sessionToSwarmGroup.remove(sid);
                StorageGrantLedger.clearSession(sid);
            }
        } catch (RuntimeException ex) {
            log.warn("QuartermasterCoordinator resetVault: {}", ex.getMessage());
        }
    }

    private void onLockRequest(SwarmStorageLockRequestEvent event) {
        try {
            if (event == null) {
                return;
            }
            ISokybotContext ctx = sokybotContext;
            ISettingsRegistry registry = settingsRegistry;
            ISwarmEventBus bus = swarmEventBus;
            if (ctx == null || registry == null || bus == null) {
                return;
            }

            QuartermasterBinding qm = findQuartermaster(ctx, registry, event.getSwarmGroupId());
            if (qm == null) {
                return;
            }

            QuartermasterSettings settings = qm.settings;
            long lockTtlMs = settings.getLockTtlMs();
            if (lockTtlMs <= 0L) {
                lockTtlMs = 30000L;
            }

            String sessionId = event.getStorageSessionId();
            LockState state = lockMap.computeIfAbsent(sessionId, k -> new LockState());

            synchronized (state) {
                long now = System.currentTimeMillis();
                boolean expired = state.getExpiresAtMs() > 0L && now >= state.getExpiresAtMs();
                if (!expired) {
                    if (state.getStatus() == LockStatus.GRANTED) {
                        return;
                    }
                    if (state.getStatus() == LockStatus.SORTING) {
                        return;
                    }
                }

                String newToken = UUID.randomUUID().toString();
                state.setStatus(LockStatus.GRANTED);
                state.setGrantedToken(newToken);
                state.setExpiresAtMs(now + lockTtlMs);
                sessionToSwarmGroup.put(sessionId, event.getSwarmGroupId());

                StorageGrantLedger.offerGrant(sessionId, newToken);
                bus.publish(new SwarmStorageLockGrantedEvent(
                        event.getRequesterMachineId(),
                        now,
                        event.getRequestId(),
                        sessionId,
                        newToken,
                        now,
                        lockTtlMs));
            }
        } catch (RuntimeException ex) {
            log.warn("QuartermasterCoordinator onLockRequest: {}", ex.getMessage());
        }
    }

    private void onRelease(SwarmStorageReleaseEvent event) {
        try {
            if (event == null) {
                return;
            }
            ISokybotContext ctx = sokybotContext;
            ISettingsRegistry registry = settingsRegistry;
            if (ctx == null || registry == null) {
                return;
            }

            String sessionId = event.getStorageSessionId();
            LockState state = lockMap.get(sessionId);
            if (state == null) {
                return;
            }

            synchronized (state) {
                if (state.getStatus() != LockStatus.GRANTED) {
                    return;
                }
                String current = state.getGrantedToken();
                if (current == null || current.isEmpty() || !current.equals(event.getGrantedToken())) {
                    return;
                }

                QuartermasterSettings settings = resolveSettingsForSession(ctx, registry, sessionId);
                long baseTtl = settings != null && settings.getLockTtlMs() > 0L
                        ? settings.getLockTtlMs()
                        : 30000L;
                long sortSafetyMs = Math.max(baseTtl * SORT_PHASE_TTL_MULT, 120_000L);
                long now = System.currentTimeMillis();

                state.setStatus(LockStatus.SORTING);
                state.setExpiresAtMs(now + sortSafetyMs);

                IMachineContext qmMachine = findQuartermasterMachine(ctx, registry, sessionToSwarmGroup.get(sessionId));
                if (qmMachine == null) {
                    qmMachine = findQuartermasterMachineAnyGroup(ctx, registry);
                }
                if (qmMachine == null) {
                    log.warn("QuartermasterCoordinator onRelease: no QM machine for session {}", sessionId);
                    return;
                }

                IEngine engine = qmMachine.getEngine();
                java.util.Optional<IWorkflowContext> wfOpt = safeWorkflow(engine);
                if (!wfOpt.isPresent()) {
                    return;
                }
                Map<String, Object> pd = wfOpt.get().getPersistentData();
                if (pd == null) {
                    return;
                }
                pd.put(QuartermasterBlackboardKeys.KEY_QUARTERMASTER_SORT_NEEDED, Boolean.TRUE);
            }
        } catch (RuntimeException ex) {
            log.warn("QuartermasterCoordinator onRelease: {}", ex.getMessage());
        }
    }

    private QuartermasterSettings resolveSettingsForSession(
            ISokybotContext ctx, ISettingsRegistry registry, String sessionId) {
        String groupId = sessionToSwarmGroup.get(sessionId);
        IMachineContext m = findQuartermasterMachine(ctx, registry, groupId);
        if (m == null) {
            return null;
        }
        return readQuartermaster(registry, m);
    }

    private QuartermasterBinding findQuartermaster(
            ISokybotContext ctx, ISettingsRegistry registry, String swarmGroupId) {
        if (swarmGroupId == null || swarmGroupId.trim().isEmpty()) {
            return null;
        }
        IMachineContext m = findQuartermasterMachine(ctx, registry, swarmGroupId.trim());
        if (m == null) {
            return null;
        }
        QuartermasterSettings s = readQuartermaster(registry, m);
        if (s == null || !s.isQuartermasterEnabled()) {
            return null;
        }
        return new QuartermasterBinding(s);
    }

    private IMachineContext findQuartermasterMachine(
            ISokybotContext ctx, ISettingsRegistry registry, String swarmGroupId) {
        if (ctx == null || registry == null || swarmGroupId == null || swarmGroupId.trim().isEmpty()) {
            return null;
        }
        String g = swarmGroupId.trim();
        for (IGroupContext group : ctx.getGroups()) {
            if (group == null || !Objects.equals(group.name().trim(), g)) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null || !machine.isRunning()) {
                    continue;
                }
                try {
                    QuartermasterSettings s = readQuartermaster(registry, machine);
                    if (s != null && s.isQuartermasterEnabled()) {
                        return machine;
                    }
                } catch (RuntimeException ex) {
                    log.trace("QuartermasterCoordinator probe {}: {}", machine.fullName(), ex.getMessage());
                }
            }
        }
        return null;
    }

    private IMachineContext findQuartermasterMachineAnyGroup(ISokybotContext ctx, ISettingsRegistry registry) {
        if (ctx == null || registry == null) {
            return null;
        }
        for (IGroupContext group : ctx.getGroups()) {
            if (group == null) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null || !machine.isRunning()) {
                    continue;
                }
                try {
                    QuartermasterSettings s = readQuartermaster(registry, machine);
                    if (s != null && s.isQuartermasterEnabled()) {
                        return machine;
                    }
                } catch (RuntimeException ex) {
                    log.trace("QuartermasterCoordinator probe {}: {}", machine.fullName(), ex.getMessage());
                }
            }
        }
        return null;
    }

    private static QuartermasterSettings readQuartermaster(ISettingsRegistry registry, IMachineContext machine) {
        try {
            ISettingsProvider<QuartermasterSettings> p = registry.getProvider(
                    machine.getGroupName(),
                    machine.getMachineName(),
                    QM_SCOPE,
                    QuartermasterSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Optional<IWorkflowContext> safeWorkflow(IEngine engine) {
        if (engine == null) {
            return Optional.empty();
        }
        try {
            return engine.optionalWorkflowContext();
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static final class QuartermasterBinding {
        final QuartermasterSettings settings;

        QuartermasterBinding(QuartermasterSettings settings) {
            this.settings = settings;
        }
    }

}
