package org.sokybot.behaviors.town.internal;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.events.chat.NpcTalkEvent;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.sokybot.gameevents.events.inventory.ItemDurabilityUpdateEvent;
import org.sokybot.gameevents.events.storage.StorageOpenEvent;
import org.sokybot.gameevents.events.teleport.TeleportCompleteEvent;
import org.sokybot.gameevents.events.teleport.TeleportResponseEvent;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.town.api.INpcInteractionFacade;
import org.sokybot.town.api.NpcInteractionResult;
import org.sokybot.town.api.NpcRef;
import org.sokybot.town.api.NpcRole;
import org.sokybot.town.api.VendorRef;
import org.sokybot.town.api.EquipSlot;

import reactor.core.Disposable;

/**
 * Sends client packets via {@link IWorkflowContext#getDispatcher()}, then completes {@link CompletableFuture}s when the
 * reactive bus observes matching server-driven events (inventory op ack, NPC talk dialog, durability updates).
 */
@Component(service = INpcInteractionFacade.class, immediate = true)
public final class NpcInteractionFacadeImpl implements INpcInteractionFacade {

    private static final long ACK_TIMEOUT_MS = 25_000L;
    private static final long TELEPORT_TALK_TIMEOUT_MS = 3_000L;
    private static final long TELEPORT_RESPONSE_TIMEOUT_MS = 5_000L;
    private static final long TELEPORT_COMPLETE_TIMEOUT_MS = 30_000L;

