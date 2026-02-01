package org.sokybot.actuator.training.behavior;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.model.IFighter;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.NetworkPeer;
import org.sokybot.actuator.training.TrainingSettings;

/**
 * Handles potion usage logic (HP/MP recovery).
 */
public class PotionBehavior {

    private static final Logger log = LoggerFactory.getLogger(PotionBehavior.class);
    private static final int USE_ITEM_OPCODE = 0x704C;

    private static final String HP_POTION_PATTERN = "_HP_POTION_";
    private static final String MP_POTION_PATTERN = "_MP_POTION_";

    /**
     * Checks if HP or MP is below threshold.
     */
    public boolean needsPotion(IWorkflowContext context, TrainingSettings settings) {
        if (settings == null)
            return false;
        return (settings.isUseHpPotion() && isLowHP(context, settings)) ||
                (settings.isUseMpPotion() && isLowMP(context, settings));
    }

    /**
     * Executes potion usage logic.
     */
    public void execute(IWorkflowContext context, TrainingSettings settings) {
        if (settings == null)
            return;

        if (settings.isUseHpPotion() && isLowHP(context, settings)) {
            usePotion(context, "HP", HP_POTION_PATTERN);
        } else if (settings.isUseMpPotion() && isLowMP(context, settings)) {
            usePotion(context, "MP", MP_POTION_PATTERN);
        }
    }

    private boolean isLowHP(IWorkflowContext context, TrainingSettings settings) {
        ITrainer trainer = context.getGameModel().getTrainer();
        if (trainer instanceof IFighter) {
            IFighter fighter = (IFighter) trainer;
            int currentHP = fighter.getCurrentHP();
            int maxHP = fighter.getMaxHP();
            if (maxHP <= 0)
                return false;

            int percent = (int) ((currentHP * 100L) / maxHP);
            return percent < settings.getHpPotionThreshold();
        }
        return false;
    }

    private boolean isLowMP(IWorkflowContext context, TrainingSettings settings) {
        ITrainer trainer = context.getGameModel().getTrainer();
        if (trainer instanceof IFighter) {
            IFighter fighter = (IFighter) trainer;
            int currentMP = fighter.getCurrentMP();
            int maxMP = fighter.getMaxMP();
            if (maxMP <= 0)
                return false;

            int percent = (int) ((currentMP * 100L) / maxMP);
            return percent < settings.getMpPotionThreshold();
        }
        return false;
    }

    private void usePotion(IWorkflowContext context, String type, String pattern) {
        try {
            Optional<IItem> potionItem = findPotion(context, pattern);

            if (potionItem.isPresent()) {
                IItem item = potionItem.get();
                byte slot = item.getSlot();
                int tid = item.getRefId();

                MutablePacket useItemPacket = MutablePacket.getBuilder(5, USE_ITEM_OPCODE)
                        .packetEncoding(Encoding.ENCRYPTED)
                        .dataEncoding(Encoding.PLAIN)
                        .packetSource(NetworkPeer.BOT)
                        .put(slot)
                        .putInt(tid)
                        .build();

                context.getDispatcher().sendToServer(useItemPacket);
                log.info("Used {} potion (RefId: {}) from slot: {}", type, tid, slot);
            } else {
                log.warn("No {} potion found in inventory matching pattern: {}", type, pattern);
            }
        } catch (Exception e) {
            log.error("Failed to use {} potion: {}", type, e.getMessage(), e);
        }
    }

    private Optional<IItem> findPotion(IWorkflowContext context, String pattern) {
        try {
            // Prefer IItem for full detail, but we could also check ITrainer.getInventory()
            Map<Integer, IItem> items = context.getGameModel().findAll(IItem.class);
            return items.values().stream()
                    .filter(item -> item.getSlot() >= 0 && item.getSlot() < 100) // Basic inventory slots
                    .filter(item -> {
                        String longId = item.getLongId();
                        return longId != null && longId.contains(pattern);
                    })
                    .findFirst();
        } catch (Exception e) {
            log.error("Error searching for potion: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
