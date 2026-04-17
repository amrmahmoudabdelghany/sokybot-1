package org.sokybot.engine.internal.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IGuard;
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder;

@Component(service = IBehaviorCycleAssembler.class, immediate = true)
public final class BehaviorCycleAssemblerImpl implements IBehaviorCycleAssembler {

    private IBehaviorRegistry behaviorRegistry;

    @Reference
    void setBehaviorRegistry(IBehaviorRegistry behaviorRegistry) {
        this.behaviorRegistry = behaviorRegistry;
    }

    @Override
    public <S> ICycleDefinition assemble(BehaviorCycleSpec<S> spec) {
        Objects.requireNonNull(spec, "spec");
        List<IBehavior<?>> raw = behaviorRegistry.forCycle(spec.getCycleId());
        if (raw.isEmpty()) {
            throw new IllegalStateException("No IBehavior services bound for cycle: " + spec.getCycleId());
        }

        List<IBehavior<S>> typedBehaviors = new ArrayList<IBehavior<S>>(raw.size());
        for (IBehavior<?> behavior : raw) {
            typedBehaviors.add(castBehavior(behavior, spec.getSettingsType(), spec.getCycleId()));
        }

        CycleDefinitionBuilder builder = new CycleDefinitionBuilder()
                .name(spec.getCycleId())
                .priority(spec.getPriority())
                .entryState(checkStateName(typedBehaviors.get(0)));

        if (spec.getEntryGuard() != null) {
            builder.entryGuard(spec.getEntryGuard());
        }
        builder.interruptible(spec.isInterruptible());
        applyInterruptors(builder, typedBehaviors, spec);

        for (int i = 0; i < typedBehaviors.size(); i++) {
            IBehavior<S> behavior = typedBehaviors.get(i);
            IBehavior<S> next = typedBehaviors.get((i + 1) % typedBehaviors.size());
            String checkName = checkStateName(behavior);
            String waitName = waitStateName(behavior);
            String nextCheck = checkStateName(next);
            Supplier<S> settingsSupplier = spec.getSettingsSupplier();

            builder.state(checkName, state -> state
                    .guard(ctx -> behavior.applies(ctx, settingsSupplier.get()))
                    .action(ctx -> {
                        BehaviorStatus status = behavior.execute(ctx, settingsSupplier.get());
                        if (status == BehaviorStatus.INTERRUPT) {
                            ctx.log("DEBUG", "Behavior {} requested INTERRUPT in cycle {}", behavior.id(), spec.getCycleId());
                        }
                    })
                    .nextState(waitName)
                    .targetState(nextCheck));

            builder.delayState(waitName, delay -> delay
                    .delay(resolveDelayMs(behavior, spec))
                    .nextState(nextCheck));
        }

        return builder.build();
    }

    private <S> void applyInterruptors(CycleDefinitionBuilder builder, List<IBehavior<S>> behaviors, BehaviorCycleSpec<S> spec) {
        List<IBehavior<S>> interruptors = new ArrayList<IBehavior<S>>();
        int maxPriority = spec.getPriority();
        for (IBehavior<S> behavior : behaviors) {
            if (behavior.canInterrupt()) {
                interruptors.add(behavior);
                maxPriority = Math.max(maxPriority, behavior.interruptionPriority());
            }
        }

        if (!interruptors.isEmpty()) {
            final Supplier<S> settingsSupplier = spec.getSettingsSupplier();
            final List<IBehavior<S>> guardBehaviors = interruptors;
            IGuard interruptionGuard = ctx -> {
                S settings = settingsSupplier.get();
                for (IBehavior<S> behavior : guardBehaviors) {
                    if (behavior.applies(ctx, settings)) {
                        return true;
                    }
                }
                return false;
            };
            builder.interruptionGuard(interruptionGuard)
                    .interruptionPriority(maxPriority);
        }

        if (spec.getInterruptionAction() != null) {
            builder.interruptionAction(spec.getInterruptionAction());
        }
    }

    private int resolveDelayMs(IBehavior<?> behavior, BehaviorCycleSpec<?> spec) {
        long delayMs = behavior.postDelayMs();
        if (delayMs <= 0) {
            delayMs = spec.getDefaultDelayMs();
        }
        if (delayMs > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) delayMs;
    }

    @SuppressWarnings("unchecked")
    private <S> IBehavior<S> castBehavior(IBehavior<?> behavior, Class<S> expectedType, String cycleId) {
        if (!expectedType.equals(behavior.settingsType())) {
            throw new IllegalStateException("Behavior " + behavior.id() + " for cycle " + cycleId
                    + " expects settings type " + behavior.settingsType().getName()
                    + " but spec uses " + expectedType.getName());
        }
        return (IBehavior<S>) behavior;
    }

    private static String checkStateName(IBehavior<?> behavior) {
        return "CHECK_" + normalize(behavior.id());
    }

    private static String waitStateName(IBehavior<?> behavior) {
        return "WAIT_AFTER_" + normalize(behavior.id());
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
