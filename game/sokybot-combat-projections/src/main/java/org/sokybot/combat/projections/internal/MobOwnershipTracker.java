package org.sokybot.combat.projections.internal;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.combat.api.IMobOwnershipTracker;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gameevents.events.skill.SkillCastEvent;
import org.sokybot.gameevents.events.spawn.MonsterSpawnEvent;

import reactor.core.Disposable;

/**
 * Records first skill caster per monster from {@link SkillCastEvent}; tracks monster ids from spawns.
 * {@link EntityHPMPUpdateEvent} is subscribed for forward compatibility (e.g. future attribution heuristics).
 */
@Component(service = IMobOwnershipTracker.class, immediate = true)
public final class MobOwnershipTracker implements IMobOwnershipTracker {

    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> firstSkillCasterByMonster = new ConcurrentHashMap<>();
    /** Known monster entity ids per machine (for validating skill targets). */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> monsterIdsByMachine = new ConcurrentHashMap<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    private Disposable skillSub;
    private Disposable spawnSub;
    private Disposable despawnSub;
    private Disposable hpSub;

    @Activate
    void activate() {
        skillSub = reactiveEventBus.on(SkillCastEvent.class).subscribe(this::onSkillCast);
        spawnSub = reactiveEventBus.on(MonsterSpawnEvent.class).subscribe(this::onMonsterSpawn);
        despawnSub = reactiveEventBus.on(EntityDespawnEvent.class).subscribe(this::onEntityDespawn);
        hpSub = reactiveEventBus.on(EntityHPMPUpdateEvent.class).subscribe(this::onHpMp);
    }

    @Deactivate
    void deactivate() {
        dispose(skillSub);
        dispose(spawnSub);
        dispose(despawnSub);
        dispose(hpSub);
        firstSkillCasterByMonster.clear();
        monsterIdsByMachine.clear();
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private void onMonsterSpawn(MonsterSpawnEvent e) {
        String key = normalize(e.getFullName());
        if (key == null || e.getMonster() == null) {
            return;
        }
        int id = e.getMonster().getUniqueId();
        monsterIdsByMachine.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(Integer.valueOf(id),
                Boolean.TRUE);
    }

    private void onEntityDespawn(EntityDespawnEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        int id = e.getEntityId();
        ConcurrentHashMap<Integer, Boolean> ids = monsterIdsByMachine.get(key);
        if (ids != null) {
            ids.remove(Integer.valueOf(id));
        }
        ConcurrentHashMap<Integer, Integer> owners = firstSkillCasterByMonster.get(key);
        if (owners != null) {
            owners.remove(Integer.valueOf(id));
        }
    }

    private void onSkillCast(SkillCastEvent e) {
        String key = normalize(e.getFullName());
        if (key == null || !e.isSuccess()) {
            return;
        }
        Integer target = e.getTargetId();
        Integer caster = e.getCasterId();
        if (target == null || caster == null) {
            return;
        }
        ConcurrentHashMap<Integer, Boolean> monsters = monsterIdsByMachine.get(key);
        if (monsters == null || !monsters.containsKey(target)) {
            return;
        }
        firstSkillCasterByMonster.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).putIfAbsent(target, caster);
    }

    private void onHpMp(EntityHPMPUpdateEvent e) {
        // Reserved: HP deltas could backfill attribution when skill events are reordered or missing.
    }

    @Override
    public Optional<Integer> getFirstAttackerEntityId(String machineFullName, int monsterEntityId) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        ConcurrentHashMap<Integer, Integer> m = firstSkillCasterByMonster.get(key);
        if (m == null) {
            return Optional.empty();
        }
        Integer v = m.get(Integer.valueOf(monsterEntityId));
        return v == null ? Optional.empty() : Optional.of(v);
    }

    @Override
    public void forgetMonster(String machineFullName, int monsterEntityId) {
        String key = normalize(machineFullName);
        if (key == null) {
            return;
        }
        ConcurrentHashMap<Integer, Integer> m = firstSkillCasterByMonster.get(key);
        if (m != null) {
            m.remove(Integer.valueOf(monsterEntityId));
        }
        ConcurrentHashMap<Integer, Boolean> ids = monsterIdsByMachine.get(key);
        if (ids != null) {
            ids.remove(Integer.valueOf(monsterEntityId));
        }
    }
}
