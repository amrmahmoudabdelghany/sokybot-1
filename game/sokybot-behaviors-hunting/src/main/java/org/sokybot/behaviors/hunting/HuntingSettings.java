package org.sokybot.behaviors.hunting;

import java.util.HashSet;
import java.util.Set;

import org.sokybot.swarm.api.ISwarmRadarPolicy;
import org.sokybot.trade.coordination.api.SwarmRole;

import lombok.Data;

@Data
public class HuntingSettings {

    private SwarmRole role = SwarmRole.NONE;
    private boolean huntDispatchEnabled = true;
    private float interactionRadius = 30f;
    private long maxHuntGoldCost = 200_000L;
    private long huntTimeoutMs = 180_000L;
    private boolean broadcastSocialAlert = true;
    private Set<Integer> watchedRefIds = new HashSet<>();

    public ISwarmRadarPolicy toRadarPolicy() {
        Set<Integer> watched = new HashSet<>(watchedRefIds);
        return new ISwarmRadarPolicy() {
            @Override
            public Set<Integer> watchedRefIds() {
                return watched;
            }
        };
    }
}
