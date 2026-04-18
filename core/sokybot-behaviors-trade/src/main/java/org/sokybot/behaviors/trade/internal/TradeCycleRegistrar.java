package org.sokybot.behaviors.trade.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.trade.internal.settings.FarmerTradeSettings;
import org.sokybot.behaviors.trade.internal.settings.MuleStallSettings;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assembles and registers {@code trade-cycle} and {@code stall-cycle} on each engine via
 * {@link IActuatorContext#getWorkflowRegistry()} (additive actuator; no core engine edits).
 */
@Component(service = IActuator.class, immediate = true)
public final class TradeCycleRegistrar implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(TradeCycleRegistrar.class);

    @Reference
    private IBehaviorCycleAssembler behaviorCycleAssembler;

    @Override
    public String getName() {
        return "trade-stall";
    }

    @Override
    public void initialize(IActuatorContext context) {
        BehaviorCycleSpec<FarmerTradeSettings> farmerSpec = BehaviorCycleSpec.builder(FarmerTradeSettings.class)
                .cycleId("trade-cycle")
                .priority(35)
                .settingsSupplier(FarmerTradeSettings::new)
                .defaultDelayMs(400L)
                .build();
        ICycleDefinition tradeCycle = behaviorCycleAssembler.assemble(farmerSpec);
        context.getWorkflowRegistry().registerCycle(tradeCycle);

        BehaviorCycleSpec<MuleStallSettings> stallSpec = BehaviorCycleSpec.builder(MuleStallSettings.class)
                .cycleId("stall-cycle")
                .priority(36)
                .settingsSupplier(MuleStallSettings::new)
                .defaultDelayMs(400L)
                .build();
        ICycleDefinition stallCycle = behaviorCycleAssembler.assemble(stallSpec);
        context.getWorkflowRegistry().registerCycle(stallCycle);

        log.info("Registered trade-cycle and stall-cycle for engine workflow");
    }

    @Override
    public void shutdown(IActuatorContext context) {
        boolean u1 = context.getWorkflowRegistry().unregisterCycle("trade-cycle");
        boolean u2 = context.getWorkflowRegistry().unregisterCycle("stall-cycle");
        log.info("Unregistered trade cycles (trade-cycle={}, stall-cycle={})", u1, u2);
    }
}
