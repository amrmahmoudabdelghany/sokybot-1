package org.sokybot.behaviors.logistics;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.IQuartermasterCoordinator;
import org.sokybot.behaviors.logistics.internal.quartermaster.QuartermasterSortPlanner;
import org.sokybot.behaviors.logistics.internal.quartermaster.QuartermasterSortPlanner.PlannedMove;
import org.sokybot.behaviors.logistics.internal.settings.quartermaster.QuartermasterSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.storage.api.IGuildStorageSnapshot;
import org.sokybot.storage.api.IStorageModel;
import org.sokybot.storage.api.StorageType;
import org.sokybot.swarm.api.quartermaster.QuartermasterBlackboardKeys;

/**
 * Epic #15 Phase 3: quartermaster guild vault tidy pass after a farmer {@link StorageDumpBehavior} release.
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE, property = "order=5")
public final class QuartermasterSortBehavior implements IBehavior<LogisticsSettings> {

    private static final String QM_SCOPE = "quartermaster";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IStorageModel storageModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IQuartermasterCoordinator vaultCoordinator;

    @Override
    public String id() {
        return "quartermaster-sort";
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return LogisticsCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<LogisticsSettings> settingsType() {
        return LogisticsSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext ctx, LogisticsSettings cfg) {
        try {
            if (ctx == null || settingsRegistry == null) {
                return false;
            }
            QuartermasterSettings qm = readQuartermaster(ctx);
            if (qm == null || !qm.isQuartermasterEnabled()) {
                return false;
            }
            Map<String, Object> pd = ctx.getPersistentData();
            return Boolean.TRUE.equals(pd.get(QuartermasterBlackboardKeys.KEY_QUARTERMASTER_SORT_NEEDED));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        try {
            if (ctx == null || storageModel == null) {
                return BehaviorStatus.SKIPPED;
            }
            QuartermasterSettings qm = readQuartermaster(ctx);
            if (qm == null || !qm.isQuartermasterEnabled()) {
                return BehaviorStatus.SKIPPED;
            }

            IGuildStorageSnapshot guild = storageModel.guild(ctx.getMachineId()).orElse(null);
            if (guild == null || !guild.isFresh()) {
                ctx.log("INFO", "Quartermaster sort: guild snapshot not ready — retry next tick");
                return BehaviorStatus.EXECUTED;
            }

            long staleMs = qm.getLockTtlMs() > 0L ? qm.getLockTtlMs() : 30_000L;
            if (!storageModel.isStorageFresh(ctx.getMachineId(), StorageType.GUILD, staleMs)) {
                ctx.log("INFO", "Quartermaster sort: guild storage projection stale — refresh window first");
                return BehaviorStatus.EXECUTED;
            }

            List<PlannedMove> plan = QuartermasterSortPlanner.plan(guild, qm);
            Map<String, Object> pd = ctx.getPersistentData();

            if (plan.isEmpty()) {
                pd.remove(QuartermasterBlackboardKeys.KEY_QUARTERMASTER_SORT_NEEDED);
                IQuartermasterCoordinator coord = vaultCoordinator;
                if (coord != null) {
                    coord.resetVault(ctx.getGroupName());
                }
                ctx.log("INFO", "Quartermaster sort: vault tidy complete for group {}", ctx.getGroupName());
                return BehaviorStatus.EXECUTED;
            }

            long budgetLong = qm.getSortBudgetMovesPerTick();
            int budget = budgetLong <= 0L ? 1 : (int) Math.min(budgetLong, Integer.MAX_VALUE);
            int use = Math.min(budget, plan.size());
            for (int i = 0; i < use; i++) {
                PlannedMove m = plan.get(i);
                emitSortMovePlaceholder(ctx, m);
            }
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            ctx.log("WARN", "QuartermasterSortBehavior: {}", ex.toString());
            return BehaviorStatus.SKIPPED;
        }
    }

    private void emitSortMovePlaceholder(IWorkflowContext ctx, PlannedMove m) {
        ctx.log(
                "INFO",
                "Quartermaster sort placeholder kind={} fromSlot={} toSlot={} itemRef={} qty={} — wire guild storage "
                        + "slot-move opcodes on IDispatcher when catalogued",
                m.kind,
                Integer.valueOf(m.fromSlot),
                Integer.valueOf(m.toSlot),
                Integer.valueOf(m.itemRefId),
                Integer.valueOf(m.quantity));
        // Placeholder: ctx.getDispatcher() → inventory/storage move packet for guild tabs (game-specific opcode).
    }

    private QuartermasterSettings readQuartermaster(IWorkflowContext ctx) {
        try {
            ISettingsRegistry reg = settingsRegistry;
            if (reg == null) {
                return null;
            }
            ISettingsProvider<QuartermasterSettings> p = reg.getProvider(
                    ctx.getGroupName(), ctx.getMachineName(), QM_SCOPE, QuartermasterSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public long postDelayMs() {
        return 350L;
    }
}
