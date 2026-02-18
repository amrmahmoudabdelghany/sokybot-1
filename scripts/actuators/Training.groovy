import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder
import org.sokybot.engine.api.workflow.IWorkflowContext
import org.sokybot.settings.api.ISettingsRegistry
import org.sokybot.settings.MonsterPreference
import org.sokybot.gamemodel.model.*
import org.sokybot.gameevents.enums.*
import org.sokybot.gameevents.dto.Skill
import org.sokybot.network.packet.*
import org.sokybot.network.NetworkPeer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Training implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(Training.class)
    private static boolean registered = false

    private final PotionBehavior potionBehavior = new PotionBehavior()
    private final CombatBehavior combatBehavior = new CombatBehavior()
    private final TownBehavior townBehavior = new TownBehavior()

    @Override
    String getName() { "training" }

    @Override
    void initialize(IActuatorContext context) {
        log.info("Initializing GROOVY training actuator for machine: {}", context.getMachineId())

        try {
            def settingsRegistry = context.getService(ISettingsRegistry.class)
            
            if (settingsRegistry == null) {
                log.error("Failed to acquire ISettingsRegistry service for Training actuator!")
                return
            }

            if (!registered) {
                log.info("Registering training settings scope from Groovy")
                settingsRegistry.register("training", TrainingSettings.class, { new TrainingSettings() })
                registered = true
            }

            def settingsProvider = settingsRegistry.getProvider(
                    context.getGroupName(),
                    context.getMachineName(),
                    "training",
                    TrainingSettings.class)

            def cycle = new CycleDefinitionBuilder()
                    .name("training-cycle")
                    .priority(300)
                    .entryState("CHECK_POTION")
                    .entryGuard({ ctx ->
                        def settings = settingsProvider.get()
                        return isLoggedIn(ctx) && settings.isAutoAttack()
                    })
                    .interruptionGuard({ ctx -> potionBehavior.needsPotion(ctx, settingsProvider.get()) })
                    .interruptionPriority(500)
                    .interruptionAction({ ctx ->
                        log.warn("Interrupting training due to low HP/MP (Groovy)")
                    })
                    .interruptible(true)

                    // Potion State
                    .state("CHECK_POTION", { builder -> builder
                            .guard({ ctx -> potionBehavior.needsPotion(ctx, settingsProvider.get()) })
                            .action({ ctx -> potionBehavior.execute(ctx, settingsProvider.get()) })
                            .nextState("WAIT_AFTER_POTION")
                            .targetState("CHECK_COMBAT")
                    })
                    .delayState("WAIT_AFTER_POTION", { builder -> builder
                            .delay(500)
                            .nextState("CHECK_COMBAT")
                    })

                    // Combat State
                    .state("CHECK_COMBAT", { builder -> builder
                            .guard({ ctx -> combatBehavior.shouldAttack(ctx, settingsProvider.get()) })
                            .action({ ctx -> combatBehavior.execute(ctx, settingsProvider.get()) })
                            .nextState("WAIT_AFTER_COMBAT")
                            .targetState("CHECK_TOWN_LOOP")
                    })
                    .delayState("WAIT_AFTER_COMBAT", { builder -> builder
                            .delay(1000)
                            .nextState("CHECK_TOWN_LOOP")
                    })

                    // Town Loop State
                    .state("CHECK_TOWN_LOOP", { builder -> builder
                            .guard({ ctx -> townBehavior.shouldReturnToTown(ctx) })
                            .action({ ctx -> townBehavior.execute(ctx) })
                            .nextState("WAIT_AFTER_TOWN")
                            .targetState("CHECK_POTION")
                    })
                    .delayState("WAIT_AFTER_TOWN", { builder -> builder
                            .delay(2000)
                            .nextState("CHECK_POTION")
                    })
                    .build()

            context.getWorkflowRegistry().registerCycle(cycle)
            log.info("Training cycle registered successfully (Groovy)")

        } catch (Exception e) {
            log.error("Failed to initialize training actuator: {}", e.getMessage(), e)
        }
    }

    @Override
    void shutdown(IActuatorContext context) {
        log.info("Shutting down Groovy training actuator")
    }

    private boolean isLoggedIn(def context) {
        try {
            def trainer = context.getGameModel().getTrainer()
            return trainer != null && trainer.getUniqueId() > 0
        } catch (Exception e) {
            return false
        }
    }
}

class TrainingSettings {
    boolean autoAttack = true
    boolean doNotAttack = false
    boolean iterateSkillsPerMonster = true
    boolean preferAttackerMonster = true

    Map<MonsterType, List<String>> attackSkills = [:]
    Map<MonsterType, MonsterPreference> monsterPreferences = [:]

