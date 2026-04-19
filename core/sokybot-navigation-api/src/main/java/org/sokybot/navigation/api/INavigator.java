package org.sokybot.navigation.api;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Issues client movement for the machine bound to {@link IWorkflowContext#getMachineId()}.
 */
public interface INavigator {

    /**
     * Walk toward an explicit world target (game X/Y/Z).
     *
     * @throws NavigationException when the movement cannot be issued (or path blocked when a pathfinder is present)
     */
    default void walkTo(IWorkflowContext ctx, WorldPoint target) throws NavigationException {
        throw new NavigationException("walkTo not implemented");
    }

    /**
     * Sends a short random displacement walk from the current trainer position.
     *
     * @param ctx workflow context with game model / dispatcher
     * @param maxRadiusWorldUnits upper bound on horizontal displacement (world units)
     */
    void randomShortWalk(IWorkflowContext ctx, float maxRadiusWorldUnits) throws NavigationException;
}
