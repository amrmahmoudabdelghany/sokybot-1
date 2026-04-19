package org.sokybot.combat.projections.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.sokybot.combat.api.ActiveBuff;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.combat.api.CombatSnapshot;
import org.sokybot.combat.api.ILeashAnchorStore;
import org.sokybot.combat.api.IMobOwnershipTracker;
import org.sokybot.combat.api.DroppedItemRef;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.MonsterRef;
import org.sokybot.combat.api.MonsterTier;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.dto.MonsterData;
import org.sokybot.gameevents.dto.PlayerData;
import org.sokybot.gameevents.dto.ItemData;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.events.buff.BuffAppliedEvent;
import org.sokybot.gameevents.events.buff.BuffRemovedEvent;
import org.sokybot.gameevents.events.character.CharacterBuffLoadedEvent;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.combat.BerserkConfirmEvent;
import org.sokybot.gameevents.events.combat.PickupAnimationEvent;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.events.entity.EntityDeselectedEvent;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.gameevents.events.entity.EntitySelectedEvent;
import org.sokybot.gameevents.events.entity.EntityStateUpdateEvent;
import org.sokybot.gameevents.events.skill.SkillCastEndEvent;
import org.sokybot.gameevents.events.skill.SkillCastErrorEvent;
import org.sokybot.gameevents.events.skill.SkillCastEvent;
import org.sokybot.gameevents.events.spawn.ItemSpawnEvent;
import org.sokybot.gameevents.events.spawn.MonsterSpawnEvent;
import org.sokybot.gameevents.events.spawn.PlayerSpawnEvent;
import org.sokybot.gameevents.events.stat.LifeStateUpdateEvent;
import org.sokybot.gamemodel.IGameModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Tactical overlay sourced from {@link org.sokybot.commons.event.IReactiveEventBus}; keys state by machine full name.
 */
@Component(service = ICombatModel.class, immediate = true)
public final class CombatModelComponent implements ICombatModel {

    private static final Logger log = LoggerFactory.getLogger(CombatModelComponent.class);

    private static final long DEFAULT_SKILL_COOLDOWN_MS = 1200L;
    private static final long SKILL_CAST_WATCHDOG_MS = 4000L;

    private final Map<String, MachineCombatState> stateByMachine = new ConcurrentHashMap<>();

    private final List<Disposable> subscriptions = new ArrayList<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Reference
    private IMobOwnershipTracker mobOwnershipTracker;

    @Reference
    private ILeashAnchorStore leashAnchorStore;

