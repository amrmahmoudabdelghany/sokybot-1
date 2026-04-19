package org.sokybot.combat.api;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Produces an immutable {@link ICombatSettings} view for the active machine workflow.
 */
public interface ICombatSettingsSnapshotter {

    /**
     * @param forceRefresh when true, implementations may bypass a short-lived cache.
     */
    ICombatSettings snapshot(IWorkflowContext context, boolean forceRefresh);
}
