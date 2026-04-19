package org.sokybot.storage.projections.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.sokybot.gameevents.events.storage.StorageBoxFinalizeEvent;
import org.sokybot.gameevents.events.storage.StorageBoxTakeItemEvent;
import org.sokybot.gameevents.events.storage.StorageItemUpdateEvent;
import org.sokybot.gameevents.events.storage.StorageOpenEvent;
import org.sokybot.storage.api.IGuildStorageSnapshot;
import org.sokybot.storage.api.IStorageModel;
import org.sokybot.storage.api.IStorageSnapshot;
import org.sokybot.storage.api.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Warehouse overlay sourced from {@link IReactiveEventBus}; keyed by machine full name.
 */
@Component(service = IStorageModel.class, immediate = true)
public final class StorageModelComponent implements IStorageModel {

    private static final Logger log = LoggerFactory.getLogger(StorageModelComponent.class);

    private final Map<String, MachineStorageState> stateByMachine = new ConcurrentHashMap<>();

    private final List<Disposable> subscriptions = new ArrayList<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
        subscriptions.add(reactiveEventBus.on(StorageOpenEvent.class).subscribe(this::onStorageOpen));
        subscriptions.add(reactiveEventBus.on(StorageItemUpdateEvent.class).subscribe(this::onStorageItemUpdate));
        subscriptions.add(reactiveEventBus.on(StorageBoxFinalizeEvent.class).subscribe(this::onStorageFinalize));
        subscriptions.add(reactiveEventBus.on(StorageBoxTakeItemEvent.class).subscribe(this::onStorageTake));
        subscriptions.add(reactiveEventBus.on(InventoryOperationEvent.class).subscribe(this::onInventoryOp));
        log.debug("IStorageModel projection active");
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        for (MachineStorageState st : stateByMachine.values()) {
            st.personalSink.tryEmitComplete();
            st.guildSink.tryEmitComplete();
        }
        stateByMachine.clear();
        log.debug("IStorageModel projection deactivated");
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private MachineStorageState stateFor(String fullNameKey) {
        return stateByMachine.computeIfAbsent(fullNameKey, fn -> new MachineStorageState());
    }

    private void publishSnapshot(String fullNameKey, MachineStorageState st, StorageType type) {
        if (fullNameKey == null || st == null || type == null) {
            return;
        }
        long now = System.currentTimeMillis();
        st.capturedAtEpochMs = now;
        if (type == StorageType.PERSONAL) {
            st.personalSink.emitNext(StorageSnapshots.buildPersonal(fullNameKey, st), Sinks.EmitFailureHandler.FAIL_FAST);
        } else if (type == StorageType.GUILD) {
            st.guildSink.emitNext(StorageSnapshots.buildGuild(fullNameKey, st), Sinks.EmitFailureHandler.FAIL_FAST);
        }
    }

    private void onStorageOpen(StorageOpenEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineStorageState st = stateFor(key);
        long ts = e.getTimestamp();
        if (e.isPersonal()) {
            st.personalStacks.clear();
            st.personalGold = e.getStorageGold();
            st.lastPersonalOpenEpochMs = ts;
            st.personalFresh = false;
            st.personalFilledSlots = -1;
        } else if (e.isGuild()) {
            st.guildStacks.clear();
            st.guildGold = e.getStorageGold();
            st.lastGuildOpenEpochMs = ts;
            st.guildFresh = false;
            st.guildFilledSlots = -1;
        }
        publishSnapshot(key, st, e.isGuild() ? StorageType.GUILD : StorageType.PERSONAL);
    }

    private void onStorageItemUpdate(StorageItemUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineStorageState st = stateFor(key);
        byte rawType = e.getStorageType();
        StorageType type = storageTypeFromOpenByte(rawType);
        if (type == null) {
            st.personalFresh = false;
            st.guildFresh = false;
            publishSnapshot(key, st, StorageType.PERSONAL);
            publishSnapshot(key, st, StorageType.GUILD);
            return;
        }
        ConcurrentMap<Integer, StorageStackMutable> map =
                type == StorageType.PERSONAL ? st.personalStacks : st.guildStacks;
        int slot = e.getSlotIndex();
        int qty = Math.max(0, e.getQuantity());
        int ref = e.getItemRefId();
        long ts = e.getTimestamp();
        int dur = e.getDurabilityPercent();
        if (qty <= 0 || ref == 0) {
            map.remove(slot);
        } else {
            map.put(slot, new StorageStackMutable(slot, ref, qty, dur, ts));
        }
        publishSnapshot(key, st, type);
    }

    private void onStorageFinalize(StorageBoxFinalizeEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineStorageState st = stateFor(key);
        byte rawType = e.getStorageType();
        StorageType type = storageTypeFromOpenByte(rawType);
        if (type == null) {
            return;
        }
        int total = e.getTotalSlots();
        int filled = e.getFilledSlots();
        if (type == StorageType.PERSONAL) {
            if (total >= 0) {
                st.personalTotalSlots = total;
            }
            if (filled >= 0) {
                st.personalFilledSlots = filled;
            }
            st.personalFresh = true;
        } else {
            if (total >= 0) {
                st.guildTotalSlots = total;
            }
            if (filled >= 0) {
                st.guildFilledSlots = filled;
            }
            st.guildFresh = true;
        }
        publishSnapshot(key, st, type);
    }