    /**
     * Optional global bind (per-machine instances normally live inside engine/workflow context). Present only if a
     * deployment registers {@link IGameModel} as an OSGi service.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IGameModel optionalGameModelService;

    @Activate
    void activate() {
        if (optionalGameModelService != null) {
            log.trace("Optional IGameModel OSGi service present; tactical projection remains event-driven per machine.");
        }

        subscriptions.add(reactiveEventBus.on(CharacterLoadedEvent.class).subscribe(this::onCharacterLoaded));
        subscriptions.add(reactiveEventBus.on(PlayerSpawnEvent.class).subscribe(this::onPlayerSpawn));
        subscriptions.add(reactiveEventBus.on(MonsterSpawnEvent.class).subscribe(this::onMonsterSpawn));
        subscriptions.add(reactiveEventBus.on(ItemSpawnEvent.class).subscribe(this::onItemSpawn));
        subscriptions.add(reactiveEventBus.on(EntityDespawnEvent.class).subscribe(this::onEntityDespawn));
        subscriptions.add(reactiveEventBus.on(EntityMovementEvent.class).subscribe(this::onEntityMovement));
        subscriptions.add(reactiveEventBus.on(EntityHPMPUpdateEvent.class).subscribe(this::onHpMp));
        subscriptions.add(reactiveEventBus.on(EntityStateUpdateEvent.class).subscribe(this::onEntityState));
        subscriptions.add(reactiveEventBus.on(EntitySelectedEvent.class).subscribe(this::onEntitySelected));
        subscriptions.add(reactiveEventBus.on(EntityDeselectedEvent.class).subscribe(this::onEntityDeselected));
        subscriptions.add(reactiveEventBus.on(SkillCastEvent.class).subscribe(this::onSkillCast));
        subscriptions.add(reactiveEventBus.on(SkillCastEndEvent.class).subscribe(this::onSkillCastEnd));
        subscriptions.add(reactiveEventBus.on(SkillCastErrorEvent.class).subscribe(this::onSkillCastError));
        subscriptions.add(reactiveEventBus.on(PickupAnimationEvent.class).subscribe(this::onPickupAnimation));
        subscriptions.add(reactiveEventBus.on(BerserkConfirmEvent.class).subscribe(this::onBerserk));
        subscriptions.add(reactiveEventBus.on(LifeStateUpdateEvent.class).subscribe(this::onLifeState));
        subscriptions.add(reactiveEventBus.on(BuffAppliedEvent.class).subscribe(this::onBuffApplied));
        subscriptions.add(reactiveEventBus.on(BuffRemovedEvent.class).subscribe(this::onBuffRemoved));
        subscriptions.add(reactiveEventBus.on(CharacterBuffLoadedEvent.class).subscribe(this::onCharacterBuffLoaded));

        log.debug("ICombatModel projection active");
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        for (MachineCombatState st : stateByMachine.values()) {
            st.snapshotSink.tryEmitComplete();
        }
        stateByMachine.clear();
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private MachineCombatState stateFor(String fullNameKey) {
        return stateByMachine.computeIfAbsent(fullNameKey, fn -> new MachineCombatState());
    }

    private void publishSnapshot(String fullNameKey, MachineCombatState st) {
        if (fullNameKey == null || st == null) {
            return;
        }
        st.snapshotSink.emitNext(buildSnapshot(fullNameKey, st), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private void onCharacterLoaded(CharacterLoadedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        st.selfEntityId = e.getUniqueId();
        st.currentHp = e.getCurrentHP();
        st.currentMp = e.getCurrentMP();
        st.maxHp = Math.max(st.maxHp, Math.max(1, st.currentHp));
        st.maxMp = Math.max(st.maxMp, Math.max(1, st.currentMp));
        float[] pos = CombatPositions.xyzFromCharacterLoaded(e);
        if (pos != null) {
            st.selfX = pos[0];
            st.selfY = pos[1];
            st.selfZ = pos[2];
        }
        publishSnapshot(key, st);
    }

    private void onPlayerSpawn(PlayerSpawnEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        PlayerData p = e.getPlayer();
        if (p == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        Integer selfId = st.selfEntityId;
        if (selfId != null && selfId.intValue() != p.getUniqueId()) {
            return;
        }
        float[] pos = CombatPositions.xyzFromSpawn(p);
        applySelfPosition(st, pos);
        publishSnapshot(key, st);
    }

    private void onMonsterSpawn(MonsterSpawnEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MonsterData m = e.getMonster();
        if (m == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        TacticalMonster tm = new TacticalMonster(m.getUniqueId());
        tm.refObjId = m.getRefId();
        tm.levelOrZero = m.getLevel();
        tm.maxHp = Math.max(1, m.getMaxHp());
        tm.currentHp = m.getCurrentHp();
        tm.monsterType = m.getMonsterType();
        float[] pos = CombatPositions.xyzFromSpawn(m);
        if (pos != null) {
            tm.x = pos[0];
            tm.y = pos[1];
            tm.z = pos[2];
        }
        st.monsters.put(tm.entityId, tm);
        publishSnapshot(key, st);
    }

    private void onItemSpawn(ItemSpawnEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        ItemData item = e.getItem();
        if (item == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        TacticalLoot tl = new TacticalLoot(item.getUniqueId());
        tl.itemRefId = item.getRefId();
        if (item.isOwnerExist()) {
            tl.ownerEntityId = item.getOwnerJID();
            tl.ownerExpiresAtEpochMs = e.getTimestamp() + LootOwnerDurationTable.durationMsFor(item);
        } else {
            tl.ownerEntityId = null;
            tl.ownerExpiresAtEpochMs = Long.MAX_VALUE;
        }
        float[] pos = CombatPositions.xyzFromSpawn(item);
        if (pos != null) {
            tl.x = pos[0];
            tl.y = pos[1];
            tl.z = pos[2];
        }
        st.loot.put(tl.entityId, tl);
        publishSnapshot(key, st);
    }

    private void onEntityDespawn(EntityDespawnEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateByMachine.get(key);
        if (st == null) {
            return;
        }
        int id = e.getEntityId();
        st.monsters.remove(id);
        st.loot.remove(id);
        Integer tgt = st.currentTargetEntityId;
        if (tgt != null && tgt.intValue() == id) {
            st.currentTargetEntityId = null;
        }
        publishSnapshot(key, st);
    }

    private void onEntityMovement(EntityMovementEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        GamePosition cp = e.getCurrentPosition();
        if (cp == null) {
            return;
        }
        float[] pos = CombatPositions.xyz(cp);
        if (pos == null) {
            return;
        }
        Integer selfId = st.selfEntityId;
        if (selfId != null && e.getEntityId() == selfId.intValue()) {
            applySelfPosition(st, pos);
            publishSnapshot(key, st);
            return;
        }
        TacticalMonster tm = st.monsters.get(e.getEntityId());
        if (tm != null) {
            tm.x = pos[0];
            tm.y = pos[1];
            tm.z = pos[2];
            publishSnapshot(key, st);
            return;
        }
        TacticalLoot loot = st.loot.get(e.getEntityId());
        if (loot != null) {
            loot.x = pos[0];
            loot.y = pos[1];
            loot.z = pos[2];
        }
        publishSnapshot(key, st);
    }

    private void onHpMp(EntityHPMPUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        int id = e.getEntityId();
        Integer selfId = st.selfEntityId;
        if (selfId != null && id == selfId.intValue()) {
            if (e.getNewHP() != null) {
                st.currentHp = e.getNewHP();
                st.maxHp = Math.max(st.maxHp, st.currentHp);
            }
            if (e.getNewMP() != null) {
                st.currentMp = e.getNewMP();
                st.maxMp = Math.max(st.maxMp, st.currentMp);
            }
            publishSnapshot(key, st);
            return;
        }
        TacticalMonster tm = st.monsters.get(id);
        if (tm != null && e.getNewHP() != null) {
            tm.currentHp = e.getNewHP();
            tm.maxHp = Math.max(tm.maxHp, tm.currentHp);
        }
        publishSnapshot(key, st);
    }

    private void onEntityState(EntityStateUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        Integer selfId = st.selfEntityId;
        int id = e.getEntityId();
        if (selfId != null && id == selfId.intValue()) {
            if (e.getCurrentHP() != null) {
                st.currentHp = e.getCurrentHP();
                st.maxHp = Math.max(st.maxHp, st.currentHp);
            }
            if (e.getCurrentMP() != null) {
                st.currentMp = e.getCurrentMP();
                st.maxMp = Math.max(st.maxMp, st.currentMp);
            }
            publishSnapshot(key, st);
            return;
        }
        TacticalMonster tm = st.monsters.get(id);
        if (tm != null) {
            Integer tgt = st.currentTargetEntityId;
            if (tgt != null && tgt.intValue() == id) {
                tm.aggressiveTowardSelf = true;
            }
            if (e.getCurrentHP() != null) {
                tm.currentHp = e.getCurrentHP();
                tm.maxHp = Math.max(tm.maxHp, tm.currentHp);
            }
        }
        publishSnapshot(key, st);
    }

    private void onEntitySelected(EntitySelectedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        st.currentTargetEntityId = e.getSelectedEntityId();
        publishSnapshot(key, st);
    }

    private void onEntityDeselected(EntityDeselectedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        st.currentTargetEntityId = null;
        publishSnapshot(key, st);
    }

    private void onSkillCast(SkillCastEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        Integer caster = e.getCasterId();
        Integer target = e.getTargetId();
        Integer selfId = st.selfEntityId;
        if (caster != null && target != null && selfId != null && target.intValue() == selfId.intValue()) {
            TacticalMonster tmAggro = st.monsters.get(caster.intValue());
            if (tmAggro != null) {
                tmAggro.aggressiveTowardSelf = true;
            }
        }
        if (selfId != null && caster != null && caster.intValue() == selfId.intValue()) {
            st.skillCastInFlight = true;
            st.lastSkillCastEventEpochMs = e.getTimestamp();
        }
        publishSnapshot(key, st);
    }

    private void onSkillCastEnd(SkillCastEndEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        Integer selfId = st.selfEntityId;
        if (selfId == null || e.getCasterId() != selfId.intValue()) {
            return;
        }
        st.skillCastInFlight = false;
        long readyAt = e.getTimestamp() + DEFAULT_SKILL_COOLDOWN_MS;
        st.skillCooldownReadyAtEpochMs.put(e.getSkillId(), readyAt);
        publishSnapshot(key, st);
    }

    private void onSkillCastError(SkillCastErrorEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        st.skillCastInFlight = false;
        publishSnapshot(key, st);
    }

    private void onPickupAnimation(PickupAnimationEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        Integer selfId = st.selfEntityId;
        if (selfId == null || e.getEntityId() != selfId.intValue()) {
            return;
        }
        st.loot.remove(e.getTargetItemId());
        publishSnapshot(key, st);
    }

    private void onBerserk(BerserkConfirmEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        if (!e.isSuccess()) {
            return;
        }
        MachineCombatState st = stateFor(key);
        st.berserkActiveUntilEpochMs = e.getTimestamp() + BerserkDurationTable.durationMsForLevel(e.getBerserkLevel());
        publishSnapshot(key, st);
    }

    private void onBuffApplied(BuffAppliedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        long appliedAt = e.getTimestamp();
        int buffId = e.getBuffId();
        int skillRefId = buffId;
        int storageKey = buffStorageKey(buffId, skillRefId);
        Integer selfId = st.selfEntityId;
        boolean fromSelf = selfId != null && e.getCasterId() == selfId.intValue();
        ActiveBuff buff = ActiveBuff.builder()
                .buffId(buffId)
                .skillRefId(skillRefId)
                .casterEntityId(e.getCasterId())
                .appliedAtEpochMs(appliedAt)
                .expiresAtEpochMs(expiresAtEpochMsFromDuration(appliedAt, e.getDuration()))
                .fromSelf(fromSelf)
                .imbue(false)
                .build();
        st.activeBuffsById.put(storageKey, buff);
        publishSnapshot(key, st);
    }

    private void onBuffRemoved(BuffRemovedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        int buffId = e.getBuffId();
        int storageKey = buffStorageKey(buffId, buffId);
        st.activeBuffsById.remove(storageKey);
        publishSnapshot(key, st);
    }

    private void onCharacterBuffLoaded(CharacterBuffLoadedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineCombatState st = stateFor(key);
        long appliedAt = e.getTimestamp();
        int buffId = e.getBuffId();
        int skillRefId = buffId;
        int storageKey = buffStorageKey(buffId, skillRefId);
        int casterId = st.selfEntityId != null ? st.selfEntityId.intValue() : 0;
        ActiveBuff buff = ActiveBuff.builder()
                .buffId(buffId)
                .skillRefId(skillRefId)
                .casterEntityId(casterId)
                .appliedAtEpochMs(appliedAt)
                .expiresAtEpochMs(expiresAtEpochMsFromDuration(appliedAt, e.getDuration()))
                .fromSelf(true)
                .imbue(false)
                .build();
        st.activeBuffsById.put(storageKey, buff);
        publishSnapshot(key, st);
    }

    private void onLifeState(LifeStateUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        if (!e.isDead()) {
            return;
        }
        MachineCombatState st = stateFor(key);
        st.monsters.remove(e.getUniqueId());
        Integer tgt = st.currentTargetEntityId;
        if (tgt != null && tgt.intValue() == e.getUniqueId()) {
            st.currentTargetEntityId = null;
        }
        publishSnapshot(key, st);
    }

    private static void applySelfPosition(MachineCombatState st, float[] pos) {
        if (pos == null) {
            return;
        }
        st.selfX = pos[0];
        st.selfY = pos[1];
        st.selfZ = pos[2];
    }

    @Override
    public Optional<ICombatSnapshot> snapshot(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        MachineCombatState st = stateByMachine.get(key);
        if (st == null) {
            return Optional.empty();
        }
        return Optional.of(buildSnapshot(key, st));
    }

    @Override
    public Flux<ICombatSnapshot> observe(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Flux.empty();
        }
        MachineCombatState st = stateFor(key);
        Flux<ICombatSnapshot> tail = st.snapshotSink.asFlux().sample(Duration.ofMillis(50));
        Optional<ICombatSnapshot> seed = snapshot(machineFullName);
        if (seed.isPresent()) {
            return Flux.concat(Flux.just(seed.get()), tail);
        }
        return tail;
    }

    @Override
    public void resetCooldowns(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return;
        }
        MachineCombatState st = stateByMachine.get(key);
        if (st != null) {
            st.resetCooldowns();
            publishSnapshot(key, st);
        }
    }

    /**
     * Map key: prefer {@code buffId}; when {@code buffId == 0}, use {@code skillRefId} (events today carry only
     * buff id).
     */
    private static int buffStorageKey(int buffId, int skillRefId) {
        return buffId != 0 ? buffId : skillRefId;
    }

