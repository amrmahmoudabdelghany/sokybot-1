package org.sokybot.town.projections.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.character.CharacterDeathEvent;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.chat.NpcTalkEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.gameevents.events.inventory.InventoryItemUpdateEvent;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.sokybot.gameevents.events.inventory.ItemDurabilityUpdateEvent;
import org.sokybot.gameevents.events.inventory.ItemUseEvent;
import org.sokybot.gameevents.events.spawn.PlayerSpawnEvent;
import org.sokybot.gameevents.events.stat.LifeStateUpdateEvent;
import org.sokybot.gameevents.events.storage.StorageOpenEvent;
import org.sokybot.town.api.EquipSlot;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Tactical town overlay sourced from {@link IReactiveEventBus}; keyed by machine full name.
 */
@Component(service = ITownModel.class, immediate = true)
public final class TownModelComponent implements ITownModel {

    private static final byte LIFE_DEAD = LifeStateUpdateEvent.LIFE_DEAD;

    private final Map<String, MachineTownState> stateByMachine = new ConcurrentHashMap<>();
    private final List<Disposable> subscriptions = new ArrayList<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
        subscriptions.add(reactiveEventBus.on(CharacterLoadedEvent.class).subscribe(this::onCharacterLoaded));
        subscriptions.add(reactiveEventBus.on(CharacterDeathEvent.class).subscribe(this::onCharacterDeath));
        subscriptions.add(reactiveEventBus.on(LifeStateUpdateEvent.class).subscribe(this::onLifeState));
        subscriptions.add(reactiveEventBus.on(InventoryItemUpdateEvent.class).subscribe(this::onInventoryItemUpdate));
        subscriptions.add(reactiveEventBus.on(InventoryOperationEvent.class).subscribe(this::onInventoryOperation));
        subscriptions.add(reactiveEventBus.on(ItemUseEvent.class).subscribe(this::onItemUse));
        subscriptions.add(reactiveEventBus.on(ItemDurabilityUpdateEvent.class).subscribe(this::onDurabilityUpdate));
        subscriptions.add(reactiveEventBus.on(StorageOpenEvent.class).subscribe(this::onStorageOpen));
        subscriptions.add(reactiveEventBus.on(NpcTalkEvent.class).subscribe(this::onNpcTalk));
        subscriptions.add(reactiveEventBus.on(EntityMovementEvent.class).subscribe(this::onEntityMovement));
        subscriptions.add(reactiveEventBus.on(PlayerSpawnEvent.class).subscribe(this::onPlayerSpawn));
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        stateByMachine.clear();
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private MachineTownState stateFor(String fullNameKey) {
        return stateByMachine.computeIfAbsent(fullNameKey, fn -> new MachineTownState());
    }

    private void onCharacterLoaded(CharacterLoadedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        st.selfEntityUniqueId = e.getUniqueId();
        st.gold = e.getGold();
        st.dead = e.getLifeState() == LIFE_DEAD;
        st.killerUniqueId = st.dead ? st.killerUniqueId : null;
        float[] pos = TownPositions.xyzFromCharacterLoaded(e);
        if (pos != null) {
            st.selfX = pos[0];
            st.selfY = pos[1];
            st.selfZ = pos[2];
        }
        st.regionPack = packRegion(e.getXSector(), e.getYSector());
        st.touchEpoch();
    }

    private void onCharacterDeath(CharacterDeathEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        st.dead = true;
        st.killerUniqueId = e.getKillerId();
        st.touchEpoch();
    }

    private void onLifeState(LifeStateUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        Integer selfId = st.selfEntityUniqueId;
        if (selfId == null || selfId.intValue() != e.getUniqueId()) {
            return;
        }
        if (!e.isLifeStateUpdate()) {
            return;
        }
        st.dead = e.getStateValue() == LIFE_DEAD;
        if (!st.dead) {
            st.killerUniqueId = null;
        }
        st.touchEpoch();
    }

