package org.sokybot.behaviors.party.internal;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.party.api.PartyMember;

/**
 * Interrupting heal cast toward the lowest-HP party member below threshold.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class HealPartyBehavior implements IBehavior<CombatSettings> {

    private static final long MIN_HEAL_INTERVAL_MS = 600L;

    @Reference
    private ICombatModel combatModel;

    @Reference
    private IPartyModel partyModel;

    @Override
    public String id() {
        return PartyCycleKeys.BEHAVIOR_HEAL_PARTY;
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return CombatCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<CombatSettings> settingsType() {
        return CombatSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, CombatSettings settings) {
        IPartyPolicy policy = PartyPolicyResolver.resolve(context);
        if (!policy.isHealOthers() || policy.getHealSkillRefId() <= 0) {
            return false;
        }
        Optional<ICombatSnapshot> c = combatModel.snapshot(context.getMachineId());
        if (!c.isPresent() || c.get().isSkillCastInFlight()) {
            return false;
        }
        Optional<IPartySnapshot> p = partyModel.snapshot(context.getMachineId());
        if (!p.isPresent()) {
            return false;
        }
        PartyMember victim = pickHealTarget(policy, p.get(), c.get());
        return victim != null && skillReady(c.get(), policy.getHealSkillRefId());
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        IPartyPolicy policy = PartyPolicyResolver.resolve(context);
        int skillRef = policy.getHealSkillRefId();
        if (skillRef <= 0 || !policy.isHealOthers()) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<ICombatSnapshot> cOpt = combatModel.snapshot(context.getMachineId());
        Optional<IPartySnapshot> pOpt = partyModel.snapshot(context.getMachineId());
        if (!cOpt.isPresent() || !pOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatSnapshot combat = cOpt.get();
        if (combat.isSkillCastInFlight()) {
            return BehaviorStatus.SKIPPED;
        }
        PartyMember victim = pickHealTarget(policy, pOpt.get(), combat);
        if (victim == null || !skillReady(combat, skillRef)) {
            return BehaviorStatus.SKIPPED;
        }
        Map<String, Object> pd = context.getPersistentData();
        long now = System.currentTimeMillis();
        Object rawLast = pd.get(PartyCycleKeys.KEY_LAST_PARTY_HEAL_AT_MS);
        long last = rawLast instanceof Number ? ((Number) rawLast).longValue() : 0L;
        if (now - last < MIN_HEAL_INTERVAL_MS) {
            return BehaviorStatus.SKIPPED;
        }

        PartyPackets.sendSkillCast(context, skillRef, victim.getEntityId());
        pd.put(PartyCycleKeys.KEY_LAST_PARTY_HEAL_AT_MS, Long.valueOf(now));
        return BehaviorStatus.EXECUTED;
    }

    private static PartyMember pickHealTarget(IPartyPolicy policy, IPartySnapshot party, ICombatSnapshot combat) {
        int threshold = Math.max(0, Math.min(100, policy.getHealHpThresholdPercent()));
        List<PartyMember> members = party.getMembers();
        if (members == null || members.isEmpty()) {
            return null;
        }
        return members.stream()
                .filter(m -> m != null && m.getEntityId() > 0)
                .filter(m -> m.getHpPercent() >= 0 && m.getHpPercent() <= threshold)
                .min(Comparator.comparingInt(PartyMember::getHpPercent))
                .orElse(null);
    }

    private static boolean skillReady(ICombatSnapshot combat, int skillRefId) {
        long now = System.currentTimeMillis();
        Map<Integer, Long> cds = combat.getSkillCooldownReadyAtEpochMs();
        Long ready = cds.get(Integer.valueOf(skillRefId));
        return ready == null || ready.longValue() <= now;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public int interruptionPriority() {
        return 900;
    }
}
