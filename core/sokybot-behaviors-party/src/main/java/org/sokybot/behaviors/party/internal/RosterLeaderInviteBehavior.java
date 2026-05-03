package org.sokybot.behaviors.party.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.party.internal.settings.PartySettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.IPlayer;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.party.api.IPartyDirectory;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.party.api.PartyMatrixSettings;
import org.sokybot.party.coordination.api.IPartyCoordinator;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.RosterBlackboardKeys;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Party leader sends client invites when pending recruits are within matrix proximity (Epic #17 Phase 4).
 * {@link org.sokybot.party.coordination.api.IPartyCoordinator#requestInvite} is primed by the swarm supervisor when dispatch wins.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=2")
public final class RosterLeaderInviteBehavior implements IBehavior<PartySettings> {

    private static final String PARTY_MATRIX_SCOPE = "party-matrix";

    private static final float DEFAULT_INVITE_RADIUS = 20.0f;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPartyCoordinator partyCoordinator;

    @Reference
    private IPartyDirectory partyDirectory;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Override
    public String id() {
        return PartyCycleKeys.BEHAVIOR_ROSTER_LEADER_INVITE;
    }

    @Override
    public int order() {
        return 2;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return PartyCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<PartySettings> settingsType() {
        return PartySettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, PartySettings settings) {
        Set<String> pending = pendingInvites(context.getPersistentData());
        return pending != null && !pending.isEmpty();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, PartySettings settings) {
        Map<String, Object> pd = context.getPersistentData();
        Set<String> pending = pendingInvites(pd);
        if (pending == null || pending.isEmpty()) {
            return BehaviorStatus.SKIPPED;
        }
        ITownModel tm = townModel;
        if (tm == null) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<ITownSnapshot> selfTown = tm.snapshot(context.getMachineId());
        if (!selfTown.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        float radius = inviteRadius(context.getMachineId());
        List<String> snapshot = new ArrayList<>(pending);
        for (String recruitName : snapshot) {
            if (recruitName == null || recruitName.trim().isEmpty()) {
                continue;
            }
            if (!pending.contains(recruitName)) {
                continue;
            }
            if (!playerInRange(context, selfTown.get(), recruitName.trim(), radius)) {
                continue;
            }
            String trimmed = recruitName.trim();
            Optional<String> inviteeMachine = partyDirectory.resolveMachineForCharacterName(trimmed);
            IPartyCoordinator coord = partyCoordinator;
            if (coord != null && inviteeMachine.isPresent()) {
                try {
                    coord.requestInvite(context.getMachineId(), inviteeMachine.get());
                } catch (Exception ex) {
                    context.log("WARN", "Roster leader coordinator priming failed: {}", ex.getMessage());
                }
            }
            PartyPackets.sendInvite(context, trimmed);
            pending.remove(recruitName);
            return BehaviorStatus.EXECUTED;
        }
        return BehaviorStatus.SKIPPED;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> pendingInvites(Map<String, Object> pd) {
        if (pd == null) {
            return null;
        }
        Object raw = pd.get(RosterBlackboardKeys.KEY_PENDING_ROSTER_INVITES);
        if (!(raw instanceof Set<?>)) {
            return null;
        }
        return (Set<String>) raw;
    }

    private float inviteRadius(String machineFullName) {
        try {
            int dot = machineFullName.indexOf('.');
            if (dot <= 0 || dot >= machineFullName.length() - 1) {
                return DEFAULT_INVITE_RADIUS;
            }
            String group = machineFullName.substring(0, dot);
            String machine = machineFullName.substring(dot + 1);
            ISettingsProvider<PartyMatrixSettings> provider = settingsRegistry.getProvider(
                    group,
                    machine,
                    PARTY_MATRIX_SCOPE,
                    PartyMatrixSettings.class);
            PartyMatrixSettings mx = provider != null ? provider.get() : null;
            if (mx == null) {
                return DEFAULT_INVITE_RADIUS;
            }
            float r = mx.getInviteProximityRadius();
            return r > 0f ? r : DEFAULT_INVITE_RADIUS;
        } catch (Exception e) {
            return DEFAULT_INVITE_RADIUS;
        }
    }

    private static boolean playerInRange(
            IWorkflowContext ctx,
            ITownSnapshot selfTown,
            String recruitName,
            float radius) {
        IGameModel gm = ctx.getGameModel();
        if (gm == null || selfTown == null) {
            return false;
        }
        ITrainer trainer = gm.getTrainer();
        int selfUid = trainer != null ? trainer.getUniqueId() : Integer.MIN_VALUE;
        float lx = selfTown.getSelfX();
        float ly = selfTown.getSelfY();
        float lz = selfTown.getSelfZ();
        float r2 = radius * radius;
        String needle = recruitName.trim();
        for (IPlayer p : gm.snapshotAll(IPlayer.class)) {
            if (p == null || p.getUniqueId() == selfUid) {
                continue;
            }
            String name = p.getName();
            if (name == null || !name.trim().equalsIgnoreCase(needle)) {
                continue;
            }
            GamePosition pos = p.getPosition();
            if (pos == null) {
                continue;
            }
            float dx = pos.getX() - lx;
            float dy = pos.getY() - ly;
            float dz = pos.getZ() - lz;
            float d2 = dx * dx + dy * dy + dz * dz;
            if (d2 <= r2) {
                return true;
            }
        }
        return false;
    }
}
