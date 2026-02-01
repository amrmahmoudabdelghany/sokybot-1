package org.sokybot.actuator.training.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.actuator.training.TrainingSettings;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.NetworkPeer;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gameevents.dto.Skill;
import org.sokybot.gameevents.enums.MonsterType;
import org.sokybot.settings.MonsterPreference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles combat logic.
 */
public class CombatBehavior {

    private static final Logger log = LoggerFactory.getLogger(CombatBehavior.class);
    private static final byte AUTO_ATTACK_ACTION = 0x01;
    private static final int USE_SKILL_OPCODE = 0x7001;

    private int skillPtr = 0;
    private int currentTargetId = 0;
    private boolean targetObstructed = false;

    public void handleCastError(int targetId, org.sokybot.gameevents.enums.SkillCastErrorType errorType) {
        if (targetId == currentTargetId) {
            switch (errorType) {
                case OBSTACLE:
                    log.warn("Target {} is obstructed, skipping for now", targetId);
                    this.targetObstructed = true;
                    this.currentTargetId = 0; // Force re-find target
                    break;
                case INVALID_TARGET:
                case WRONG_WEAPON:
                case INSUFFICIENT_BOLTS:
                    log.error("Fatal combat error: {}, clearing target {}", errorType, targetId);
                    this.currentTargetId = 0;
                    break;
                case SKILL_ON_COOLDOWN:
                    // Just wait for next cycle
                    break;
                default:
                    break;
            }
        }
    }

    public boolean shouldAttack(IWorkflowContext context, TrainingSettings settings) {
        try {
            if (!settings.isAutoAttack() || settings.isDoNotAttack()) {
                return false;
            }
            return findTarget(context, settings) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void execute(IWorkflowContext context, TrainingSettings settings) {
        try {
            ITrainer trainer = context.getGameModel().getTrainer();

            // Check for previous errors
            if (trainer.getLastError() != org.sokybot.gameevents.enums.SkillCastErrorType.UNKNOWN &&
                    trainer.getLastError() != org.sokybot.gameevents.enums.SkillCastErrorType.SKILL_ON_COOLDOWN) {

                handleCastError(currentTargetId, trainer.getLastError());
                // Reset error state in behavior?
                // Wait, if it's in the model, should I reset it there?
                // The actuator shouldn't mutate the model directly...
                // But it needs to know the error was "handled".
            }

            int targetId = findTarget(context, settings);
            if (targetId <= 0)
                return;

            if (targetId != currentTargetId) {
                this.currentTargetId = targetId;
                if (settings.isIterateSkillsPerMonster()) {
                    this.skillPtr = 0;
                }
            }

            IMonster target = (IMonster) context.getGameModel().find(targetId).orElse(null);
            if (target == null)
                return;

            Skill skill = nextSkill(context, settings, target.getMonsterType());

            if (skill == null) {
                // Fallback to basic auto attack
                sendAttackPacket(context, targetId, AUTO_ATTACK_ACTION);
            } else {
                sendUseSkillPacket(context, targetId, skill.getRefId());
            }

        } catch (Exception e) {
            log.error("Failed to perform attack: {}", e.getMessage(), e);
        }
    }

    private Skill nextSkill(IWorkflowContext context, TrainingSettings settings, MonsterType type) {
        List<String> attackSkills = settings.getSkillsFor(type);
        if (attackSkills.isEmpty()) {
            attackSkills = settings.getSkillsFor(MonsterType.Normal);
        }

        if (attackSkills.isEmpty())
            return null;

        for (int i = 0; i < attackSkills.size(); i++) {
            int idx = (skillPtr + i) % attackSkills.size();
            String skillName = attackSkills.get(idx);

            Optional<Skill> skill = context.getGameModel().getTrainer().findSkill(skillName);
            if (skill.isPresent() && skill.get().isEnabled()) {
                skillPtr = (idx + 1) % attackSkills.size();
                return skill.get();
            }
        }

        return null;
    }

    private void sendAttackPacket(IWorkflowContext context, int targetId, byte action) {
        MutablePacket attackPacket = MutablePacket.getBuilder(7, ClientOpcode.CHAR_ACTION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(action)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(targetId)
                .build();
        context.getDispatcher().sendToServer(attackPacket);
    }

    private void sendUseSkillPacket(IWorkflowContext context, int targetId, int skillId) {
        MutablePacket skillPacket = MutablePacket.getBuilder(10, USE_SKILL_OPCODE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x01) // Type
                .put((byte) 0x01) // Unknown?
                .putInt(skillId)
                .put((byte) 0x01) // Has target?
                .putInt(targetId)
                .build();
        context.getDispatcher().sendToServer(skillPacket);
    }

    private int findTarget(IWorkflowContext context, TrainingSettings settings) {
        try {
            Map<Integer, IMonster> monsters = context.getGameModel().findAll(IMonster.class);
            if (monsters == null || monsters.isEmpty())
                return 0;

            ITrainer trainer = context.getGameModel().getTrainer();
            int trainerX = trainer.getX();
            int trainerY = trainer.getY();

            // Area filtering
            int centerX = settings.getAreaX() != 0 ? settings.getAreaX() : trainerX;
            int centerY = settings.getAreaY() != 0 ? settings.getAreaY() : trainerY;
            int radius = settings.getAreaRadius();

            return monsters.values().stream()
                    .filter(IMonster::isAlive)
                    .filter(m -> m.getUniqueId() != (targetObstructed ? currentTargetId : -1)) // Optional: ignore last
                                                                                               // obstructed
                    .filter(m -> settings.getMonsterPreference(m.getMonsterType()) != MonsterPreference.AVOID)
                    .filter(m -> {
                        if (radius <= 0)
                            return true;
                        // Prioritize attackers even if out of area
                        if (settings.isPreferAttackerMonster() && m.getTargetId() == trainer.getUniqueId())
                            return true;
                        return m.distance(centerX, centerY) <= radius;
                    })
                    .min((m1, m2) -> {
                        // Priority 0: Attacker (if preferred)
                        if (settings.isPreferAttackerMonster()) {
                            boolean a1 = m1.getTargetId() == trainer.getUniqueId();
                            boolean a2 = m2.getTargetId() == trainer.getUniqueId();
                            if (a1 && !a2)
                                return -1;
                            if (!a1 && a2)
                                return 1;
                        }

                        // Priority 1: Picked preference
                        MonsterPreference p1 = settings.getMonsterPreference(m1.getMonsterType());
                        MonsterPreference p2 = settings.getMonsterPreference(m2.getMonsterType());
                        if (p1 == MonsterPreference.PREFER && p2 != MonsterPreference.PREFER)
                            return -1;
                        if (p1 != MonsterPreference.PREFER && p2 == MonsterPreference.PREFER)
                            return 1;

                        // Priority 2: Currently targeted monster
                        if (m1.getUniqueId() == currentTargetId)
                            return -1;
                        if (m2.getUniqueId() == currentTargetId)
                            return 1;

                        // Priority 3: Distance
                        return Double.compare(m1.distance(centerX, centerY), m2.distance(centerX, centerY));
                    })
                    .map(IMonster::getUniqueId)
                    .orElse(0);
        } catch (Exception e) {
            log.error("Error finding target: {}", e.getMessage());
            return 0;
        }
    }
}
