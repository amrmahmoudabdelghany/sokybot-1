package org.sokybot.behaviors.training.internal;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.behaviors.training.api.TrainingSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.model.IFighter;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

@Component(service = IBehavior.class, immediate = true)
public class PotionBehavior implements IBehavior<TrainingSettings> {
    private static final Logger log = LoggerFactory.getLogger(PotionBehavior.class);
    private static final int USE_ITEM_OPCODE = 0x704C;
    private static final String HP_POTION_PATTERN = "_HP_POTION_";
    private static final String MP_POTION_PATTERN = "_MP_POTION_";

    @Override
    public String id() {
        return "potion";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return "training-cycle".equals(cycleId);
    }

    @Override
    public Class<TrainingSettings> settingsType() {
        return TrainingSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, TrainingSettings settings) {
        return needsPotion(context, settings);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TrainingSettings settings) {
        if (settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        if (settings.isUseHpPotion() && isLowHP(context, settings)) {
            return usePotion(context, "HP", HP_POTION_PATTERN);
        }
        if (settings.isUseMpPotion() && isLowMP(context, settings)) {
            return usePotion(context, "MP", MP_POTION_PATTERN);
        }
        return BehaviorStatus.SKIPPED;
    }

    @Override
    public long postDelayMs() {
        return 500L;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public int interruptionPriority() {
        return 500;
    }

    private boolean needsPotion(IWorkflowContext context, TrainingSettings settings) {
        if (settings == null) {
            return false;
        }
        return (settings.isUseHpPotion() && isLowHP(context, settings))
                || (settings.isUseMpPotion() && isLowMP(context, settings));
    }

    private boolean isLowHP(IWorkflowContext context, TrainingSettings settings) {
        Object trainer = context.getGameModel().getTrainer();
        if (trainer instanceof IFighter) {
            IFighter fighter = (IFighter) trainer;
            int maxHP = fighter.getMaxHP();
            if (maxHP <= 0) {
                return false;
            }
            return ((fighter.getCurrentHP() * 100L) / maxHP) < settings.getHpPotionThreshold();
        }
        return false;
    }

    private boolean isLowMP(IWorkflowContext context, TrainingSettings settings) {
        Object trainer = context.getGameModel().getTrainer();
        if (trainer instanceof IFighter) {
            IFighter fighter = (IFighter) trainer;
            int maxMP = fighter.getMaxMP();
            if (maxMP <= 0) {
                return false;
            }
            return ((fighter.getCurrentMP() * 100L) / maxMP) < settings.getMpPotionThreshold();
        }
        return false;
    }

    private BehaviorStatus usePotion(IWorkflowContext context, String type, String pattern) {
        try {
            IItem potionItem = findPotion(context, pattern);
            if (potionItem == null) {
                log.warn("No {} potion found in inventory matching pattern: {}", type, pattern);
                return BehaviorStatus.SKIPPED;
            }

            byte slot = potionItem.getSlot();
            int tid = potionItem.getRefId();
            MutablePacket packet = MutablePacket.getBuilder(5, USE_ITEM_OPCODE)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .put(slot)
                    .putInt(tid)
                    .build();
            context.getDispatcher().sendToServer(packet);
            log.info("Used {} potion (RefId: {}) from slot: {}", type, tid, slot);
            return BehaviorStatus.EXECUTED;
        } catch (Exception e) {
            log.error("Failed to use {} potion: {}", type, e.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    private IItem findPotion(IWorkflowContext context, String pattern) {
        try {
            List<IItem> items = context.getGameModel().snapshotAll(IItem.class);
            for (IItem item : items) {
                if (item.getSlot() >= 0
                        && item.getSlot() < 100
                        && item.getLongId() != null
                        && item.getLongId().contains(pattern)) {
                    return item;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
