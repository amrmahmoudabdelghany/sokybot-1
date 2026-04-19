package org.sokybot.behaviors.combat.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.IRecoveryStrategy;
import org.sokybot.combat.api.RecoveryAction;
import org.sokybot.combat.api.RecoveryActionKind;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.model.IItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class RecoverHpMpBehavior implements IBehavior<CombatSettings> {

    private static final Logger log = LoggerFactory.getLogger(RecoverHpMpBehavior.class);
    private static final String HP_PATTERN = "_HP_POTION_";
    private static final String MP_PATTERN = "_MP_POTION_";

    @Reference
    private ICombatModel combatModel;

    @Reference
    private IRecoveryStrategy recoveryStrategy;

    @Override
    public String id() {
        return CombatCycleKeys.BEHAVIOR_RECOVER;
    }

    @Override
    public int order() {
        return 10;
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
        ICombatPolicy policy = settings.toPolicy();
        if (isRecoveryThrottleActive(context.getPersistentData(), policy.getRecoveryCooldownMs())) {
            return false;
        }
        return recoveryStrategy.nextRecovery(snap.get(), policy).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatPolicy policy = settings.toPolicy();
        if (isRecoveryThrottleActive(context.getPersistentData(), policy.getRecoveryCooldownMs())) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<RecoveryAction> ra = recoveryStrategy.nextRecovery(snap.get(), policy);
        if (!ra.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        RecoveryAction action = ra.get();
        RecoveryActionKind k = action.getKind();
        if (k != RecoveryActionKind.HP_POTION && k != RecoveryActionKind.MP_POTION) {
            return BehaviorStatus.SKIPPED;
        }
        int wantRef = action.getItemRefId();
        String pattern = k == RecoveryActionKind.HP_POTION ? HP_PATTERN : MP_PATTERN;
        if (wantRef > 0) {
            pattern = null;
        }
        IItem item = findPotion(context, wantRef, pattern);
        if (item == null) {
            log.debug("Recover: no potion item (kind={})", k);
            return BehaviorStatus.SKIPPED;
        }
        CombatPackets.sendInventoryItemUse(context, item.getSlot(), item.getRefId());
        context.getPersistentData().put(CombatCycleKeys.KEY_RECOVERY_LAST_ACTION_AT_MS,
                Long.valueOf(System.currentTimeMillis()));
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
        return 800;
    }

    private static boolean isRecoveryThrottleActive(Map<String, Object> persistentData, int cooldownMs) {
        if (cooldownMs <= 0) {
            return false;
        }
        Object raw = persistentData.get(CombatCycleKeys.KEY_RECOVERY_LAST_ACTION_AT_MS);
        long last = raw instanceof Number ? ((Number) raw).longValue() : 0L;
        if (last <= 0L) {
            return false;
        }
        long now = System.currentTimeMillis();
        return now - last < cooldownMs;
    }

    private static IItem findPotion(IWorkflowContext ctx, int refId, String longIdPattern) {
        List<IItem> items = ctx.getGameModel().snapshotAll(IItem.class);
        if (items == null) {
            return null;
        }
        for (IItem item : items) {
            int slotUnsigned = item.getSlot() & 0xFF;
            if (slotUnsigned >= 128) {
                continue;
            }
            if (refId > 0 && item.getRefId() == refId) {
                return item;
            }
            if (refId <= 0 && longIdPattern != null && item.getLongId() != null
                    && item.getLongId().contains(longIdPattern)) {
                return item;
            }
        }
        return null;
    }
}
