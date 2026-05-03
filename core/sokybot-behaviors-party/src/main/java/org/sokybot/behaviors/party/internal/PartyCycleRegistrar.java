package org.sokybot.behaviors.party.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.party.internal.blackhole.BlackHoleAnchorSettings;
import org.sokybot.behaviors.party.internal.settings.PartySettings;
import org.sokybot.party.api.PartyMatrixSettings;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers {@link PartyCycleKeys#CYCLE_NAME} when party settings are available and enabled.
 */
@Component(service = IActuator.class, immediate = true)
public final class PartyCycleRegistrar implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(PartyCycleRegistrar.class);

    private static final String BLACK_HOLE_SCOPE = "blackHole";

    private static final String PARTY_MATRIX_SCOPE = "party-matrix";

    @Reference
    private IBehaviorCycleAssembler behaviorCycleAssembler;

    @Override
    public String getName() {
        return "party";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("Party: ISettingsRegistry unavailable; party-cycle not registered");
            return;
        }
        if (!registry.getRegisteredScopes().contains("party")) {
            registry.register("party", PartySettings.class, PartySettings::new);
        }
        if (!registry.getRegisteredScopes().contains(BLACK_HOLE_SCOPE)) {
            registry.register(BLACK_HOLE_SCOPE, BlackHoleAnchorSettings.class, BlackHoleAnchorSettings::new);
        }
        if (!registry.getRegisteredScopes().contains(PARTY_MATRIX_SCOPE)) {
            registry.register(PARTY_MATRIX_SCOPE, PartyMatrixSettings.class, PartyMatrixSettings::new);
        }
        ISettingsProvider<PartySettings> provider = registry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                "party",
                PartySettings.class);
        if (provider == null) {
            log.warn("Party: settings provider unavailable; party-cycle not registered");
            return;
        }

        BehaviorCycleSpec<PartySettings> spec = BehaviorCycleSpec.builder(PartySettings.class)
                .cycleId(PartyCycleKeys.CYCLE_NAME)
                .priority(220)
                .settingsSupplier(provider::get)
                .entryGuard(ctx -> partyEntryAllowed(ctx, provider))
                .interruptible(false)
                .defaultDelayMs(400L)
                .build();

        ICycleDefinition cycle = behaviorCycleAssembler.assemble(spec);
        context.getWorkflowRegistry().registerCycle(cycle);
        log.info("Registered {} for machine {}", PartyCycleKeys.CYCLE_NAME, context.getMachineId());
    }

    @Override
    public void shutdown(IActuatorContext context) {
        boolean ok = context.getWorkflowRegistry().unregisterCycle(PartyCycleKeys.CYCLE_NAME);
        log.info("Unregistered {} (success={})", PartyCycleKeys.CYCLE_NAME, ok);
    }

    private boolean partyEntryAllowed(IWorkflowContext ctx, ISettingsProvider<PartySettings> provider) {
        try {
            PartySettings s = provider.get();
            return s != null && s.toPolicy().isPartyCycleEnabled();
        } catch (Exception e) {
            return false;
        }
    }
}
