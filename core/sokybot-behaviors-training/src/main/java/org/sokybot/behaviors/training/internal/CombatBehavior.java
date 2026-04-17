package org.sokybot.behaviors.training.internal;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.behaviors.training.api.TrainingSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.Skill;
import org.sokybot.gameevents.enums.MonsterType;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public class CombatBehavior implements IBehavior<TrainingSettings> {
    private static final Logger log = LoggerFactory.getLogger(CombatBehavior.class);
    private static final byte AUTO_ATTACK_ACTION = 0x01;
    private static final int USE_SKILL_OPCODE = 0x7001;

    private int skillPtr;
    private int currentTargetId;
    private boolean targetObstructed;

    @Override
    public String id() {
        return "combat";
    }

    @Override
    public int order() {
        return 20;
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
        if (settings == null || !settings.isAutoAttack() || settings.isDoNotAttack()) {
            return false;
        }
        return findTarget(context, settings) > 0;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TrainingSettings settings) {
        if (settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        try {
            ITrainer trainer = context.getGameModel().getTrainer();
            int targetId = findTarget(context, settings);
            if (targetId <= 0) {
                return BehaviorStatus.SKIPPED;
            }

            if (targetId != currentTargetId) {
                currentTargetId = targetId;
                if (settings.isIterateSkillsPerMonster()) {
                    skillPtr = 0;
                }
            }

            Object targetObj = context.getGameModel().find(targetId).orElse(null);
            IMonster target = targetObj instanceof IMonster ? (IMonster) targetObj : null;
            if (target == null) {
                return BehaviorStatus.SKIPPED;
            }

            Skill skill = nextSkill(context, settings, target.getMonsterType());
            if (skill == null) {
                sendAttackPacket(context, targetId, AUTO_ATTACK_ACTION);
            } else {
                sendUseSkillPacket(context, targetId, skill.getRefId());
            }

            return BehaviorStatus.EXECUTED;
        } catch (Exception e) {
            log.error("Failed to perform attack: {}", e.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    @Override
    public long postDelayMs() {
        return 1000L;
    }

    private Skill nextSkill(IWorkflowContext context, TrainingSettings settings, MonsterType type) {
        List<String> attackSkills = settings.getSkillsFor(type);
        if (attackSkills.isEmpty()) {
            attackSkills = settings.getSkillsFor(MonsterType.Normal);
        }
        if (attackSkills.isEmpty()) {
            return null;
        }

        for (int i = 0; i < attackSkills.size(); i++) {
            int idx = (skillPtr + i) % attackSkills.size();
            String skillName = attackSkills.get(idx);
            Skill skill = context.getGameModel().getTrainer().findSkill(skillName).orElse(null);
            if (skill != null && skill.isEnabled()) {
                skillPtr = (idx + 1) % attackSkills.size();
                return skill;
            }
        }
        return null;
    }

    private void sendAttackPacket(IWorkflowContext context, int targetId, byte action) {
        MutablePacket packet = MutablePacket.getBuilder(7, ClientOpcode.CHAR_ACTION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(action)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(targetId)
                .build();
        context.getDispatcher().sendToServer(packet);
    }

    private void sendUseSkillPacket(IWorkflowContext context, int targetId, int skillId) {
        MutablePacket packet = MutablePacket.getBuilder(10, USE_SKILL_OPCODE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(skillId)
                .put((byte) 0x01)
                .putInt(targetId)
                .build();
        context.getDispatcher().sendToServer(packet);
    }

    private int findTarget(IWorkflowContext context, TrainingSettings settings) {
        try {
            List<IMonster> monsters = context.getGameModel().snapshotAll(IMonster.class);
            if (monsters == null || monsters.isEmpty()) {
                return 0;
            }

            ITrainer trainer = context.getGameModel().getTrainer();
            int trainerX = trainer.getX();
            int trainerY = trainer.getY();
            int centerX = settings.getAreaX() != 0 ? settings.getAreaX() : trainerX;
            int centerY = settings.getAreaY() != 0 ? settings.getAreaY() : trainerY;
            int radius = settings.getAreaRadius();

            IMonster best = null;
            for (IMonster monster : monsters) {
                if (!monster.isAlive()) {
                    continue;
                }
                if (monster.getUniqueId() == (targetObstructed ? currentTargetId : -1)) {
                    continue;
                }
                if (settings.getMonsterPreference(monster.getMonsterType()) == org.sokybot.settings.MonsterPreference.AVOID) {
                    continue;
                }
                if (radius > 0 && !(settings.isPreferAttackerMonster() && monster.getTargetId() == trainer.getUniqueId())
                        && monster.distance(centerX, centerY) > radius) {
                    continue;
                }
                if (best == null || compareMonsters(best, monster, trainer, settings, centerX, centerY) > 0) {
                    best = monster;
                }
            }
            return best != null ? best.getUniqueId() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int compareMonsters(IMonster a, IMonster b, ITrainer trainer, TrainingSettings settings, int centerX, int centerY) {
        if (settings.isPreferAttackerMonster()) {
            boolean aTargetsTrainer = a.getTargetId() == trainer.getUniqueId();
            boolean bTargetsTrainer = b.getTargetId() == trainer.getUniqueId();
            if (aTargetsTrainer && !bTargetsTrainer) {
                return -1;
            }
            if (!aTargetsTrainer && bTargetsTrainer) {
                return 1;
            }
        }
        org.sokybot.settings.MonsterPreference aPref = settings.getMonsterPreference(a.getMonsterType());
        org.sokybot.settings.MonsterPreference bPref = settings.getMonsterPreference(b.getMonsterType());
        if (aPref == org.sokybot.settings.MonsterPreference.PREFER && bPref != org.sokybot.settings.MonsterPreference.PREFER) {
            return -1;
        }
        if (aPref != org.sokybot.settings.MonsterPreference.PREFER && bPref == org.sokybot.settings.MonsterPreference.PREFER) {
            return 1;
        }
        if (a.getUniqueId() == currentTargetId) {
            return -1;
        }
        if (b.getUniqueId() == currentTargetId) {
            return 1;
        }
        return Double.compare(a.distance(centerX, centerY), b.distance(centerX, centerY));
    }
}
