package org.sokybot.behaviors.combat.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.ICombatSettings;
import org.sokybot.combat.api.ICombatSettingsSnapshotter;
import org.sokybot.combat.api.IWeaponSwapCoordinator;
import org.sokybot.combat.api.WeaponLoadout;
import org.sokybot.combat.api.WeaponSwapException;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;

/**
 * Async weapon swap using {@code 0x7034} inventory moves and {@link InventoryOperationEvent} acks.
 */
@Component(service = IWeaponSwapCoordinator.class, immediate = true)
public final class WeaponSwapCoordinatorImpl implements IWeaponSwapCoordinator {

    private static final Logger log = LoggerFactory.getLogger(WeaponSwapCoordinatorImpl.class);

    /** Equip slot indices (vSRO / RSBot-style). */
    private static final byte EQUIP_MAIN_HAND = 6;
    private static final byte EQUIP_OFF_HAND = 7;

    private final ConcurrentHashMap<String, WeaponLoadout> loadoutByMachine = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor timeouts = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, "weapon-swap-timeout");
        t.setDaemon(true);
        return t;
    });

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Reference
    private ICombatSettingsSnapshotter combatSettingsSnapshotter;

    @Activate
    void activate() {
    }

    @Deactivate
    void deactivate() {
        timeouts.shutdownNow();
        loadoutByMachine.clear();
    }

    @Override
    public WeaponLoadout currentLoadout(String machineFullName) {
        WeaponLoadout l = loadoutByMachine.get(machineFullName);
        return l != null ? l : WeaponLoadout.UNKNOWN;
    }

    @Override
    public CompletableFuture<Void> swapTo(String machineFullName, WeaponLoadout target) {
        return CompletableFuture.failedFuture(new WeaponSwapException(
                "Weapon swap requires IWorkflowContext; use swapTo(IWorkflowContext, WeaponLoadout)"));
    }

    @Override
    public CompletableFuture<Void> swapTo(IWorkflowContext ctx, WeaponLoadout target) {
        if (ctx == null || target == null) {
            return CompletableFuture.failedFuture(new WeaponSwapException("null context or loadout"));
        }
        if (target == WeaponLoadout.UNKNOWN) {
            return CompletableFuture.completedFuture(null);
        }
        ICombatSettings raw = combatSettingsSnapshotter.snapshot(ctx, false);
        if (!(raw instanceof CombatSettings)) {
            return CompletableFuture.failedFuture(new WeaponSwapException("unexpected settings type"));
        }
        CombatSettings settings = (CombatSettings) raw;
        int timeoutMs = Math.max(500, settings.getWeaponSwapTimeoutMs());

        int primaryInv;
        int secondaryInv;
        switch (target) {
            case MAIN_DAMAGE:
                primaryInv = settings.getMainDamagePrimaryInventorySlot();
                secondaryInv = settings.getMainDamageSecondaryInventorySlot();
                break;
            case BUFF_CASTER:
                primaryInv = settings.getBuffCasterPrimaryInventorySlot();
                secondaryInv = settings.getBuffCasterSecondaryInventorySlot();
                break;
            default:
                return CompletableFuture.completedFuture(null);
        }

        boolean primaryOk = primaryInv >= 0 && primaryInv <= 255;
        boolean secondaryOk = secondaryInv >= 0 && secondaryInv <= 255;
        boolean anyMove = primaryOk || secondaryOk;
        if (!anyMove) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        if (primaryOk) {
            byte src = (byte) primaryInv;
            chain = chain.thenCompose(v -> sendMoveAndAwait(ctx, src, EQUIP_MAIN_HAND, (short) 1, timeoutMs));
        }
        if (secondaryOk) {
            byte src = (byte) secondaryInv;
            chain = chain.thenCompose(v -> sendMoveAndAwait(ctx, src, EQUIP_OFF_HAND, (short) 1, timeoutMs));
        }

        final WeaponLoadout resolvedTarget = target;
        return chain.whenComplete((ok, err) -> {
            if (err == null) {
                loadoutByMachine.put(ctx.getMachineId(), resolvedTarget);
            } else {
                log.debug("Weapon swap failed for {}: {}", ctx.getMachineId(), err.toString());
            }
        });
    }

    private CompletableFuture<Void> sendMoveAndAwait(IWorkflowContext ctx, byte sourceSlot, byte destSlot,
            short quantity, long timeoutMs) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        CombatPackets.sendInventoryMoveSlot(ctx, sourceSlot, destSlot, quantity);

        Disposable sub = reactiveEventBus.on(InventoryOperationEvent.class)
                .filter(e -> ctx.getMachineId().equals(e.getFullName()))
                .filter(e -> e.isSuccess() && e.getOperationType() == InventoryOperationEvent.OP_MOVE_SLOTS)
                .filter(e -> slotsMatch(e, sourceSlot, destSlot))
                .take(1)
                .subscribe(e -> cf.complete(null),
                        err -> cf.completeExceptionally(err != null ? err
                                : new WeaponSwapException("inventory-op-subscription-error")));

        attachTimeout(cf, sub, timeoutMs);
        return cf;
    }

    private static boolean slotsMatch(InventoryOperationEvent e, byte sourceSlot, byte destSlot) {
        Byte s = e.getSourceSlot();
        Byte d = e.getDestSlot();
        return s != null && d != null && s.byteValue() == sourceSlot && d.byteValue() == destSlot;
    }

    private void attachTimeout(CompletableFuture<Void> cf, Disposable subscription, long timeoutMs) {
        AtomicReference<ScheduledFuture<?>> scheduled = new AtomicReference<>();
        ScheduledFuture<?> sf = timeouts.schedule(() -> {
            if (!cf.isDone()) {
                subscription.dispose();
                cf.completeExceptionally(new WeaponSwapException("inventory-move-timeout"));
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
}
