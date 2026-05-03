package org.sokybot.behaviors.logistics;

/**
 * Epic #15: JVM vault mutex coordinator — allows the QM sort behavior to clear {@link LockStatus#SORTING} state after a pass.
 */
public interface IQuartermasterCoordinator {

    /**
     * Clears coord lock bookkeeping for every storage session belonging to the given swarm group (typically
     * {@link org.sokybot.engine.api.workflow.IWorkflowContext#getGroupName()}).
     */
    void resetVault(String swarmGroupId);
}