    // Healing Settings
    int hpPotionThreshold = 50
    int mpPotionThreshold = 50
    int hpPetPotionThreshold = 50
    boolean useHpPotion = true
    boolean useMpPotion = true
    boolean usePetPotion = true

    // Navigation / Town Loop Settings
    boolean loopInTown = true
    String scriptPath = ""
    boolean reverseReturnScroll = false

    // Training area settings
    String activeAreaName = ""
    int areaX = 0
    int areaY = 0
    int areaRadius = 0

    void addSkill(MonsterType type, String skill) {
        if (!attackSkills.containsKey(type)) attackSkills[type] = []
        attackSkills[type].add(skill)
    }

    List<String> getSkillsFor(MonsterType type) {
        return attackSkills.getOrDefault(type, [])
    }

    void setMonsterPreference(MonsterType type, MonsterPreference preference) {
        monsterPreferences[type] = preference
    }

    MonsterPreference getMonsterPreference(MonsterType type) {
        return monsterPreferences.getOrDefault(type, MonsterPreference.NONE)
    }
}

class PotionBehavior {
    private static final Logger log = LoggerFactory.getLogger(PotionBehavior.class)
    private static final int USE_ITEM_OPCODE = 0x704C

    private static final String HP_POTION_PATTERN = "_HP_POTION_"
    private static final String MP_POTION_PATTERN = "_MP_POTION_"

    boolean needsPotion(IWorkflowContext context, TrainingSettings settings) {
        if (settings == null) return false
        return (settings.useHpPotion && isLowHP(context, settings)) ||
                (settings.useMpPotion && isLowMP(context, settings))
    }

    void execute(IWorkflowContext context, TrainingSettings settings) {
        if (settings == null) return

        if (settings.useHpPotion && isLowHP(context, settings)) {
            usePotion(context, "HP", HP_POTION_PATTERN)
        } else if (settings.useMpPotion && isLowMP(context, settings)) {
            usePotion(context, "MP", MP_POTION_PATTERN)
        }
    }

    private boolean isLowHP(IWorkflowContext context, TrainingSettings settings) {
        def trainer = context.getGameModel().getTrainer()
        if (trainer instanceof IFighter) {
            int currentHP = trainer.getCurrentHP()
            int maxHP = trainer.getMaxHP()
            if (maxHP <= 0) return false
            return ((currentHP * 100L) / maxHP) < settings.hpPotionThreshold
        }
        return false
    }

    private boolean isLowMP(IWorkflowContext context, TrainingSettings settings) {
        def trainer = context.getGameModel().getTrainer()
        if (trainer instanceof IFighter) {
            int currentMP = trainer.getCurrentMP()
            int maxMP = trainer.getMaxMP()
            if (maxMP <= 0) return false
            return ((currentMP * 100L) / maxMP) < settings.mpPotionThreshold
        }
        return false
    }

    private void usePotion(IWorkflowContext context, String type, String pattern) {
        try {
            def potionItem = findPotion(context, pattern)
            if (potionItem != null) {
                byte slot = potionItem.getSlot()
                int tid = potionItem.getRefId()

                def useItemPacket = MutablePacket.getBuilder(5, USE_ITEM_OPCODE)
                        .packetEncoding(Encoding.ENCRYPTED)
                        .dataEncoding(Encoding.PLAIN)
                        .packetSource(NetworkPeer.BOT)
                        .put(slot)
                        .putInt(tid)
                        .build()

                context.getDispatcher().sendToServer(useItemPacket)
                log.info("Used {} potion (RefId: {}) from slot: {}", type, tid, slot)
            } else {
                log.warn("No {} potion found in inventory matching pattern: {}", type, pattern)
            }
        } catch (Exception e) {
            log.error("Failed to use {} potion: {}", type, e.getMessage())
        }
    }

    private def findPotion(IWorkflowContext context, String pattern) {
        try {
            def items = context.getGameModel().findAll(IItem.class)
            return items.values().find { item -> 
                item.getSlot() >= 0 && item.getSlot() < 100 && 
                item.getLongId() != null && item.getLongId().contains(pattern)
            }
        } catch (Exception e) {
            return null
        }
    }
}

class CombatBehavior {
    private static final Logger log = LoggerFactory.getLogger(CombatBehavior.class)
    private static final byte AUTO_ATTACK_ACTION = 0x01
    private static final int USE_SKILL_OPCODE = 0x7001

    private int skillPtr = 0
    private int currentTargetId = 0
    private boolean targetObstructed = false

