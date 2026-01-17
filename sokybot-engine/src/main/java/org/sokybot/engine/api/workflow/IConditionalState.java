package org.sokybot.engine.api.workflow;

import java.util.List;

/**
 * Conditional state - complex routing with multiple paths.
 */
public interface IConditionalState extends IWorkflowState {
    
    /**
     * Multiple conditional transitions.
     * Evaluated in order, first matching guard wins.
     * 
     * @return List of conditional transitions
     */
    List<IConditionalTransition> getTransitions();
    
    /**
     * Default transition if no conditions match.
     * 
     * @return Default state name
     */
    String getDefaultState();
}
