package org.sokybot.gamemodel.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.commons.SilkroadUtils;
import org.sokybot.commons.event.IReactiveEventBus;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;
import org.sokybot.gamemodel.ModelUpdate;
import org.sokybot.gamemodel.ModelUpdateType;
import org.sokybot.gamemodel.LoginState;
import org.sokybot.gameevents.dto.MonsterData;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.character.CharacterSelectionActionEvent;
import org.sokybot.gameevents.events.combat.AgentListEvent;
import org.sokybot.gameevents.events.entity.EntityAngleUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.gameevents.events.entity.EntitySpeedUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityStoppedEvent;
import org.sokybot.gameevents.events.entity.GroupSpawnBeginEvent;
import org.sokybot.gameevents.events.entity.GroupSpawnEndEvent;
import org.sokybot.gameevents.events.spawn.MonsterSpawnEvent;
import org.sokybot.gameevents.events.session.AuthResponseEvent;
import org.sokybot.gameevents.events.session.CaptchaChallengeEvent;
import org.sokybot.gameevents.events.session.LoginResponseEvent;
import org.sokybot.gameevents.events.session.PasscodeRequiredEvent;
import org.sokybot.gameevents.events.skill.SkillCastEvent;
import org.sokybot.gameevents.events.skill.SkillCastErrorEvent;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;

import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.events.core.IGameEvent;

public class GameModelImpl implements IGameModel {

    private static final Logger log = LoggerFactory.getLogger(GameModelImpl.class);

    /**
     * Full machine id ({@code group.machineName}); must match
     * {@link IGameEvent#getFullName()}.
     */
    private final String machineName;
    private final IReactiveEventBus eventBus;
    private final java.util.List<Disposable> subscriptions = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Internal mutable map
    private final Map<Integer, Spawn> spawns = new ConcurrentHashMap<>();
    private final Trainer trainer = new Trainer();
    private final LoginState loginState = new LoginState();

