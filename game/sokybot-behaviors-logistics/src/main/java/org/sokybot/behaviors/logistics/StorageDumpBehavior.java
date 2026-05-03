package org.sokybot.behaviors.logistics;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.internal.settings.quartermaster.QuartermasterSettings;
import org.sokybot.behaviors.logistics.quartermaster.StorageGrantLedger;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.quartermaster.QuartermasterBlackboardKeys;
import org.sokybot.swarm.api.quartermaster.SwarmStorageLockRequestEvent;
import org.sokybot.swarm.api.quartermaster.SwarmStorageReleaseEvent;
import org.sokybot.storage.api.IStorageModel;
import org.sokybot.storage.api.StorageType;
import org.sokybot.town.api.INpcInteractionFacade;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.ItemStackSnapshot;
import org.sokybot.town.api.NpcRole;
import org.sokybot.town.api.VendorRef;
import org.sokybot.town.projections.api.ITownDirectory;
import org.sokybot.town.projections.api.ITownModel;
import org.sokybot.trade.coordination.api.SwarmRole;

/**
 * Epic #15 Phase 3: farmer guild-vault dump behind the swarm storage mutex.
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE, property = "order=10")
public final class StorageDumpBehavior implements IBehavior<LogisticsSettings> {

    private static final String QM_SCOPE = "quartermaster";

    private static final String PHASE_REQUEST = "REQUEST";
    private static final String PHASE_WAIT = "WAIT";
    private static final String PHASE_DUMP = "DUMP";
    private static final String PHASE_RELEASE = "RELEASE";

    private static final long DEFAULT_WAIT_TIMEOUT_MS = 60_000L;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IStorageModel storageModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INpcInteractionFacade npcFacade;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownDirectory townDirectory;

    @Override
    public String id() {
        return "storage-dump";
    }

    @Override
    public int order() {
        return 10;
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
            if (ctx == null || cfg == null || swarmBus == null || townModel == null) {
                return false;
            }
            Map<String, Object> pd = ctx.getPersistentData();
            if (pd.containsKey(QuartermasterBlackboardKeys.KEY_STORAGE_DUMP_PHASE)) {
                return swarmBus != null && townModel != null;
            }
            if (settingsRegistry == null) {
                return false;
            }
            QuartermasterSettings qm = readQuartermaster(ctx);
            if (qm != null && qm.isQuartermasterEnabled()) {
                return false;
            }
            SwarmRole role = cfg.getRole();
            if (role != SwarmRole.FARMER && role != SwarmRole.BOTH) {
                return false;
            }
            Optional<ITownSnapshot> snap = townModel.snapshot(ctx.getMachineId());
            if (!snap.isPresent() || snap.get().isDead()) {
                return false;
            }
            if (!hasLoot(snap.get())) {
                return false;
            }
            if (townDirectory == null || !nearStorageNpc(ctx)) {
                return false;
            }
            return storageModel != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        try {
            if (ctx == null || cfg == null || swarmBus == null) {
                return BehaviorStatus.SKIPPED;
            }
            Map<String, Object> pd = ctx.getPersistentData();
            String phase = (String) pd.get(QuartermasterBlackboardKeys.KEY_STORAGE_DUMP_PHASE);
            if (phase == null) {
                phase = PHASE_REQUEST;
            }

            switch (phase) {
                case PHASE_REQUEST:
                    return onRequest(ctx);
                case PHASE_WAIT:
                    return onWait(ctx);
                case PHASE_DUMP:
                    return onDump(ctx);
                case PHASE_RELEASE:
                    return onRelease(ctx);
                default:
                    clearDumpKeys(pd);
                    return BehaviorStatus.SKIPPED;
            }
        } catch (RuntimeException ex) {
            ctx.log("WARN", "StorageDumpBehavior: {}", ex.toString());
            return BehaviorStatus.SKIPPED;
        }
    }

    private BehaviorStatus onRequest(IWorkflowContext ctx) {
        Map<String, Object> pd = ctx.getPersistentData();
        String sessionId = ctx.getMachineId() + "_dump_" + UUID.randomUUID();
        pd.put(LogisticsCycleKeys.KEY_STORAGE_DUMP_SESSION_ID, sessionId);
        long now = System.currentTimeMillis();
        String reqId = UUID.randomUUID().toString();
        swarmBus.publish(new SwarmStorageLockRequestEvent(
                ctx.getMachineId(),
                now,
                reqId,
                sessionId,
                ctx.getGroupName()));
        pd.put(QuartermasterBlackboardKeys.KEY_STORAGE_DUMP_PHASE, PHASE_WAIT);
        pd.put(LogisticsCycleKeys.KEY_STORAGE_DUMP_WAIT_AT_MS, Long.valueOf(now));
        ctx.log("INFO", "Storage dump REQUEST session={}", sessionId);
        return BehaviorStatus.EXECUTED;
    }

    private BehaviorStatus onWait(IWorkflowContext ctx) {
        Map<String, Object> pd = ctx.getPersistentData();
        String sessionId = (String) pd.get(LogisticsCycleKeys.KEY_STORAGE_DUMP_SESSION_ID);
        if (sessionId == null || sessionId.trim().isEmpty()) {
            clearDumpKeys(pd);
            return BehaviorStatus.SKIPPED;
        }
        String token = (String) pd.get(QuartermasterBlackboardKeys.KEY_STORAGE_LOCK_TOKEN);
        if (token == null || token.isEmpty()) {
            token = StorageGrantLedger.peekToken(sessionId);
        }
        if (token != null && !token.isEmpty()) {
            pd.put(QuartermasterBlackboardKeys.KEY_STORAGE_LOCK_TOKEN, token);
            pd.put(QuartermasterBlackboardKeys.KEY_STORAGE_DUMP_PHASE, PHASE_DUMP);
            ctx.log("INFO", "Storage dump acquired lock token for {}", sessionId);
            return BehaviorStatus.EXECUTED;
        }

        long waitStart = 0L;
        Object w = pd.get(LogisticsCycleKeys.KEY_STORAGE_DUMP_WAIT_AT_MS);
        if (w instanceof Long) {
            waitStart = ((Long) w).longValue();
        } else if (w instanceof Integer) {
            waitStart = ((Integer) w).longValue();
        }
        long timeoutMs = resolveWaitTimeoutMs(ctx);
        long now = System.currentTimeMillis();
        if (waitStart > 0L && now - waitStart > timeoutMs) {
            ctx.log("WARN", "Storage dump WAIT timeout; resetting session {}", sessionId);
            clearDumpKeys(pd);
            return BehaviorStatus.EXECUTED;
        }
        return BehaviorStatus.EXECUTED;
    }

    private BehaviorStatus onDump(IWorkflowContext ctx) {
        Map<String, Object> pd = ctx.getPersistentData();
        Optional<ITownSnapshot> snap = townModel != null ? townModel.snapshot(ctx.getMachineId()) : Optional.empty();
        if (!snap.isPresent() || snap.get().isDead()) {
            clearDumpKeys(pd);
            return BehaviorStatus.SKIPPED;
        }
        if (!hasLoot(snap.get())) {
            pd.put(QuartermasterBlackboardKeys.KEY_STORAGE_DUMP_PHASE, PHASE_RELEASE);
            ctx.log("INFO", "Storage dump inventory empty — releasing mutex");
            return BehaviorStatus.EXECUTED;
        }

        if (storageModel == null || npcFacade == null || townDirectory == null) {
            return BehaviorStatus.EXECUTED;
        }

        QuartermasterSettings qm = readQuartermaster(ctx);
        long staleMs = qm != null && qm.getLockTtlMs() > 0L ? qm.getLockTtlMs() : 30_000L;

        if (!storageModel.isStorageFresh(ctx.getMachineId(), StorageType.GUILD, staleMs)) {
            // Guild warehouse open + StorageOpenEvent(TYPE_GUILD) path should run here; personal openStorage is insufficient.
            ctx.log("INFO", "Storage dump: guild storage not fresh — open guild vault NPC dialog (placeholder wiring)");
            Optional<VendorRef> vendor = resolveGuildVendor(ctx);
            if (vendor.isPresent()) {
                try {
                    npcFacade.openStorage(ctx, vendor.get()).get(20, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    ctx.log("WARN", "Storage dump openStorage failed: {}", ex.toString());
                }
            }
            return BehaviorStatus.EXECUTED;
        }

        Optional<VendorRef> vendor = resolveGuildVendor(ctx);
        if (!vendor.isPresent()) {
            return BehaviorStatus.EXECUTED;
        }

        Optional<ItemStackSnapshot> stack = firstLootStack(snap.get());
        if (!stack.isPresent()) {
            pd.put(QuartermasterBlackboardKeys.KEY_STORAGE_DUMP_PHASE, PHASE_RELEASE);
            return BehaviorStatus.EXECUTED;
        }

        int slot = stack.get().getSlotIndex();
        int qty = Math.max(1, stack.get().getQuantity());
        try {
            // Inventory -> guild warehouse deposit; same stash pipeline once GUILD window is active.
            npcFacade.stash(ctx, vendor.get(), slot, qty).get(20, TimeUnit.SECONDS);
        } catch (Exception ex) {
            ctx.log("WARN", "Storage dump stash failed: {}", ex.toString());
            return BehaviorStatus.EXECUTED;
        }
        return BehaviorStatus.EXECUTED;
    }

    private BehaviorStatus onRelease(IWorkflowContext ctx) {
        Map<String, Object> pd = ctx.getPersistentData();
        String sessionId = (String) pd.get(LogisticsCycleKeys.KEY_STORAGE_DUMP_SESSION_ID);
        String token = (String) pd.get(QuartermasterBlackboardKeys.KEY_STORAGE_LOCK_TOKEN);
        if (sessionId != null && token != null && !token.isEmpty() && swarmBus != null) {
            long now = System.currentTimeMillis();
            swarmBus.publish(new SwarmStorageReleaseEvent(
                    ctx.getMachineId(),
                    now,
                    UUID.randomUUID().toString(),
                    sessionId,
                    token));
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            StorageGrantLedger.clearSession(sessionId);
        }
        clearDumpKeys(pd);
        ctx.log("INFO", "Storage dump RELEASE complete");
        return BehaviorStatus.SKIPPED;
    }

    private void clearDumpKeys(Map<String, Object> pd) {
        pd.remove(LogisticsCycleKeys.KEY_STORAGE_DUMP_SESSION_ID);
        pd.remove(LogisticsCycleKeys.KEY_STORAGE_DUMP_WAIT_AT_MS);
        pd.remove(QuartermasterBlackboardKeys.KEY_STORAGE_LOCK_TOKEN);
        pd.remove(QuartermasterBlackboardKeys.KEY_STORAGE_DUMP_PHASE);
    }

    private long resolveWaitTimeoutMs(IWorkflowContext ctx) {
        QuartermasterSettings qm = readQuartermaster(ctx);
        if (qm != null && qm.getLockTtlMs() > 0L) {
            return Math.max(qm.getLockTtlMs() * 2L, 10_000L);
        }
        return DEFAULT_WAIT_TIMEOUT_MS;
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

    private static boolean hasLoot(ITownSnapshot snap) {
        return snap.getInventory().getFreeSlots() < snap.getInventory().getTotalSlots();
    }

    private static Optional<ItemStackSnapshot> firstLootStack(ITownSnapshot snap) {
        for (ItemStackSnapshot st : snap.getInventory().listStacks()) {
            if (st != null && st.getItemRefId() > 0 && st.getQuantity() > 0) {
                return Optional.of(st);
            }
        }
        return Optional.empty();
    }

    private boolean nearStorageNpc(IWorkflowContext ctx) {
        try {
            return townDirectory.nearestTown(ctx.getMachineId())
                    .filter(n -> n.getRole() == NpcRole.STORAGE)
                    .isPresent();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Optional<VendorRef> resolveGuildVendor(IWorkflowContext ctx) {
        try {
            return townDirectory.nearestTown(ctx.getMachineId())
                    .filter(n -> n.getRole() == NpcRole.STORAGE)
                    .map(n -> VendorRef.fromNpc(n, "guild_storage"));
        } catch (RuntimeException ex) {
            return Optional.empty();
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
