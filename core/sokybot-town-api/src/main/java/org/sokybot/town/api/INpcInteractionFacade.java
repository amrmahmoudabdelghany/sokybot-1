package org.sokybot.town.api;

import java.util.concurrent.CompletableFuture;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Thin async façade over NPC dialogs, vendors, repair, and stash interactions.
 * Implementations translate workflow intent into packets and complete futures after server acks.
 */
public interface INpcInteractionFacade {

    CompletableFuture<NpcInteractionResult> openDialog(IWorkflowContext ctx, NpcRef npc);

    CompletableFuture<NpcInteractionResult> buy(IWorkflowContext ctx, VendorRef vendor, int itemRefId, int quantity);

    CompletableFuture<NpcInteractionResult> sell(IWorkflowContext ctx, VendorRef vendor, int inventorySlotIndex,
            int quantity);

    CompletableFuture<NpcInteractionResult> repair(IWorkflowContext ctx, VendorRef blacksmith, EquipSlot slot);

    CompletableFuture<NpcInteractionResult> stash(IWorkflowContext ctx, VendorRef storageNpc, int inventorySlotIndex,
            int quantity);

    default CompletableFuture<NpcInteractionResult> withdraw(IWorkflowContext ctx, VendorRef storageNpc,
            int storageSlotIndex, int quantity) {
        CompletableFuture<NpcInteractionResult> cf = new CompletableFuture<>();
        cf.complete(NpcInteractionResult.failure("withdraw-not-supported"));
        return cf;
    }

    /** Opens personal storage UI (fallback: plain NPC dialog). */
    default CompletableFuture<NpcInteractionResult> openStorage(IWorkflowContext ctx, VendorRef storageNpc) {
        return openDialog(ctx, storageNpc.toNpc());
    }

    /**
     * Escape hatch for scripted sequences (guild skills, teleport menus, stable routes, …).
     *
     * @param serviceId stable id agreed between catalogue data and translator bundle
     */
    CompletableFuture<NpcInteractionResult> invokeService(IWorkflowContext ctx, NpcRef npc, String serviceId);
}