    boolean shouldAttack(IWorkflowContext context, TrainingSettings settings) {
        if (!settings.autoAttack || settings.doNotAttack) return false
        return findTarget(context, settings) > 0
    }

    void execute(IWorkflowContext context, TrainingSettings settings) {
        try {
            def trainer = context.getGameModel().getTrainer()
            int targetId = findTarget(context, settings)
            if (targetId <= 0) return

            if (targetId != currentTargetId) {
                this.currentTargetId = targetId
                if (settings.iterateSkillsPerMonster) this.skillPtr = 0
            }

            def target = context.getGameModel().find(targetId).orElse(null)
            if (target == null) return

            def skill = nextSkill(context, settings, target.getMonsterType())

            if (skill == null) {
                sendAttackPacket(context, targetId, AUTO_ATTACK_ACTION)
            } else {
                sendUseSkillPacket(context, targetId, skill.getRefId())
            }
        } catch (Exception e) {
            log.error("Failed to perform attack: {}", e.getMessage())
        }
    }

    private Skill nextSkill(IWorkflowContext context, TrainingSettings settings, MonsterType type) {
        def attackSkills = settings.getSkillsFor(type)
        if (attackSkills.isEmpty()) attackSkills = settings.getSkillsFor(MonsterType.Normal)
        if (attackSkills.isEmpty()) return null

        for (int i = 0; i < attackSkills.size(); i++) {
            int idx = (skillPtr + i) % attackSkills.size()
            String skillName = attackSkills.get(idx)
            def skill = context.getGameModel().getTrainer().findSkill(skillName).orElse(null)
            if (skill != null && skill.isEnabled()) {
                skillPtr = (idx + 1) % attackSkills.size()
                return skill
            }
        }
        return null
    }

    private void sendAttackPacket(IWorkflowContext context, int targetId, byte action) {
        def packet = MutablePacket.getBuilder(7, ClientOpcode.CHAR_ACTION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(action)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(targetId)
                .build()
        context.getDispatcher().sendToServer(packet)
    }

    private void sendUseSkillPacket(IWorkflowContext context, int targetId, int skillId) {
        def packet = MutablePacket.getBuilder(10, USE_SKILL_OPCODE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(skillId)
                .put((byte) 0x01)
                .putInt(targetId)
                .build()
        context.getDispatcher().sendToServer(packet)
    }

    private int findTarget(IWorkflowContext context, TrainingSettings settings) {
        try {
            def monsters = context.getGameModel().findAll(IMonster.class)
            if (!monsters) return 0

            def trainer = context.getGameModel().getTrainer()
            int trainerX = trainer.getX()
            int trainerY = trainer.getY()
            int centerX = settings.areaX != 0 ? settings.areaX : trainerX
            int centerY = settings.areaY != 0 ? settings.areaY : trainerY
            int radius = settings.areaRadius

            return monsters.values().stream()
                    .filter { m -> m.isAlive() }
                    .filter { m -> m.getUniqueId() != (targetObstructed ? currentTargetId : -1) }
                    .filter { m -> settings.getMonsterPreference(m.getMonsterType()) != MonsterPreference.AVOID }
                    .filter { m ->
                        if (radius <= 0) return true
                        if (settings.preferAttackerMonster && m.getTargetId() == trainer.getUniqueId()) return true
                        return m.distance(centerX, centerY) <= radius
                    }
                    .min { m1, m2 ->
                        if (settings.preferAttackerMonster) {
                            boolean a1 = m1.getTargetId() == trainer.getUniqueId()
                            boolean a2 = m2.getTargetId() == trainer.getUniqueId()
                            if (a1 && !a2) return -1
                            if (!a1 && a2) return 1
                        }
                        def p1 = settings.getMonsterPreference(m1.getMonsterType())
                        def p2 = settings.getMonsterPreference(m2.getMonsterType())
                        if (p1 == MonsterPreference.PREFER && p2 != MonsterPreference.PREFER) return -1
                        if (p1 != MonsterPreference.PREFER && p2 == MonsterPreference.PREFER) return 1
                        if (m1.getUniqueId() == currentTargetId) return -1
                        if (m2.getUniqueId() == currentTargetId) return 1
                        return Double.compare(m1.distance(centerX, centerY), m2.distance(centerX, centerY))
                    }
                    .map { m -> m.getUniqueId() }
                    .orElse(0)
        } catch (Exception e) {
            return 0
        }
    }
}

class TownBehavior {
    private static final Logger log = LoggerFactory.getLogger(TownBehavior.class)

    boolean shouldReturnToTown(IWorkflowContext context) {
        return false
    }

    void execute(IWorkflowContext context) {
        log.info("Returning to town logic (Groovy)")
    }
}

new Training()
