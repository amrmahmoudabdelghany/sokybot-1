package org.sokybot.behaviors.party.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sokybot.combat.api.ActiveBuff;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.party.api.PartyMember;

final class PartyBuffShareLogic {

    private static final long MIN_BUFF_GAP_MS = 1800L;

    private PartyBuffShareLogic() {
    }

    static boolean shouldBuff(ICombatSnapshot combat, IPartySnapshot party, IPartyPolicy policy, Map<String, Object> pd) {
        List<Integer> ids = policy.getBuffShareSkillIds();
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Object raw = pd.get(PartyCycleKeys.KEY_LAST_PARTY_BUFF_AT_MS);
        long last = raw instanceof Number ? ((Number) raw).longValue() : 0L;
        if (now - last < MIN_BUFF_GAP_MS) {
            return false;
        }
        if (combat.isSkillCastInFlight()) {
            return false;
        }
        Object idxRaw = pd.get(PartyCycleKeys.KEY_BUFF_SKILL_INDEX);
        int idx = idxRaw instanceof Number ? ((Number) idxRaw).intValue() : 0;
        int skillRef = ids.get(Math.floorMod(idx, ids.size())).intValue();
        Optional<Integer> self = combat.getSelfEntityId();
        if (!self.isPresent()) {
            return false;
        }
        PartyMember target = pickBuffTarget(party, self.get().intValue(), skillRef, combat);
        return target != null && skillReady(combat, skillRef);
    }

    static BehaviorStatus executeBuff(IWorkflowContext ctx, ICombatSnapshot combat, IPartySnapshot party,
            IPartyPolicy policy, Map<String, Object> pd) {
        List<Integer> ids = policy.getBuffShareSkillIds();
        if (ids == null || ids.isEmpty()) {
            return BehaviorStatus.SKIPPED;
        }
        Object idxRaw = pd.get(PartyCycleKeys.KEY_BUFF_SKILL_INDEX);
        int idx = idxRaw instanceof Number ? ((Number) idxRaw).intValue() : 0;
        int rot = ids.size();
        int skillRef = ids.get(Math.floorMod(idx, rot)).intValue();
        Optional<Integer> self = combat.getSelfEntityId();
        if (!self.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        PartyMember target = pickBuffTarget(party, self.get().intValue(), skillRef, combat);
        if (target == null || !skillReady(combat, skillRef)) {
            pd.put(PartyCycleKeys.KEY_BUFF_SKILL_INDEX, Integer.valueOf(idx + 1));
            return BehaviorStatus.SKIPPED;
        }
        PartyPackets.sendSkillCast(ctx, skillRef, target.getEntityId());
        pd.put(PartyCycleKeys.KEY_BUFF_SKILL_INDEX, Integer.valueOf(idx + 1));
        pd.put(PartyCycleKeys.KEY_LAST_PARTY_BUFF_AT_MS, Long.valueOf(System.currentTimeMillis()));
        return BehaviorStatus.EXECUTED;
    }

    private static PartyMember pickBuffTarget(IPartySnapshot party, int selfId, int skillRef,
            ICombatSnapshot combat) {
        List<PartyMember> members = party.getMembers();
        if (members == null || members.isEmpty()) {
            return null;
        }
        for (PartyMember m : members) {
            if (m == null || m.getEntityId() <= 0 || m.getEntityId() == selfId) {
                continue;
            }
            if (!buffLikelyPresent(combat, m.getEntityId(), skillRef)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Remote buff presence is unknown; only skip when local combat snapshot matches target entity (rare solo edge).
     */
    private static boolean buffLikelyPresent(ICombatSnapshot combat, int targetEntityId, int skillRefId) {
        Optional<Integer> self = combat.getSelfEntityId();
        if (self.isPresent() && self.get().intValue() == targetEntityId) {
            return buffPresentSelf(combat, skillRefId);
        }
        return false;
    }

    private static boolean buffPresentSelf(ICombatSnapshot combat, int skillRefId) {
        for (ActiveBuff b : combat.getActiveBuffs()) {
            if (b.getSkillRefId() == skillRefId) {
                long exp = b.getExpiresAtEpochMs();
                return exp == Long.MAX_VALUE || exp > System.currentTimeMillis();
            }
        }
        return false;
    }

    private static boolean skillReady(ICombatSnapshot combat, int skillRefId) {
        long now = System.currentTimeMillis();
        Long ready = combat.getSkillCooldownReadyAtEpochMs().get(Integer.valueOf(skillRefId));
        return ready == null || ready.longValue() <= now;
    }
}