    private static long expiresAtEpochMsFromDuration(long appliedAtEpochMs, int durationSeconds) {
        if (durationSeconds <= 0) {
            return Long.MAX_VALUE;
        }
        return appliedAtEpochMs + durationSeconds * 1000L;
    }

    private static void removeExpiredBuffs(MachineCombatState st, long nowEpochMs) {
        st.activeBuffsById.entrySet()
                .removeIf(en -> en.getValue().getExpiresAtEpochMs() < nowEpochMs);
    }

    private static int hpPercent(int hp, int maxHp) {
        if (maxHp <= 0) {
            return -1;
        }
        return (int) Math.min(100L, (100L * hp) / maxHp);
    }

    private ICombatSnapshot buildSnapshot(String machineFullName, MachineCombatState st) {
        long now = System.currentTimeMillis();
        removeExpiredBuffs(st, now);

        if (st.skillCastInFlight && st.lastSkillCastEventEpochMs > 0L
                && now - st.lastSkillCastEventEpochMs > SKILL_CAST_WATCHDOG_MS) {
            st.skillCastInFlight = false;
        }

        float[] selfPos = new float[] { st.selfX, st.selfY, st.selfZ };

        Optional<float[]> anchorOpt = leashAnchorStore.getAnchor(machineFullName);

        List<MonsterRef> monsters = new ArrayList<>();
        for (TacticalMonster tm : st.monsters.values()) {
            float[] mpos = new float[] { tm.x, tm.y, tm.z };
            float dist = CombatPositions.distance(selfPos, mpos);
            int pct = hpPercent(tm.currentHp, tm.maxHp);
            Optional<Float> distFromAnchor = Optional.empty();
            if (anchorOpt.isPresent()) {
                float[] ap = anchorOpt.get();
                distFromAnchor = Optional.of(Float.valueOf(CombatPositions.distance(ap, mpos)));
            }
            Optional<Integer> firstAttacker = mobOwnershipTracker.getFirstAttackerEntityId(machineFullName,
                    tm.entityId);
            MonsterTier tier = MonsterTierMapping.from(tm.monsterType);
            monsters.add(new MonsterRef(tm.entityId, tm.refObjId, tm.levelOrZero, dist, pct, tm.aggressiveTowardSelf,
                    tm.championOrUnique(), firstAttacker, distFromAnchor, tier));
        }

        List<DroppedItemRef> drops = new ArrayList<>();
        for (TacticalLoot l : st.loot.values()) {
            float[] lpos = new float[] { l.x, l.y, l.z };
            float dist = CombatPositions.distance(selfPos, lpos);
            Integer owner = l.ownerEntityId;
            if (owner != null && owner.intValue() == 0) {
                owner = null;
            }
            drops.add(new DroppedItemRef(l.entityId, l.itemRefId, owner, l.ownerExpiresAtEpochMs, dist));
        }

        Map<Integer, Long> cds = new HashMap<>(st.skillCooldownReadyAtEpochMs);

        List<ActiveBuff> activeBuffs = new ArrayList<>(st.activeBuffsById.values());

        return CombatSnapshot.builder(machineFullName)
                .snapshotEpochMs(now)
                .selfEntityId(st.selfEntityId)
                .currentTargetEntityId(st.currentTargetEntityId)
                .currentHp(st.currentHp)
                .maxHp(Math.max(1, st.maxHp))
                .currentMp(st.currentMp)
                .maxMp(Math.max(1, st.maxMp))
                .selfPosition(st.selfX, st.selfY, st.selfZ)
                .nearbyMonsters(monsters)
                .nearbyLoot(drops)
                .skillCooldownReadyAtEpochMs(cds)
                .skillCastInFlight(st.skillCastInFlight)
                .berserkActiveUntilEpochMs(st.berserkActiveUntilEpochMs)
                .activeBuffs(activeBuffs)
                .build();
    }
}
