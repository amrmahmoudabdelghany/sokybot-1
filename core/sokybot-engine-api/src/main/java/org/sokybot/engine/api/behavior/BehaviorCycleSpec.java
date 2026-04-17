package org.sokybot.engine.api.behavior;

import java.util.Objects;
import java.util.function.Supplier;

import org.sokybot.engine.api.workflow.IAction;
import org.sokybot.engine.api.workflow.IGuard;

/**
 * Immutable specification used by {@link IBehaviorCycleAssembler}.
 *
 * @param <S> settings type
 */
public final class BehaviorCycleSpec<S> {
    private final String cycleId;
    private final int priority;
    private final Class<S> settingsType;
    private final Supplier<S> settingsSupplier;
    private final IGuard entryGuard;
    private final boolean interruptible;
    private final IAction interruptionAction;
    private final long defaultDelayMs;

    private BehaviorCycleSpec(Builder<S> builder) {
        this.cycleId = builder.cycleId;
        this.priority = builder.priority;
        this.settingsType = builder.settingsType;
        this.settingsSupplier = builder.settingsSupplier;
        this.entryGuard = builder.entryGuard;
        this.interruptible = builder.interruptible;
        this.interruptionAction = builder.interruptionAction;
        this.defaultDelayMs = builder.defaultDelayMs;
    }

    public static <S> Builder<S> builder(Class<S> settingsType) {
        return new Builder<S>(settingsType);
    }

    public String getCycleId() {
        return cycleId;
    }

    public int getPriority() {
        return priority;
    }

    public Class<S> getSettingsType() {
        return settingsType;
    }

    public Supplier<S> getSettingsSupplier() {
        return settingsSupplier;
    }

    public IGuard getEntryGuard() {
        return entryGuard;
    }

    public boolean isInterruptible() {
        return interruptible;
    }

    public IAction getInterruptionAction() {
        return interruptionAction;
    }

    public long getDefaultDelayMs() {
        return defaultDelayMs;
    }

    public static final class Builder<S> {
        private final Class<S> settingsType;
        private String cycleId;
        private int priority;
        private Supplier<S> settingsSupplier;
        private IGuard entryGuard;
        private boolean interruptible = true;
        private IAction interruptionAction;
        private long defaultDelayMs;

        private Builder(Class<S> settingsType) {
            this.settingsType = Objects.requireNonNull(settingsType, "settingsType");
        }

        public Builder<S> cycleId(String cycleId) {
            this.cycleId = cycleId;
            return this;
        }

        public Builder<S> priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder<S> settingsSupplier(Supplier<S> settingsSupplier) {
            this.settingsSupplier = settingsSupplier;
            return this;
        }

        public Builder<S> entryGuard(IGuard entryGuard) {
            this.entryGuard = entryGuard;
            return this;
        }

        public Builder<S> interruptible(boolean interruptible) {
            this.interruptible = interruptible;
            return this;
        }

        public Builder<S> interruptionAction(IAction interruptionAction) {
            this.interruptionAction = interruptionAction;
            return this;
        }

        public Builder<S> defaultDelayMs(long defaultDelayMs) {
            this.defaultDelayMs = defaultDelayMs;
            return this;
        }

        public BehaviorCycleSpec<S> build() {
            if (cycleId == null || cycleId.trim().isEmpty()) {
                throw new IllegalArgumentException("cycleId is required");
            }
            Objects.requireNonNull(settingsSupplier, "settingsSupplier");
            if (defaultDelayMs < 0) {
                throw new IllegalArgumentException("defaultDelayMs must be >= 0");
            }
            return new BehaviorCycleSpec<S>(this);
        }
    }
}
