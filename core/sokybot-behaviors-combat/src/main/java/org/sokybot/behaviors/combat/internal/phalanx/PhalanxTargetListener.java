package org.sokybot.behaviors.combat.internal.phalanx;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.enums.SkillCastErrorType;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.events.skill.SkillCastErrorEvent;
import org.sokybot.gameevents.events.stat.LifeStateUpdateEvent;
import org.sokybot.party.api.IPartyDirectory;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmTargetEngagedEvent;

import reactor.core.Disposable;

@Component(immediate = true, service = PhalanxTargetListener.class)
public final class PhalanxTargetListener {

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Reference
    private IPartyDirectory partyDirectory;

    private final ConcurrentMap<String, SwarmTargetEngagedEvent> latestByFollower = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SwarmTargetEngagedEvent> latestByLeader = new ConcurrentHashMap<>();

    private Disposable swarmSub;
    private Disposable despawnSub;
    private Disposable lifeSub;
    private Disposable skillErrSub;

    @Activate
    void activate() {
        swarmSub = swarmBus.observe(SwarmTargetEngagedEvent.class).subscribe(this::onSwarmTarget);
        despawnSub = reactiveEventBus.on(EntityDespawnEvent.class).subscribe(this::onDespawn);
        lifeSub = reactiveEventBus.on(LifeStateUpdateEvent.class).subscribe(this::onLifeState);
        skillErrSub = reactiveEventBus.on(SkillCastErrorEvent.class).subscribe(this::onSkillError);
    }

    @Deactivate
    void deactivate() {
        dispose(swarmSub);
        dispose(despawnSub);
        dispose(lifeSub);
        dispose(skillErrSub);
        latestByFollower.clear();
        latestByLeader.clear();
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onSwarmTarget(SwarmTargetEngagedEvent event) {
        latestByLeader.put(event.getLeaderMachineFullName(), event);
        for (Map.Entry<String, SwarmTargetEngagedEvent> entry : latestByFollower.entrySet()) {
            String followerMachine = entry.getKey();
            Optional<String> leader = partyDirectory.resolveLeaderMachine(followerMachine);
            if (leader.isPresent() && leader.get().equals(event.getLeaderMachineFullName())) {
                latestByFollower.put(followerMachine, event);
            }
        }
    }

    private void onDespawn(EntityDespawnEvent event) {
        clearByTargetEntity(event.getEntityId());
    }

    private void onLifeState(LifeStateUpdateEvent event) {
        if (event.isDead()) {
            clearByTargetEntity(event.getUniqueId());
        }
    }

    private void onSkillError(SkillCastErrorEvent event) {
        SkillCastErrorType t = event.getErrorType();
        if (t == SkillCastErrorType.OBSTACLE || t == SkillCastErrorType.INVALID_TARGET) {
            latestByFollower.remove(event.getFullName());
        }
    }

    private void clearByTargetEntity(int targetEntityId) {
        for (Map.Entry<String, SwarmTargetEngagedEvent> e : latestByFollower.entrySet()) {
            SwarmTargetEngagedEvent cached = e.getValue();
            if (cached != null && cached.getTargetEntityId() == targetEntityId) {
                latestByFollower.remove(e.getKey(), cached);
            }
        }
    }

    public Optional<SwarmTargetEngagedEvent> peekFresh(String followerMachineFullName, long stalenessMs) {
        if (followerMachineFullName == null || followerMachineFullName.trim().isEmpty()) {
            return Optional.empty();
        }
        String follower = followerMachineFullName.trim();
        SwarmTargetEngagedEvent cached = latestByFollower.get(follower);
        if (cached == null) {
            Optional<String> leader = partyDirectory.resolveLeaderMachine(follower);
            if (leader.isPresent()) {
                SwarmTargetEngagedEvent fromLeader = latestByLeader.get(leader.get());
                if (fromLeader != null) {
                    latestByFollower.put(follower, fromLeader);
                    cached = fromLeader;
                }
            }
        }
        if (cached == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        if (now - cached.getTimestampEpochMs() > Math.max(0L, stalenessMs)) {
            latestByFollower.remove(follower, cached);
            return Optional.empty();
        }
        Optional<String> leader = partyDirectory.resolveLeaderMachine(follower);
        if (!leader.isPresent() || !leader.get().equals(cached.getLeaderMachineFullName())) {
            latestByFollower.remove(follower, cached);
            return Optional.empty();
        }
        return Optional.of(cached);
    }
}
