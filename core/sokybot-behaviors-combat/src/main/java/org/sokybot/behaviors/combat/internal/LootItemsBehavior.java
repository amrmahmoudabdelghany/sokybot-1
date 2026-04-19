package org.sokybot.behaviors.combat.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.DroppedItemRef;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.ILootingStrategy;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.pet.api.IPetModel;
import org.sokybot.pet.api.PetRole;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class LootItemsBehavior implements IBehavior<CombatSettings> {

    @Reference
    private ICombatModel combatModel;

    @Reference
    private ILootingStrategy lootingStrategy;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPetModel petModel;

    @Override
    public String id() {
        return CombatCycleKeys.BEHAVIOR_LOOT;
    }

    @Override
    public int order() {
        return 20;
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
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || settings == null) {
            return false;
        }
        if (snap.get().isSkillCastInFlight()) {
            return false;
        }
        IPetModel pm = petModel;
        if (pm != null && pm.isPetActive(context.getMachineId(), PetRole.GRAB_PET)) {
            return false;
        }
        return lootingStrategy.pickNext(snap.get(), settings.toPolicy()).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<DroppedItemRef> pick = lootingStrategy.pickNext(snap.get(), settings.toPolicy());
        if (!pick.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        CombatPackets.sendCharActionPickup(context, pick.get().getEntityId());
        context.getPersistentData().put(CombatCycleKeys.KEY_COMBAT_PHASE, CombatCycleKeys.PHASE_LOOTING);
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 300L;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public int interruptionPriority() {
        return 400;
    }
}
