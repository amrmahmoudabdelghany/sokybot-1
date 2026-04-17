package org.sokybot.engine.api.behavior;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Pluggable behavior unit that can be assembled into a workflow cycle.
 *
 * @param <S> behavior settings type
 */
public interface IBehavior<S> {

    String id();

    default int order() {
        return 0;
    }

    default boolean appliesTo(String cycleId) {
        return true;
    }

    Class<S> settingsType();

    boolean applies(IWorkflowContext context, S settings);

    BehaviorStatus execute(IWorkflowContext context, S settings);

    default long postDelayMs() {
        return 0L;
    }

    default boolean canInterrupt() {
        return false;
    }

    default int interruptionPriority() {
        return 0;
    }
}
