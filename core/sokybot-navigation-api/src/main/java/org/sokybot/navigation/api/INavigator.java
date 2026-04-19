package org.sokybot.navigation.api;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Issues client movement for the machine bound to {@link IWorkflowContext#getMachineId()}.
 */
public interface INavigator {

    /**
     * Sends a short random displacement walk from the current trainer position.
     *
     * @param ctx workflow context with game model / dispatcher
     * @param maxRadiusWorldUnits upper bound on horizontal displacement (world units)
     */
    void randomShortWalk(IWorkflowContext ctx, float maxRadiusWorldUnits) throws NavigationException;
}
