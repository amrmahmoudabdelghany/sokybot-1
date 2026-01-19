package org.sokybot.actuator.training;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder;
import org.sokybot.gamemodel.model.IFighter;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.NetworkPeer;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.api.ISettingsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Actuator for training functionality.
 * Manages combat, town loop, and potion usage.
 * Merges combat, town loop, and potion cycles into one training cycle.
 */
@Component(service = IActuator.class, property = {"actuator.name=training"})
public class TrainingActuator implements IActuator {
    
    private static final Logger log = LoggerFactory.getLogger(TrainingActuator.class);
    
    // Action types for CHAR_ACTION opcode
    private static final byte AUTO_ATTACK_ACTION = 0x01;
    private static final byte USE_SKILL_ACTION = 0x02;
    
    // Use item opcode
    private static final int USE_ITEM_OPCODE = 0x704C;
    
    @Override
    public String getName() {
        return "training";
    }

    private ISettingsRegistry settingsRegistry;

    @Reference
    public void setSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }
    
    @Override
    public void initialize(IActuatorContext context) throws BundleException {
        log.info("Initializing training actuator for machine: {}", context.getMachineId());
        
        try {
            // Register settings
            settingsRegistry.register("training", TrainingSettings.class, TrainingSettings::new);
            
            // Get settings provider
            ISettingsProvider<TrainingSettings> settingsProvider = settingsRegistry.getProvider(
                context.getGroupName(), 
                context.getMachineName(), 
                "training", 
                TrainingSettings.class
            );

            // Register training cycle with interruption support for low HP/MP
            ICycleDefinition cycle = new CycleDefinitionBuilder()
                .name("training-cycle")
                .priority(300) // Lower priority - training happens after login
                .entryState("CHECK_POTION")
                .entryGuard(ctx -> {
                    // Only enter if logged in and auto-attack enabled
                    TrainingSettings settings = settingsProvider.get();
                    return isLoggedIn(ctx) && settings.isAutoAttack();
                })
                // Interruption guard for low HP/MP - high priority interruption
                .interruptionGuard(ctx -> {
                    return isLowHP(ctx) || isLowMP(ctx);
                })
                .interruptionPriority(500) // Higher priority interruption
                .interruptionAction(ctx -> {
                    log.warn("Interrupting training due to low HP/MP");
                    // Potion usage will be handled by potion state
                })
                .interruptible(true) // Training can be interrupted
                
                // Potion checking (highest priority in cycle)
                .state("CHECK_POTION", builder -> builder
                    .guard(ctx -> {
                        // Check if HP or MP is low
                        return isLowHP(ctx) || isLowMP(ctx);
                    })
                    .action(ctx -> {
                        log.info("HP/MP low, using potion");
                        usePotion(ctx);
                    })
                    .nextState("WAIT_AFTER_POTION")
                    .targetState("CHECK_COMBAT")) // If guard fails, continue to combat
                .delayState("WAIT_AFTER_POTION", builder -> builder
                    .delay(500) // Wait 500ms after using potion
                    .nextState("CHECK_COMBAT"))
                
                // Combat state
                .state("CHECK_COMBAT", builder -> builder
                    .guard(ctx -> {
                        // Check if should attack
                        return shouldAttack(ctx, settingsProvider.get());
                    })
                    .action(ctx -> {
                        log.info("Engaging in combat");
                        performAttack(ctx);
                    })
                    .nextState("WAIT_AFTER_COMBAT")
                    .targetState("CHECK_TOWN_LOOP")) // If guard fails, go to town loop
                .delayState("WAIT_AFTER_COMBAT", builder -> builder
                    .delay(1000) // Wait 1 second after combat
                    .nextState("CHECK_TOWN_LOOP"))
                
                // Town loop state
                .state("CHECK_TOWN_LOOP", builder -> builder
                    .guard(ctx -> {
                        // Check if should return to town (low inventory, need repair, etc.)
                        return shouldReturnToTown(ctx);
                    })
                    .action(ctx -> {
                        log.info("Returning to town");
                        returnToTown(ctx);
                    })
                    .nextState("WAIT_AFTER_TOWN")
                    .targetState("CHECK_POTION")) // If guard fails, loop back to potion check
                .delayState("WAIT_AFTER_TOWN", builder -> builder
                    .delay(2000) // Wait 2 seconds after town actions
                    .nextState("CHECK_POTION")) // Loop back to start
                
                .build();
            
            context.getWorkflowRegistry().registerCycle(cycle);
            log.info("Training cycle registered successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize training actuator: {}", e.getMessage(), e);
            throw new BundleException("Failed to initialize training actuator: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void shutdown(IActuatorContext context) {
        log.info("Shutting down training actuator for machine: {}", context.getMachineId());
    }
    
    /**
     * Checks if the player is logged in.
     */
    private boolean isLoggedIn(IWorkflowContext context) {
        try {
            ITrainer trainer = context.getGameModel().getTrainer();
            return trainer != null && trainer.getUniqueId() > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if HP is low (below 30%).
     */
    private boolean isLowHP(IWorkflowContext context) {
        try {
            ITrainer trainer = context.getGameModel().getTrainer();
            if (trainer instanceof IFighter) {
                IFighter fighter = (IFighter) trainer;
                int currentHP = fighter.getCurrentHP();
                int maxHP = fighter.getMaxHP();
                return maxHP > 0 && (currentHP * 100 / maxHP) < 30;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if MP is low (below 30%).
     */
    private boolean isLowMP(IWorkflowContext context) {
        try {
            ITrainer trainer = context.getGameModel().getTrainer();
            if (trainer instanceof IFighter) {
                IFighter fighter = (IFighter) trainer;
                int currentMP = fighter.getCurrentMP();
                int maxMP = fighter.getMaxMP();
                return maxMP > 0 && (currentMP * 100 / maxMP) < 30;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Uses a potion from inventory.
     */
    private void usePotion(IWorkflowContext context) {
        try {
            // Find HP or MP potion in inventory
            Optional<Byte> potionSlot = findPotionSlot(context);
            
            if (potionSlot.isPresent()) {
                byte slot = potionSlot.get();
                
                // Find item to get TID (Type ID / RefId)
                Optional<IItem> item = findItemBySlot(context, slot);
                if (item.isPresent()) {
                    IItem potionItem = item.get();
                    int tid = potionItem.getRefId(); // Get TID (RefId is the item type ID)
                    
                    // Create use item packet
                    // Packet format: slot (byte) + TID (int for newer clients, short for older)
                    // Using int for TID (assumes newer client version)
                    MutablePacket useItemPacket = MutablePacket.getBuilder(5, USE_ITEM_OPCODE)
                        .packetEncoding(Encoding.ENCRYPTED)
                        .dataEncoding(Encoding.PLAIN)
                        .packetSource(NetworkPeer.BOT)
                        .put(slot)
                        .putInt(tid)
                        .build();
                    
                    context.getDispatcher().sendToServer(useItemPacket);
                    log.debug("Used potion from slot: {}", slot);
                } else {
                    log.warn("Potion item not found in slot: {}", slot);
                }
            } else {
                log.warn("No potion found in inventory");
            }
        } catch (Exception e) {
            log.error("Failed to use potion: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Finds a potion slot in inventory.
     */
    private Optional<Byte> findPotionSlot(IWorkflowContext context) {
        try {
            // Search inventory for potions (simplified - would need to check item type)
            // This is a placeholder - actual implementation would query game model
            Map<Integer, IItem> inventory = context.getGameModel().findAll(IItem.class);
            
            // Look for potions (HP/MP recovery items)
            for (Map.Entry<Integer, IItem> entry : inventory.entrySet()) {
                IItem item = entry.getValue();
                // Check if item is a potion (would need item type checking)
                // For now, just return first item slot as placeholder
                return Optional.of(item.getSlot());
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to find potion slot: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Finds an item by slot number.
     */
    private Optional<IItem> findItemBySlot(IWorkflowContext context, byte slot) {
        try {
            Map<Integer, IItem> inventory = context.getGameModel().findAll(IItem.class);
            for (IItem item : inventory.values()) {
                if (item.getSlot() == slot) {
                    return Optional.of(item);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Checks if should attack.
     */
    private boolean shouldAttack(IWorkflowContext context, TrainingSettings settings) {
        try {
            // Check if auto-attack is enabled
            if (!settings.isAutoAttack()) {
                return false;
            }
            
            // Check if doNotAttack flag is set
            if (settings.isDoNotAttack()) {
                return false;
            }
            
            // Check if there are targets nearby (simplified - would check game model)
            // This is a placeholder
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Performs attack action.
     */
    private void performAttack(IWorkflowContext context) {
        try {
            // Find target (simplified - would query game model for monsters)
            // This is a placeholder
            int targetId = findTarget(context);
            
            if (targetId > 0) {
                // Create attack packet
                MutablePacket attackPacket = MutablePacket.getBuilder(7, ClientOpcode.CHAR_ACTION)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .put(AUTO_ATTACK_ACTION)
                    .put((byte) 0x01)
                    .put((byte) 0x01)
                    .putInt(targetId)
                    .build();
                
                context.getDispatcher().sendToServer(attackPacket);
                log.debug("Attacked target: {}", targetId);
            }
        } catch (Exception e) {
            log.error("Failed to perform attack: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Finds a target to attack.
     */
    private int findTarget(IWorkflowContext context) {
        // Placeholder - would query game model for nearby monsters
        // Return 0 if no target found
        return 0;
    }
    
    /**
     * Checks if should return to town.
     */
    private boolean shouldReturnToTown(IWorkflowContext context) {
        try {
            // Check various conditions (inventory full, equipment broken, etc.)
            // This is a placeholder
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Returns to town.
     */
    private void returnToTown(IWorkflowContext context) {
        // Town loop logic would be here
        // Would handle movement, NPC interaction, etc.
        log.debug("Returning to town");
    }
}
