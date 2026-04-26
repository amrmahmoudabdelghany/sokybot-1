package org.sokybot.behaviors.logistics;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers {@code logistics} settings scope and the {@value LogisticsCycleKeys#CYCLE_NAME} workflow cycle.
 */
@Component(service = IActuator.class, immediate = true)
public final class LogisticsActuator implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(LogisticsActuator.class);

    @Reference
    private volatile IBehaviorCycleAssembler behaviorCycleAssembler;

    @Override
    public String getName() {
        return "logistics";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("Logistics: ISettingsRegistry unavailable; scope not registered");
        } else if (!registry.getRegisteredScopes().contains("logistics")) {
            registry.register("logistics", LogisticsSettings.class, LogisticsSettings::new);
            log.info("Registered 'logistics' settings scope");
        }

        ISettingsProvider<LogisticsSettings> provider = registry == null
                ? null
                : registry.getProvider(
                        context.getGroupName(), context.getMachineName(), "logistics", LogisticsSettings.class);
        if (provider == null) {
            log.warn("Logistics: settings provider unavailable; {} not registered", LogisticsCycleKeys.CYCLE_NAME);
            return;
        }

        BehaviorCycleSpec<LogisticsSettings> spec = BehaviorCycleSpec.builder(LogisticsSettings.class)
                .cycleId(LogisticsCycleKeys.CYCLE_NAME)
                .priority(235)
                .settingsSupplier(provider::get)
                .entryGuard(ctx -> logisticsEntryAllowed(ctx, provider))
                .interruptible(true)
                .defaultDelayMs(350L)
                .build();

        ICycleDefinition cycle = behaviorCycleAssembler.assemble(spec);
        context.getWorkflowRegistry().registerCycle(cycle);
        log.info("Registered {} for machine {}", LogisticsCycleKeys.CYCLE_NAME, context.getMachineId());
    }

    @Override
    public void shutdown(IActuatorContext context) {
        boolean ok = context.getWorkflowRegistry().unregisterCycle(LogisticsCycleKeys.CYCLE_NAME);
        log.info("Unregistered {} (success={})", LogisticsCycleKeys.CYCLE_NAME, ok);
    }

    private static boolean logisticsEntryAllowed(IWorkflowContext ctx, ISettingsProvider<LogisticsSettings> provider) {
        try {
            LogisticsSettings s = provider.get();
            return s != null && s.getRole() != null && s.getRole() != org.sokybot.trade.coordination.api.SwarmRole.NONE;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