    private void onStorageTake(StorageBoxTakeItemEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineStorageState st = stateFor(key);
        if (!e.isSuccess()) {
            st.personalFresh = false;
            st.guildFresh = false;
            publishSnapshot(key, st, StorageType.PERSONAL);
            publishSnapshot(key, st, StorageType.GUILD);
        }
    }

    private void onInventoryOp(InventoryOperationEvent e) {
        String key = normalize(e.getFullName());
        if (key == null || !e.isSuccess()) {
            return;
        }
        MachineStorageState st = stateFor(key);
        byte op = e.getOperationType();
        boolean partial = false;
        switch (op) {
            case InventoryOperationEvent.OP_DEPOSIT_ITEM:
                partial = applyDeposit(st.personalStacks, e);
                break;
            case InventoryOperationEvent.OP_WITHDRAW_ITEM:
                partial = applyWithdraw(st.personalStacks, e);
                break;
            case InventoryOperationEvent.OP_GUILD_DEPOSIT:
                partial = applyDeposit(st.guildStacks, e);
                break;
            case InventoryOperationEvent.OP_GUILD_WITHDRAW:
                partial = applyWithdraw(st.guildStacks, e);
                break;
            default:
                return;
        }
        if (partial) {
            st.personalFresh = false;
            st.guildFresh = false;
            publishSnapshot(key, st, StorageType.PERSONAL);
            publishSnapshot(key, st, StorageType.GUILD);
            return;
        }
        publishSnapshot(key, st,
                op == InventoryOperationEvent.OP_GUILD_DEPOSIT || op == InventoryOperationEvent.OP_GUILD_WITHDRAW
                        ? StorageType.GUILD
                        : StorageType.PERSONAL);
    }

    /**
     * @return {@code true} when event lacked concrete slot data (caller should mark stale).
     */
    private static boolean applyDeposit(ConcurrentMap<Integer, StorageStackMutable> stacks,
            InventoryOperationEvent e) {
        Byte dest = e.getDestSlot();
        Integer amount = e.getAmount();
        Integer itemId = e.getItemId();
        if (dest == null || amount == null || itemId == null) {
            return true;
        }
        int slot = dest.intValue() & 0xFF;
        int qty = amount.intValue();
        int ref = itemId.intValue();
        if (slot < 0 || qty <= 0 || ref == 0) {
            return true;
        }
        long ts = e.getTimestamp();
        stacks.put(slot, new StorageStackMutable(slot, ref, qty, -1, ts));
        return false;
    }

    private static boolean applyWithdraw(ConcurrentMap<Integer, StorageStackMutable> stacks,
            InventoryOperationEvent e) {
        Byte src = e.getSourceSlot();
        Integer amount = e.getAmount();
        if (src == null || amount == null) {
            return true;
        }
        int slot = src.intValue() & 0xFF;
        int dec = amount.intValue();
        StorageStackMutable cur = stacks.get(slot);
        if (cur == null) {
            return true;
        }
        int next = cur.quantity - dec;
        if (next <= 0) {
            stacks.remove(slot);
        } else {
            cur.quantity = next;
            cur.lastUpdateEpochMs = e.getTimestamp();
        }
        return false;
    }

    private static StorageType storageTypeFromOpenByte(byte storageType) {
        if (storageType == StorageOpenEvent.TYPE_PERSONAL) {
            return StorageType.PERSONAL;
        }
        if (storageType == StorageOpenEvent.TYPE_GUILD) {
            return StorageType.GUILD;
        }
        return null;
    }

    @Override
    public Optional<IStorageSnapshot> personal(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        MachineStorageState st = stateByMachine.get(key);
        if (st == null) {
            return Optional.empty();
        }
        return Optional.of(StorageSnapshots.buildPersonal(key, st));
    }

    @Override
    public Optional<IGuildStorageSnapshot> guild(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        MachineStorageState st = stateByMachine.get(key);
        if (st == null) {
            return Optional.empty();
        }
        return Optional.of(StorageSnapshots.buildGuild(key, st));
    }

    @Override
    public Flux<IStorageSnapshot> observePersonal(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Flux.empty();
        }
        MachineStorageState st = stateFor(key);
        Flux<IStorageSnapshot> tail = st.personalSink.asFlux().sample(Duration.ofMillis(50));
        Optional<IStorageSnapshot> seed = personal(machineFullName);
        if (seed.isPresent()) {
            return Flux.concat(Flux.just(seed.get()), tail);
        }
        return tail;
    }

    @Override
    public Flux<IGuildStorageSnapshot> observeGuild(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Flux.empty();
        }
        MachineStorageState st = stateFor(key);
        Flux<IGuildStorageSnapshot> tail = st.guildSink.asFlux().sample(Duration.ofMillis(50));
        Optional<IGuildStorageSnapshot> seed = guild(machineFullName);
        if (seed.isPresent()) {
            return Flux.concat(Flux.just(seed.get()), tail);
        }
        return tail;
    }

    @Override
    public boolean isStorageFresh(String machineFullName, StorageType type, long maxAgeMs) {
        String key = normalize(machineFullName);
        if (key == null || type == null || maxAgeMs < 0L) {
            return false;
        }
        MachineStorageState st = stateByMachine.get(key);
        if (st == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (type == StorageType.PERSONAL) {
            return st.personalFresh && now - st.lastPersonalOpenEpochMs <= maxAgeMs;
        }
        return st.guildFresh && now - st.lastGuildOpenEpochMs <= maxAgeMs;
    }
}