    // Movement handling
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2); // 2 threads enough?
    private final Map<Integer, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    private final Sinks.Many<ModelUpdate<ISpawn>> modelSink = Sinks.many().multicast().onBackpressureBuffer();
    private volatile ScheduledFuture<?> spawnQuietWindowTask;
    private final AtomicLong lastSpawnSignalAt = new AtomicLong(0L);
    private static final long SPAWN_SYNC_QUIET_WINDOW_MS = 700L;

    private int selectedId = -1;

    public GameModelImpl(String machineName, IReactiveEventBus eventBus) {
        this.machineName = machineName;
        this.eventBus = eventBus;
    }

    private boolean isForThisMachine(IGameEvent event) {
        String fn = event.getFullName();
        return fn != null && fn.equals(machineName);
    }

    public void start() {
        // World/combat events via reactive bus (may originate from other publishers).
        // Login/session events
        // are applied via {@link #dispatchGameEvent(IGameEvent)} from the packet
        // translator bridge only.
        subscriptions.add(eventBus.on(MonsterSpawnEvent.class).subscribe(this::handleMonsterSpawn));
        subscriptions.add(eventBus.on(EntityDespawnEvent.class).subscribe(this::handleDespawn));
        subscriptions.add(eventBus.on(EntityMovementEvent.class).subscribe(this::handleMovement));
        subscriptions.add(eventBus.on(EntityStoppedEvent.class).subscribe(this::handleStopped));
        subscriptions.add(eventBus.on(EntityHPMPUpdateEvent.class).subscribe(this::handleHPMP));
        subscriptions.add(eventBus.on(EntitySpeedUpdateEvent.class).subscribe(this::handleSpeed));
        subscriptions.add(eventBus.on(EntityAngleUpdateEvent.class).subscribe(this::handleAngle));
        subscriptions.add(eventBus.on(SkillCastEvent.class).subscribe(this::handleSkillCast));
        subscriptions.add(eventBus.on(SkillCastErrorEvent.class).subscribe(this::handleSkillCastError));
        subscriptions.add(eventBus.on(GroupSpawnBeginEvent.class).subscribe(this::handleGroupSpawnBegin));
        subscriptions.add(eventBus.on(GroupSpawnEndEvent.class).subscribe(this::handleGroupSpawnEnd));
    }

    @Override
    public void dispatchGameEvent(IGameEvent event) {
        if (event == null || !isForThisMachine(event)) {
            return;
        }
        if (event instanceof AgentListEvent) {
            handleAgentList((AgentListEvent) event);
        } else if (event instanceof LoginResponseEvent) {
            handleLoginResponse((LoginResponseEvent) event);
        } else if (event instanceof AuthResponseEvent) {
            handleAuthResponse((AuthResponseEvent) event);
        } else if (event instanceof PasscodeRequiredEvent) {
            handlePasscodeRequired((PasscodeRequiredEvent) event);
        } else if (event instanceof CaptchaChallengeEvent) {
            handleCaptchaChallenge((CaptchaChallengeEvent) event);
        } else if (event instanceof CharacterSelectionActionEvent) {
            handleCharacterSelection((CharacterSelectionActionEvent) event);
        } else if (event instanceof CharacterLoadedEvent) {
            handleCharacterLoaded((CharacterLoadedEvent) event);
        } else if (event instanceof GroupSpawnBeginEvent) {
            handleGroupSpawnBegin((GroupSpawnBeginEvent) event);
        } else if (event instanceof GroupSpawnEndEvent) {
            handleGroupSpawnEnd((GroupSpawnEndEvent) event);
        }
    }

    public void stop() {
        subscriptions.forEach(Disposable::dispose);
        subscriptions.clear();
        ScheduledFuture<?> task = spawnQuietWindowTask;
        if (task != null) {
            task.cancel(true);
            spawnQuietWindowTask = null;
        }
        scheduler.shutdownNow();
    }

    @Override
    public Optional<ISpawn> find(int id) {
        if (id == trainer.getUniqueId())
            return Optional.of(trainer);
        return Optional.ofNullable(spawns.get(id));
    }

    @Override
    public <T extends ISpawn> Map<Integer, T> findAll(Class<T> type) {
        Map<Integer, T> result = new HashMap<>();
        if (type.isInstance(trainer)) {
            result.put(trainer.getUniqueId(), type.cast(trainer));
        }
        spawns.values().stream()
                .filter(type::isInstance)
                .forEach(s -> result.put(s.getUniqueId(), type.cast(s)));
        return result;
    }

    @Override
    public <T extends ISpawn> Optional<T> find(int id, Class<T> type) {
        ISpawn s = null;
        if (id == trainer.getUniqueId())
            s = trainer;
        else
            s = spawns.get(id);

        if (s != null && type.isInstance(s)) {
            return Optional.of(type.cast(s));
        }
        return Optional.empty();
    }

    @Override
    public Optional<ISpawn> getSelected() {
        return find(selectedId);
    }

    @Override
    public ITrainer getTrainer() {
        return trainer;
    }

    @Override
    public LoginState getLoginState() {
        return loginState;
    }

    @Override
    public <T extends ISpawn> Flux<T> observe(int id, Class<T> type) {
        Optional<T> initial = find(id, type);
        Flux<T> updates = modelSink.asFlux()
                .filter(update -> update.getEntity().getUniqueId() == id && type.isInstance(update.getEntity()))
                .filter(update -> update.getType() != ModelUpdateType.REMOVED)
                .map(update -> type.cast(update.getEntity()));

        return initial.map(Flux::just).orElse(Flux.empty()).concatWith(updates);
    }

    @Override
    public <T extends ISpawn> Flux<ModelUpdate<T>> observeAll(Class<T> type) {
        return Flux.defer(() -> {
            Flux<ModelUpdate<T>> initial = Flux.fromIterable(findAll(type).values())
                    .map(e -> new ModelUpdate<>(e, ModelUpdateType.ADDED));

            Flux<ModelUpdate<T>> updates = modelSink.asFlux()
                    .filter(update -> type.isInstance(update.getEntity()))
                    .map(update -> new ModelUpdate<>(type.cast(update.getEntity()), update.getType()));

            return initial.concatWith(updates);
        });
    }

    private void emitUpdate(ISpawn entity, ModelUpdateType type) {
        modelSink.tryEmitNext(new ModelUpdate<>(entity, type));
    }

    // ================= Event Handling =================
    // Note: handleEvent(Event) removed in favor of typed subscriptions

    private void handleMonsterSpawn(MonsterSpawnEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        MonsterData md = event.getMonster();
        Monster m = new Monster(md);
        // Initial pos calculation?
        // md likely has sectors/offsets.
        int x = SilkroadUtils.getXCoord(md.getXOffset(), (short) md.getXSector());
        int y = SilkroadUtils.getYCoord(md.getYOffset(), (short) md.getYSector());
        // m.setDescription(md.getRefId() + ""); // hack?
        m.setLocation(x, y);
        m.setX(x); // Explicit internal setter
        m.setY(y);
        spawns.put(m.getUniqueId(), m);
        emitUpdate(m, ModelUpdateType.ADDED);
        onSpawnSignal();
    }

    private void handleDespawn(EntityDespawnEvent event) {
        int id = event.getEntityId();
        Spawn s = spawns.remove(id);
        if (s != null) {
            emitUpdate(s, ModelUpdateType.REMOVED);
        }
        stopMovement(id);
        onSpawnSignal();
    }

    private void handleHPMP(EntityHPMPUpdateEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        int id = event.getEntityId();
        if (id == trainer.getUniqueId()) {
            if (event.getNewHP() != null)
                trainer.setCharHP(event.getNewHP());
            if (event.getNewMP() != null)
                trainer.setCharMP(event.getNewMP());
        } else {
            Spawn s = spawns.get(id);
            if (s instanceof Fighter) {
                if (event.getNewHP() != null)
                    ((Fighter) s).setCurrentHP(event.getNewHP());
                emitUpdate(s, ModelUpdateType.UPDATED);
                // MP?
            }
        }
    }

    private void handleSpeed(EntitySpeedUpdateEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        int id = event.getEntityId();
        Fighter f = resolveFighter(id);
        if (f != null) {
            f.setWalkSpeed(event.getWalkSpeed());
            f.setRunSpeed(event.getRunSpeed());
            emitUpdate(f, ModelUpdateType.UPDATED);
            // Restart movement if moving?
        }
    }

    private void handleAngle(EntityAngleUpdateEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        int id = event.getEntityId();
        Spawn s = resolveSpawn(id);
        if (s != null) {
            s.setAngle((short) event.getNewAngle());
            emitUpdate(s, ModelUpdateType.UPDATED);
        }
    }

    private void handleSkillCast(SkillCastEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        if (event.isSuccess()) {
            Integer casterId = event.getCasterId();
            Integer targetId = event.getTargetId();
            if (casterId != null) {
                Fighter f = resolveFighter(casterId);
                if (f != null && targetId != null) {
                    f.setTargetId(targetId);
                    emitUpdate(f, ModelUpdateType.UPDATED);
                }
            }
        }
    }

    private void handleSkillCastError(SkillCastErrorEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        log.warn("Skill cast error detected: {}", event.getErrorType());
        trainer.setLastError(event.getErrorType());
        emitUpdate(trainer, ModelUpdateType.UPDATED);
    }

    private void handleAgentList(AgentListEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        loginState.setAgentList(event.getAgents());
        loginState.setPhase(LoginState.Phase.AGENTS_RECEIVED);
        int receivedCount = event.getAgents() != null ? event.getAgents().size() : 0;
        log.debug("Machine {} handleAgentList received {} agents; phase -> {}", machineName, receivedCount,
                LoginState.Phase.AGENTS_RECEIVED);
    }

    private void handleLoginResponse(LoginResponseEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        LoginState.Phase phase = loginState.getPhase();
        if (event.isSuccess()) {
            // Ignore late success only while not past gateway handshake (e.g. reconnect
            // race).
            if (phase == LoginState.Phase.CONNECTING_GATEWAY || phase == LoginState.Phase.DISCONNECTED) {
                return;
            }
            loginState.setLoginId(event.getLoginId());
            loginState.setAgentHost(event.getAgentHost());
            loginState.setAgentPort(event.getAgentPort());
            loginState.setFailureReason(null);
            loginState.setGatewayResultCode(null);
            loginState.setPhase(LoginState.Phase.LOGIN_SUCCESS);
        } else {
            // Always apply gateway failure so login cycle does not sit in LOGIN_SENT until
            // timeout.
            // (Removed check for DISCONNECTED phase here to prevent hiding login failures
            // during rapid disconnects)
            loginState.setGatewayResultCode((int) event.getResultCode());
            loginState.setFailureReason("Gateway login failed: code " + (event.getResultCode() & 0xFF));
            loginState.setPhase(LoginState.Phase.FAILED);
        }
    }

    private void handleAuthResponse(AuthResponseEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        LoginState.Phase phase = loginState.getPhase();
        if (phase == LoginState.Phase.CONNECTING_GATEWAY || phase == LoginState.Phase.DISCONNECTED) {
            return;
        }
        loginState.setAuthSuccess(event.isSuccess());
        if (event.isSuccess()) {
            loginState.setFailureReason(null);
            loginState.setAgentAuthResultCode(null);
            loginState.setPhase(LoginState.Phase.AUTHENTICATED);
        } else {
            loginState.setAgentAuthResultCode((int) event.getResultCode());
            loginState.setFailureReason("Agent auth failed: code " + event.getResultCode());
            loginState.setPhase(LoginState.Phase.FAILED);
        }
    }

    private void handlePasscodeRequired(PasscodeRequiredEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        loginState.setFailureReason("Passcode required");
        loginState.setPhase(LoginState.Phase.WAITING_FOR_PASSCODE);
    }

    private void handleCaptchaChallenge(CaptchaChallengeEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        loginState.setFailureReason("Captcha challenge required");
        loginState.setPhase(LoginState.Phase.WAIT_FOR_CAPTCHA);
    }

    private void handleCharacterSelection(CharacterSelectionActionEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        if (event.getResult() == 1 && event.getCharacters() != null) {
            loginState.setAvailableCharacters(event.getCharacters());
        }
    }

    private void handleCharacterLoaded(CharacterLoadedEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        if (event.getCharacterName() != null && !event.getCharacterName().isBlank()) {
            loginState.setSelectedCharacterName(event.getCharacterName());
        }
        if (loginState.getPhase() == LoginState.Phase.AUTHENTICATED
                || loginState.getPhase() == LoginState.Phase.AGENT_CONNECTED) {
            loginState.setPhase(LoginState.Phase.LOADING_ENVIRONMENT);
        }
        tryCompleteWorldReady();
    }

    private void handleGroupSpawnBegin(GroupSpawnBeginEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        loginState.setSpawnSyncActive(true);
        loginState.setWorldReady(false);
        if (loginState.getPhase() == LoginState.Phase.AUTHENTICATED
                || loginState.getPhase() == LoginState.Phase.AGENT_CONNECTED) {
            loginState.setPhase(LoginState.Phase.LOADING_ENVIRONMENT);
        }
        onSpawnSignal();
    }

    private void handleGroupSpawnEnd(GroupSpawnEndEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        loginState.setSpawnSyncActive(false);
        tryCompleteWorldReady();
    }

    private void onSpawnSignal() {
        if (!loginState.isSpawnSyncActive()) {
            return;
        }
        lastSpawnSignalAt.set(System.currentTimeMillis());
        ScheduledFuture<?> existing = spawnQuietWindowTask;
        if (existing != null) {
            existing.cancel(false);
        }
        spawnQuietWindowTask = scheduler.schedule(() -> {
            if (!loginState.isSpawnSyncActive()) {
                return;
            }
            long elapsed = System.currentTimeMillis() - lastSpawnSignalAt.get();
            if (elapsed >= SPAWN_SYNC_QUIET_WINDOW_MS) {
                loginState.setSpawnSyncActive(false);
                tryCompleteWorldReady();
            }
        }, SPAWN_SYNC_QUIET_WINDOW_MS, TimeUnit.MILLISECONDS);
    }

    private void tryCompleteWorldReady() {
        if (loginState.isSpawnSyncActive()) {
            return;
        }
        if (trainer == null || trainer.getUniqueId() <= 0) {
            return;
        }
        loginState.setWorldReady(true);
        loginState.setFailureReason(null);
        loginState.setPhase(LoginState.Phase.IN_GAME);
    }

    private Fighter resolveFighter(int id) {
        if (id == trainer.getUniqueId())
            return trainer;
        Spawn s = spawns.get(id);
        if (s instanceof Fighter)
            return (Fighter) s;
        return null;
    }

    private Spawn resolveSpawn(int id) {
        if (id == trainer.getUniqueId())
            return trainer;
        return spawns.get(id);
    }

    private void handleMovement(EntityMovementEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        int id = event.getEntityId();
        Fighter fighter = resolveFighter(id);

        if (fighter != null) {
            boolean hasDestination = event.hasDestination();
            fighter.setHasDestination(hasDestination);

            if (event.getMovementType() != null) {
                fighter.setMovementType(org.sokybot.gameevents.enums.MovementType.of(event.getMovementType()));
                // MovementType is in game-enums, so it matches.
            }

            GamePosition dest = event.getDestination();
            if (hasDestination && dest != null) {
                byte destXSector = event.getDestXSector() != null ? event.getDestXSector().byteValue() : (byte) 0;
                byte destYSector = event.getDestYSector() != null ? event.getDestYSector().byteValue() : (byte) 0;
                float destXOffset = dest.getX();
                float destYOffset = dest.getY();
                float destZOffset = dest.getZ();

                fighter.setDestXSector(destXSector);
                fighter.setDestYSector(destYSector);
                fighter.setDestXOffset((short) destXOffset);
                fighter.setDestYOffset((short) destYOffset);
                fighter.setDestZOffset((short) destZOffset);

                int destX = SilkroadUtils.getXCoord(destXOffset, destXSector);
                int destY = SilkroadUtils.getYCoord(destYOffset, destYSector);
                fighter.setDestX(destX);
                fighter.setDestY(destY);
            } else {
                if (event.getSkyClickFlag() != null) {
                    fighter.setSkyClickFlag(event.getSkyClickFlag());
                }
                // Angle logic
                if (event.getAngleAction() != null && event.getSkyClickFlag() != null && event.getSkyClickFlag() == 1) {
                    fighter.setAngle(event.getAngleAction());
                }
            }

            GamePosition currentPos = event.getCurrentPosition();
            if (currentPos != null && event.getCurrentXSector() != null && event.getCurrentYSector() != null) {
                byte xSector = event.getCurrentXSector().byteValue();
                byte ySector = event.getCurrentYSector().byteValue();
                float xOffset = currentPos.getX();
                float yOffset = currentPos.getY();
                float zOffset = currentPos.getZ();

                fighter.setXSector(xSector);
                fighter.setYSector(ySector);
                fighter.setXOffset((short) xOffset);
                fighter.setYOffset((short) yOffset);
                fighter.setZOffset((short) zOffset);

                if (event.getCurrentAngle() != null) {
                    // Need Util match
                    fighter.setAngle(org.sokybot.commons.SilkroadUtils.getAngle(event.getCurrentAngle()));
                }

                int x = SilkroadUtils.getXCoord(xOffset, xSector); // overload with 100?
                int y = SilkroadUtils.getYCoord(yOffset, ySector);
                fighter.setLocation(x, y);
            }

            startMovement(fighter);
            emitUpdate(fighter, ModelUpdateType.UPDATED);
        }
    }

    private void handleStopped(EntityStoppedEvent event) {
        if (!isForThisMachine(event)) {
            return;
        }
        int id = event.getEntityId();
        stopMovement(id);
        // Optional: snap to final pos?
    }

    private void stopMovement(int id) {
        ScheduledFuture<?> task = tasks.remove(id);
        if (task != null) {
            task.cancel(true);
        }
    }

    private void startMovement(Fighter fighter) {
        stopMovement(fighter.getUniqueId());

        // Only animate if destination exists or forced
        // Logic from EnvironmentHandler

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            double angle = 0;
            if (fighter.isHasDestination()) {
                angle = Math.atan2(fighter.getDestY() - fighter.getY(), fighter.getDestX() - fighter.getX());
            } else {
                angle = Math.toRadians(fighter.getAngle());
            }

            if (angle != 0) {
                // Speed factor? 0.1?
                // Logic: 1000 / (runSpeed * 0.1) ms delay?
                // Wait, scheduleAtFixedRate is (runnable, init, period).
                // If period is calculated inside? No, period is fixed.
                // EnvironmentHandler calculated period: (long) (1000 / (fighter.getRunSpeed() *
                // 0.1))
                // This assumes constant speed.

                // We perform ONE step here.
                fighter.translate((int) Math.round(Math.cos(angle)), (int) Math.round(Math.sin(angle)));
                // Update specific internal logic if needed
                int newX = fighter.getX() + (int) Math.round(Math.cos(angle));
                int newY = fighter.getY() + (int) Math.round(Math.sin(angle));
                fighter.setLocation(newX, newY);
                emitUpdate(fighter, ModelUpdateType.UPDATED);

            } else {
                stopMovement(fighter.getUniqueId());
            }
        }, 0, calculatePeriod(fighter), TimeUnit.MILLISECONDS);

        tasks.put(fighter.getUniqueId(), task);
    }

    private long calculatePeriod(Fighter f) {
        float speed = f.getRunSpeed();
        // fallback
        if (speed <= 0)
            speed = 50f;
        return (long) (1000 / (speed * 0.1));
    }

}