    private final ScheduledThreadPoolExecutor timeouts = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, "town-npc-facade-timeout");
        t.setDaemon(true);
        return t;
    });

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
    }

    @Deactivate
    void deactivate() {
        timeouts.shutdownNow();
    }

    @Override
    public CompletableFuture<NpcInteractionResult> openDialog(IWorkflowContext ctx, NpcRef npc) {
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        if (!npc.getEntityUniqueId().isPresent()) {
            cf.complete(NpcInteractionResult.failure("missing-npc-entity-id"));
            return cf;
        }
        int npcUid = npc.getEntityUniqueId().get().intValue();
        TownPackets.sendNpcSelect(ctx, npc);
        TownPackets.sendNpcInteract(ctx, npcUid, NpcTalkEvent.OPTION_TALK);
        Disposable sub = reactiveEventBus.on(NpcTalkEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()) && e.getNpcUniqueId() == npcUid)
                .take(1)
                .subscribe(e -> cf.complete(NpcInteractionResult.ok()),
                        err -> cf.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeout(cf, sub, () -> NpcInteractionResult.failure("talk-ack-timeout"));
        return cf;
    }

    @Override
    public CompletableFuture<NpcInteractionResult> buy(IWorkflowContext ctx, VendorRef vendor, int itemRefId,
            int quantity) {
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        int npcUid = vendor.getEntityUniqueId().orElse(0);
        if (npcUid <= 0) {
            cf.complete(NpcInteractionResult.failure("missing-vendor-entity-id"));
            return cf;
        }
        TownPackets.sendBuy(ctx, (byte) 0x00, npcUid, itemRefId, (short) Math.min(ushortMax(), quantity));
        Disposable sub = reactiveEventBus.on(InventoryOperationEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()) && e.isSuccess() && e.isBuy())
                .take(1)
                .subscribe(e -> cf.complete(NpcInteractionResult.ok()),
                        err -> cf.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeout(cf, sub, () -> NpcInteractionResult.failure("buy-ack-timeout"));
        return cf;
    }

    @Override
    public CompletableFuture<NpcInteractionResult> sell(IWorkflowContext ctx, VendorRef vendor, int inventorySlotIndex,
            int quantity) {
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        TownPackets.sendSell(ctx, (byte) inventorySlotIndex, (short) Math.min(ushortMax(), quantity));
        Disposable sub = reactiveEventBus.on(InventoryOperationEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()) && e.isSuccess() && e.isSell())
                .take(1)
                .subscribe(e -> cf.complete(NpcInteractionResult.ok()),
                        err -> cf.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeout(cf, sub, () -> NpcInteractionResult.failure("sell-ack-timeout"));
        return cf;
    }

    @Override
    public CompletableFuture<NpcInteractionResult> repair(IWorkflowContext ctx, VendorRef blacksmith,
            EquipSlot slot) {
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        TownPackets.sendRepair(ctx, TownPackets.equipSlotByte(slot));
        Disposable sub = reactiveEventBus.on(ItemDurabilityUpdateEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()))
                .take(1)
                .subscribe(e -> cf.complete(NpcInteractionResult.ok()),
                        err -> cf.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeout(cf, sub, () -> NpcInteractionResult.failure("repair-ack-timeout"));
        return cf;
    }

    @Override
    public CompletableFuture<NpcInteractionResult> stash(IWorkflowContext ctx, VendorRef storageNpc,
            int inventorySlotIndex, int quantity) {
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        TownPackets.sendDepositItem(ctx, inventorySlotIndex, quantity);
        Disposable sub = reactiveEventBus.on(InventoryOperationEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()) && e.isSuccess()
                        && e.getOperationType() == InventoryOperationEvent.OP_DEPOSIT_ITEM)
                .take(1)
                .subscribe(e -> cf.complete(NpcInteractionResult.ok()),
                        err -> cf.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeout(cf, sub, () -> NpcInteractionResult.failure("stash-ack-timeout"));
        return cf;
    }

    @Override
    public CompletableFuture<NpcInteractionResult> openStorage(IWorkflowContext ctx, VendorRef storageNpc) {
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        if (!storageNpc.getEntityUniqueId().isPresent()) {
            cf.complete(NpcInteractionResult.failure("missing-storage-npc-entity-id"));
            return cf;
        }
        int npcUid = storageNpc.getEntityUniqueId().get().intValue();
        TownPackets.sendNpcSelect(ctx, storageNpc.toNpc());
        TownPackets.sendNpcInteract(ctx, npcUid, NpcTalkEvent.OPTION_TRADE);
        Disposable sub = reactiveEventBus.on(StorageOpenEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()) && e.isPersonal())
                .take(1)
                .subscribe(e -> cf.complete(NpcInteractionResult.ok()),
                        err -> cf.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeout(cf, sub, () -> NpcInteractionResult.failure("storage-open-timeout"));
        return cf;
    }

    @Override
    public CompletableFuture<NpcInteractionResult> withdraw(IWorkflowContext ctx, VendorRef storageNpc,
            int storageSlotIndex, int quantity) {
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        TownPackets.sendWithdrawItem(ctx, storageSlotIndex, quantity);
        Disposable sub = reactiveEventBus.on(InventoryOperationEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()) && e.isSuccess()
                        && e.getOperationType() == InventoryOperationEvent.OP_WITHDRAW_ITEM)
                .take(1)
                .subscribe(e -> cf.complete(NpcInteractionResult.ok()),
                        err -> cf.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeout(cf, sub, () -> NpcInteractionResult.failure("withdraw-ack-timeout"));
        return cf;
    }

    @Override
    public CompletableFuture<NpcInteractionResult> teleport(IWorkflowContext ctx, int npcRefId, int destinationRefId) {
        Optional<NpcRef> npcOpt = resolveNpcRef(ctx, npcRefId);
        if (!npcOpt.isPresent()) {
            return CompletableFuture.completedFuture(NpcInteractionResult.failure("npc-not-found"));
        }
        NpcRef npc = npcOpt.get();
        if (!npc.getEntityUniqueId().isPresent()) {
            return CompletableFuture.completedFuture(NpcInteractionResult.failure("missing-npc-entity-id"));
        }
        int npcUid = npc.getEntityUniqueId().get().intValue();
        TownPackets.sendNpcSelect(ctx, npc);
        TownPackets.sendNpcInteract(ctx, npcUid, NpcTalkEvent.OPTION_TALK);
        CompletableFuture<NpcInteractionResult> talkAck = new CompletableFuture<>();
        Disposable talkSub = reactiveEventBus.on(NpcTalkEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()) && e.getNpcUniqueId() == npcUid)
                .take(1)
                .subscribe(e -> talkAck.complete(NpcInteractionResult.ok()),
                        err -> talkAck.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeoutMs(talkAck, talkSub, TELEPORT_TALK_TIMEOUT_MS,
                () -> NpcInteractionResult.failure("teleport-talk-timeout"));
        return talkAck.thenCompose(r -> {
            if (!r.isSuccess()) {
                return CompletableFuture.completedFuture(r);
            }
            TownPackets.sendTeleportUse(ctx, destinationRefId);
            CompletableFuture<NpcInteractionResult> respAck = new CompletableFuture<>();
            Disposable respSub = reactiveEventBus.on(TeleportResponseEvent.class)
                    .filter(e -> ctx.getMachineId().equals(e.getFullName()) && e.isSuccess()
                            && e.getDestinationId() == destinationRefId)
                    .take(1)
                    .subscribe(e -> respAck.complete(NpcInteractionResult.ok()),
                            err -> respAck.complete(NpcInteractionResult.failure(err.getMessage())));
            attachTimeoutMs(respAck, respSub, TELEPORT_RESPONSE_TIMEOUT_MS,
                    () -> NpcInteractionResult.failure("teleport-response-timeout"));
            return respAck.thenCompose(r2 -> {
                if (!r2.isSuccess()) {
                    return CompletableFuture.completedFuture(r2);
                }
                CompletableFuture<NpcInteractionResult> done = new CompletableFuture<>();
                Disposable doneSub = reactiveEventBus.on(TeleportCompleteEvent.class)
                        .filter(e -> ctx.getMachineId().equals(e.getFullName()))
                        .take(1)
                        .subscribe(e -> done.complete(NpcInteractionResult.ok()),
                                err -> done.complete(NpcInteractionResult.failure(err.getMessage())));
                attachTimeoutMs(done, doneSub, TELEPORT_COMPLETE_TIMEOUT_MS,
                        () -> NpcInteractionResult.failure("teleport-complete-timeout"));
                return done;
            });
        });
    }

    private static Optional<NpcRef> resolveNpcRef(IWorkflowContext ctx, int npcRefId) {
        if (ctx.getGameModel() == null) {
            return Optional.empty();
        }
        List<ISpawn> spawns = ctx.getGameModel().snapshotAll(ISpawn.class);
        for (ISpawn s : spawns) {
            if (s.getRefId() == npcRefId) {
                float wx = s.getX();
                float wy = s.getY();
                float wz = s.getZOffset();
                GamePosition p = s.getPosition();
                if (p != null) {
                    wx = p.getX();
                    wy = p.getY();
                    wz = p.getZ();
                }
                return Optional.of(NpcRef.builder()
                        .entityUniqueId(s.getUniqueId())
                        .catalogueNpcRefId(npcRefId)
                        .displayName(s.getName() != null ? s.getName() : "")
                        .role(NpcRole.UNKNOWN)
                        .worldPosition(wx, wy, wz)
                        .regionId(0)
                        .build());
            }
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<NpcInteractionResult> invokeService(IWorkflowContext ctx, NpcRef npc,
            String serviceId) {
        ctx.log("DEBUG", "Npc invokeService id={}", serviceId != null ? serviceId : "");
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        if (!npc.getEntityUniqueId().isPresent()) {
            cf.complete(NpcInteractionResult.failure("missing-npc-entity-id"));
            return cf;
        }
        int uid = npc.getEntityUniqueId().get().intValue();
        TownPackets.sendNpcInteract(ctx, uid, NpcTalkEvent.OPTION_TRADE);
        Disposable sub = reactiveEventBus.on(NpcTalkEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()))
                .take(1)
                .subscribe(e -> cf.complete(NpcInteractionResult.ok()),
                        err -> cf.complete(NpcInteractionResult.failure(err.getMessage())));
        attachTimeout(cf, sub, () -> NpcInteractionResult.failure("service-ack-timeout"));
        return cf;
    }

    private void attachTimeout(CompletableFuture<NpcInteractionResult> cf, Disposable subscription,
            java.util.function.Supplier<NpcInteractionResult> timeoutResult) {
        attachTimeoutMs(cf, subscription, ACK_TIMEOUT_MS, timeoutResult);
    }

    private void attachTimeoutMs(CompletableFuture<NpcInteractionResult> cf, Disposable subscription, long timeoutMs,
            java.util.function.Supplier<NpcInteractionResult> timeoutResult) {
        AtomicReference<ScheduledFuture<?>> scheduled = new AtomicReference<>();
        ScheduledFuture<?> sf = timeouts.schedule(() -> {
            if (!cf.isDone()) {
                subscription.dispose();
                cf.complete(timeoutResult.get());
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
        scheduled.set(sf);
        cf.whenComplete((r, t) -> {
            subscription.dispose();
            ScheduledFuture<?> s = scheduled.get();
            if (s != null) {
                s.cancel(false);
            }
        });
    }

    private static int ushortMax() {
        return 0xffff;
    }
}
