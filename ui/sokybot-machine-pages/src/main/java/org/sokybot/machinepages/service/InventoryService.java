package org.sokybot.machinepages.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.inventory.InventoryItemUpdateEvent;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.sokybot.gameevents.events.inventory.InventorySizeUpdateEvent;
import org.sokybot.gameevents.events.inventory.ItemObtainedEvent;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.gamemodel.model.ITrainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing inventory page state and handling inventory-related game events.
 */
public class InventoryService implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    private final String machineFullName;
    private final ITrainer trainer;
    private final Sinks.Many<Map<String, Object>> stateSink = 
        Sinks.many().multicast().onBackpressureBuffer(100);
    private volatile List<Map<String, Object>> currentItems = new ArrayList<>();
    
    public InventoryService(String machineFullName, ITrainer trainer) {
        this.machineFullName = machineFullName;
        this.trainer = trainer;
        // Load initial state
        refreshInventory();
    }
    
    @Override
    public void handleEvent(Event osgiEvent) {
        String fullName = (String) osgiEvent.getProperty("fullName");
        if (fullName == null || !machineFullName.equals(fullName)) {
            return; // Not for this machine
        }
        
        IGameEvent event = (IGameEvent) osgiEvent.getProperty("event");
        if (event instanceof InventoryItemUpdateEvent) {
            handleItemUpdate((InventoryItemUpdateEvent) event);
        } else if (event instanceof InventoryOperationEvent) {
            handleOperation((InventoryOperationEvent) event);
        } else if (event instanceof InventorySizeUpdateEvent) {
            handleSizeUpdate((InventorySizeUpdateEvent) event);
        } else if (event instanceof ItemObtainedEvent) {
            handleItemObtained((ItemObtainedEvent) event);
        }
    }
    
    private void handleItemUpdate(InventoryItemUpdateEvent event) {
        logger.debug("Inventory item updated: slot={}, flags=0x{}", 
            event.getSlot(), Integer.toHexString(event.getUpdateFlags() & 0xFF));
        // Refresh full list for simplicity (could optimize to update single item)
        refreshInventory();
        emitStateUpdate();
    }
    
    private void handleOperation(InventoryOperationEvent event) {
        if (event.isSuccess()) {
            logger.debug("Inventory operation successful: type={}", event.getOperationType());
            refreshInventory();
            emitStateUpdate();
        }
    }
    
    private void handleSizeUpdate(InventorySizeUpdateEvent event) {
        if (event.isInventory()) {
            logger.debug("Inventory size updated: size={}", event.getSize());
            refreshInventory();
            emitStateUpdate();
        }
    }
    
    private void handleItemObtained(ItemObtainedEvent event) {
        logger.debug("Item obtained: refId={}, slot={}, quantity={}", 
            event.getItemRefId(), event.getSlot(), event.getQuantity());
        refreshInventory();
        emitStateUpdate();
    }
    
    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        Map<String, Object> newState = new HashMap<>();
        switch (action) {
            case "refresh":
                refreshInventory();
                emitStateUpdate();
                newState.put("items", new ArrayList<>(currentItems));
                return Map.of("success", true, "state", newState);
            default:
                return Map.of("success", false, "error", "Unknown action: " + action);
        }
    }
    
    public Flux<Map<String, Object>> streamInventory() {
        // Return initial state + updates
        return Flux.concat(
            Flux.just(getInventoryData()),
            stateSink.asFlux()
        );
    }
    
    private void refreshInventory() {
        List<Map<String, Object>> items = new ArrayList<>();
        if (trainer != null && trainer.getInventory() != null) {
            for (IItem item : trainer.getInventory()) {
                if (item != null) {
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("slot", item.getSlot());
                    itemData.put("name", item.getName());
                    itemData.put("count", item.getStackCount());
                    itemData.put("refId", String.valueOf(item.getLongId()));
                    items.add(itemData);
                }
            }
            // Sort by slot
            items.sort((a, b) -> Integer.compare((Integer) a.get("slot"), (Integer) b.get("slot")));
        }
        currentItems = items;
    }
    
    private Map<String, Object> getInventoryData() {
        return Map.of("items", new ArrayList<>(currentItems));
    }
    
    private void emitStateUpdate() {
        stateSink.tryEmitNext(getInventoryData());
    }
    
    public Map<String, Object> getInitialState() {
        return getInventoryData();
    }
    
    public void shutdown() {
        stateSink.tryEmitComplete();
    }
}
