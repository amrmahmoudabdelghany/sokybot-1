package org.sokybot.behaviors.combat.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.pet.api.IPetModel;
import org.sokybot.pet.api.IPetSnapshot;
import org.sokybot.pet.api.PetInfo;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class FeedPetBehavior implements IBehavior<CombatSettings> {

    private static final int DEFAULT_HUNGER_SCALE = 1000;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPetModel petModel;

    @Override
    public String id() {
        return CombatCycleKeys.BEHAVIOR_FEED_PET;
    }

    @Override
    public int order() {
        return 15;
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
        IPetModel model = petModel;
        if (model == null || settings == null) {
            return false;
        }
        ICombatPolicy policy = settings.toPolicy();
        int foodRef = policy.getPetFoodItemRefId();
        if (foodRef <= 0) {
            return false;
        }
        Map<String, Object> pd = context.getPersistentData();
        if (isThrottleActive(pd, CombatCycleKeys.KEY_RECOVERY_LAST_ACTION_AT_MS, policy.getRecoveryCooldownMs())) {
            return false;
        }
        if (isThrottleActive(pd, CombatCycleKeys.KEY_LAST_PET_FEED_AT_MS,
                Math.max(policy.getRecoveryCooldownMs(), 5000L))) {
            return false;
        }
        Optional<IPetSnapshot> snap = model.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return false;
        }
        int threshold = policy.getPetHungerThresholdPercent();
        for (PetInfo p : snap.get().getActivePets()) {
            if (isPetHungry(p, threshold)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        IPetModel model = petModel;
        if (model == null || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatPolicy policy = settings.toPolicy();
        int foodRef = policy.getPetFoodItemRefId();
        if (foodRef <= 0) {
            return BehaviorStatus.SKIPPED;
        }
        Map<String, Object> pd = context.getPersistentData();
        if (isThrottleActive(pd, CombatCycleKeys.KEY_RECOVERY_LAST_ACTION_AT_MS, policy.getRecoveryCooldownMs())) {
            return BehaviorStatus.SKIPPED;
        }
        if (isThrottleActive(pd, CombatCycleKeys.KEY_LAST_PET_FEED_AT_MS,
                Math.max(policy.getRecoveryCooldownMs(), 5000L))) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<IPetSnapshot> snap = model.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        int threshold = policy.getPetHungerThresholdPercent();
        boolean anyHungry = false;
        for (PetInfo p : snap.get().getActivePets()) {
            if (isPetHungry(p, threshold)) {
                anyHungry = true;
                break;
            }
        }
        if (!anyHungry) {
            return BehaviorStatus.SKIPPED;
        }
        IItem item = findFoodItem(context, foodRef);
        if (item == null) {
            return BehaviorStatus.SKIPPED;
        }
        CombatPackets.sendInventoryItemUse(context, item.getSlot(), item.getRefId());
        long now = System.currentTimeMillis();
        context.getPersistentData().put(CombatCycleKeys.KEY_RECOVERY_LAST_ACTION_AT_MS, Long.valueOf(now));
        context.getPersistentData().put(CombatCycleKeys.KEY_LAST_PET_FEED_AT_MS, Long.valueOf(now));
        context.getPersistentData().put(CombatCycleKeys.KEY_COMBAT_PHASE, CombatCycleKeys.PHASE_RECOVERING);
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public int interruptionPriority() {
        return 700;
    }

    private static boolean isPetHungry(PetInfo p, int hungerThresholdPercent) {
        if (p == null || !p.isAlive() || p.getMaxHp() <= 0) {
            return false;
        }
        int hunger = p.getHunger();
        if (hunger < 0) {
            return false;
        }
        int normalizedPercent = (hunger * 100) / DEFAULT_HUNGER_SCALE;
        return normalizedPercent <= hungerThresholdPercent;
    }

    private static boolean isThrottleActive(Map<String, Object> persistentData, String key, long cooldownMs) {
        if (cooldownMs <= 0 || persistentData == null) {
            return false;
        }
        Object raw = persistentData.get(key);
        long last = raw instanceof Number ? ((Number) raw).longValue() : 0L;
        if (last <= 0L) {
            return false;
        }
        long now = System.currentTimeMillis();
        return now - last < cooldownMs;
    }

    private static IItem findFoodItem(IWorkflowContext ctx, int foodRefId) {
        List<IItem> items = ctx.getGameModel().snapshotAll(IItem.class);
        if (items == null) {
            return null;
        }
        for (IItem item : items) {
            int slotUnsigned = item.getSlot() & 0xFF;
            if (slotUnsigned >= 128) {
                continue;
            }
            if (item.getRefId() == foodRefId && (item.getStackCount() & 0xFFFF) > 0) {
                return item;
            }
        }
        return null;
    }
}
