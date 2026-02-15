package org.sokybot.actuator.training;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.actuator.training.behavior.PotionBehavior;
import org.sokybot.actuator.training.behavior.CombatBehavior;
import org.sokybot.actuator.training.behavior.TownBehavior;

/**
 * Actuator for training functionality.
 * Manages combat, town loop, and potion usage.
 * Merges combat, town loop, and potion cycles into one training cycle.
 */
@Component(service = IActuator.class, property = { "actuator.name=training" })
public class TrainingActuator implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(TrainingActuator.class);

    private ISettingsRegistry settingsRegistry;

    private final PotionBehavior potionBehavior = new PotionBehavior();
    private final CombatBehavior combatBehavior = new CombatBehavior();
    private final TownBehavior townBehavior = new TownBehavior();

    @Reference
    public void setSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }

    @Override
    public String getName() {
        return "training";
    }

    @Override
    public void initialize(IActuatorContext context) throws BundleException {
        log.info("Initializing training actuator for machine: {}", context.getMachineId());

        try {
            // Settings scope is now registered globally by TrainingSettingsRegistrar

            ISettingsProvider<TrainingSettings> settingsProvider = settingsRegistry.getProvider(
                    context.getGroupName(),
                    context.getMachineName(),
                    "training",
                    TrainingSettings.class);

            ICycleDefinition cycle = new CycleDefinitionBuilder()
                    .name("training-cycle")
                    .priority(300)
                    .entryState("CHECK_POTION")
                    .entryGuard(ctx -> {
                        TrainingSettings settings = settingsProvider.get();
                        return isLoggedIn(ctx) && settings.isAutoAttack();
                    })
                    .interruptionGuard(ctx -> potionBehavior.needsPotion(ctx, settingsProvider.get()))
                    .interruptionPriority(500)
                    .interruptionAction(ctx -> {
                        log.warn("Interrupting training due to low HP/MP");
                        // State machine will transition naturally if we design it right,
                        // or we handle it here.
                        // But interruptionAction is usually for cleanup or immediate reaction.
                    })
                    .interruptible(true)

                    // Potion State
                    .state("CHECK_POTION", builder -> builder
                            .guard(ctx -> potionBehavior.needsPotion(ctx, settingsProvider.get()))
                            .action(ctx -> potionBehavior.execute(ctx, settingsProvider.get()))
                            .nextState("WAIT_AFTER_POTION")
                            .targetState("CHECK_COMBAT"))
                    .delayState("WAIT_AFTER_POTION", builder -> builder
                            .delay(500)
                            .nextState("CHECK_COMBAT"))

                    // Combat State
                    .state("CHECK_COMBAT", builder -> builder
                            .guard(ctx -> combatBehavior.shouldAttack(ctx, settingsProvider.get()))
                            .action(ctx -> combatBehavior.execute(ctx, settingsProvider.get()))
                            .nextState("WAIT_AFTER_COMBAT")
                            .targetState("CHECK_TOWN_LOOP"))
                    .delayState("WAIT_AFTER_COMBAT", builder -> builder
                            .delay(1000)
                            .nextState("CHECK_TOWN_LOOP"))

                    // Town Loop State
                    .state("CHECK_TOWN_LOOP", builder -> builder
                            .guard(ctx -> townBehavior.shouldReturnToTown(ctx))
                            .action(ctx -> townBehavior.execute(ctx))
                            .nextState("WAIT_AFTER_TOWN")
                            .targetState("CHECK_POTION"))
                    .delayState("WAIT_AFTER_TOWN", builder -> builder
                            .delay(2000)
                            .nextState("CHECK_POTION"))
                    .build();

            context.getWorkflowRegistry().registerCycle(cycle);
            log.info("Training cycle registered successfully");

        } catch (Exception e) {
            log.error("Failed to initialize training actuator", e);
            throw new BundleException("Failed to initialize training actuator: " + e.getMessage(), e);
        }
    }

    @Override
    public void shutdown(IActuatorContext context) {
        log.info("Shutting down training actuator for machine: {}", context.getMachineId());
    }

    private boolean isLoggedIn(IWorkflowContext context) {
        try {
            ITrainer trainer = context.getGameModel().getTrainer();
            return trainer != null && trainer.getUniqueId() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
