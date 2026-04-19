package org.sokybot.behaviors.party.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.party.coordination.api.IPartyCoordinator;

/**
 * Shared auto-accept decision path for combat-cycle and party-cycle variants.
 */
final class PartyAutoAcceptLogic {

    private PartyAutoAcceptLogic() {
    }

    static boolean shouldRun(IWorkflowContext ctx, IPartySnapshot snap, Map<String, Object> pd, IPartyPolicy policy) {
        if (ctx == null || snap == null || policy == null || !policy.isAutoAcceptInvites()) {
            return false;
        }
        long lastInvite = snap.getLastInviteEpochMs();
        if (lastInvite <= 0L) {
            return false;
        }
        Object raw = pd.get(PartyCycleKeys.KEY_LAST_INVITE_DECISION_AT_MS);
        long decided = raw instanceof Number ? ((Number) raw).longValue() : 0L;
        return lastInvite > decided;
    }

    static BehaviorStatus execute(IWorkflowContext ctx, IPartyPolicy policy, IPartyCoordinator coordinator,
            IPartySnapshot snap) {
        boolean accept = false;
        if (coordinator != null && coordinator.onInviteReceived(ctx.getMachineId(), 0)) {
            accept = true;
        }
        if (!accept) {
            accept = policyAllows(policy, null);
        }
        PartyPackets.sendInviteResponse(ctx, accept);
        ctx.getPersistentData().put(PartyCycleKeys.KEY_LAST_INVITE_DECISION_AT_MS,
                Long.valueOf(snap.getLastInviteEpochMs()));
        ctx.log("INFO", "Party auto-accept invite decision accept={}", Boolean.valueOf(accept));
        return BehaviorStatus.EXECUTED;
    }

    /**
     * Without inviter name events, whitelist is permissive when empty (accept any); non-empty lists require a name match
     * once the projection exposes inviter identity.
     */
    static boolean policyAllows(IPartyPolicy policy, String inviterCharName) {
        if (!policy.isAutoAcceptInvites()) {
            return false;
        }
        List<String> wl = policy.getPartyInviteWhitelist();
        if (wl == null || wl.isEmpty()) {
            return true;
        }
        if (inviterCharName == null || inviterCharName.trim().isEmpty()) {
            return false;
        }
        String needle = inviterCharName.trim().toLowerCase(Locale.ROOT);
        for (String n : wl) {
            if (n != null && needle.equals(n.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
