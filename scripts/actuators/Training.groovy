import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder
import org.sokybot.settings.api.ISettingsRegistry
import org.sokybot.actuator.training.TrainingSettings
import org.sokybot.actuator.training.behavior.PotionBehavior
import org.sokybot.actuator.training.behavior.CombatBehavior
import org.sokybot.actuator.training.behavior.TownBehavior
import org.sokybot.gamemodel.model.ITrainer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Training implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(Training.class)

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

new Training()