    private void onInventoryItemUpdate(InventoryItemUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        int slot = Byte.toUnsignedInt(e.getSlot());
        Integer itemId = e.getItemId();
        Integer quantity = e.getQuantity();
        Integer durability = e.getDurability();

        if (itemId != null && itemId.intValue() == 0 && (quantity == null || quantity.intValue() == 0)) {
            st.inventorySlots.remove(slot);
        } else {
            MachineTownState.InvSlot cur = st.inventorySlots.get(slot);
            int ref = itemId != null ? itemId.intValue() : cur != null ? cur.itemRefId : 0;
            int qty = quantity != null ? quantity.intValue() : cur != null ? cur.quantity : 0;
            int durPct = cur != null ? cur.durabilityPercent : -1;
            if (durability != null && (e.getUpdateFlags() & InventoryItemUpdateEvent.FLAG_DURABILITY) != 0) {
                durPct = clampPercent(durability.longValue());
            }
            if (qty <= 0 || ref == 0) {
                st.inventorySlots.remove(slot);
            } else {
                st.inventorySlots.put(slot, new MachineTownState.InvSlot(ref, qty, durPct));
            }
        }

        Optional<EquipSlot> mapped = EquipmentSlotMappings.fromRawSlot(slot);
        if (mapped.isPresent() && durability != null
                && (e.getUpdateFlags() & InventoryItemUpdateEvent.FLAG_DURABILITY) != 0) {
            st.equipDurabilityPercent.put(mapped.get(), clampPercent(durability.longValue()));
        }

        st.npcDialogNpcUniqueId = 0;
        st.touchEpoch();
    }

    private void onInventoryOperation(InventoryOperationEvent e) {
        String key = normalize(e.getFullName());
        if (key == null || !e.isSuccess()) {
            return;
        }
        MachineTownState st = stateFor(key);
        Long goldAmount = e.getGoldAmount();
        if (goldAmount != null) {
            st.gold = goldAmount.longValue();
        }
        if (e.isBuy() || e.isSell() || e.isPickup() || e.isDrop()) {
            st.npcDialogNpcUniqueId = 0;
        }
        if (e.getOperationType() == InventoryOperationEvent.OP_DEPOSIT_ITEM
                || e.getOperationType() == InventoryOperationEvent.OP_WITHDRAW_ITEM
                || e.getOperationType() == InventoryOperationEvent.OP_DEPOSIT_GOLD
                || e.getOperationType() == InventoryOperationEvent.OP_WITHDRAW_GOLD) {
            st.storageSessionOpen = true;
        }
        st.touchEpoch();
    }

    private void onItemUse(ItemUseEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        MachineTownState.InvSlot cur = st.inventorySlots.get(e.getSourceSlot());
        if (cur != null) {
            cur.quantity = Math.max(0, e.getNewAmount());
            if (cur.quantity == 0) {
                st.inventorySlots.remove(e.getSourceSlot());
            }
        }
        st.touchEpoch();
    }

    private void onDurabilityUpdate(ItemDurabilityUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        Optional<EquipSlot> mapped = EquipmentSlotMappings.fromRawSlot(e.getSlot());
        if (mapped.isPresent()) {
            int pct = normalizeDurabilityMetric(e.getDurability());
            st.equipDurabilityPercent.put(mapped.get(), pct);
        }
        st.touchEpoch();
    }

    private void onStorageOpen(StorageOpenEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        st.storageSessionOpen = true;
        st.touchEpoch();
    }

    private void onNpcTalk(NpcTalkEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        st.npcDialogNpcUniqueId = e.getNpcUniqueId();
        st.touchEpoch();
    }

    private void onEntityMovement(EntityMovementEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        Integer selfId = st.selfEntityUniqueId;
        if (selfId == null || e.getEntityId() != selfId.intValue()) {
            return;
        }
        float[] pos = TownPositions.xyz(e.getCurrentPosition());
        if (pos != null) {
            st.selfX = pos[0];
            st.selfY = pos[1];
            st.selfZ = pos[2];
        }
        st.touchEpoch();
    }

    private void onPlayerSpawn(PlayerSpawnEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        MachineTownState st = stateFor(key);
        Integer selfId = st.selfEntityUniqueId;
        if (e.getPlayer() == null || (selfId != null && selfId.intValue() != e.getPlayer().getUniqueId())) {
            return;
        }
        float[] pos = TownPositions.xyzFromPlayer(e.getPlayer());
        if (pos != null) {
            st.selfX = pos[0];
            st.selfY = pos[1];
            st.selfZ = pos[2];
        }
        st.touchEpoch();
    }

    private static int packRegion(int xSector, int ySector) {
        return (xSector << 16) | (ySector & 0xffff);
    }

    private static int clampPercent(long raw) {
        return (int) Math.max(0L, Math.min(100L, raw));
    }

    private static int normalizeDurabilityMetric(long d) {
        if (d >= 0L && d <= 100L) {
            return (int) d;
        }
        return clampPercent(d / 100L);
    }

    @Override
    public Optional<ITownSnapshot> snapshot(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        MachineTownState st = stateByMachine.get(key);
        if (st == null) {
            return Optional.empty();
        }
        return Optional.of(TownSnapshots.build(key, st));
    }

    @Override
    public Flux<ITownSnapshot> observe(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Flux.empty();
        }
        return Flux.interval(Duration.ofMillis(200))
                .map(t -> snapshot(key))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
}
