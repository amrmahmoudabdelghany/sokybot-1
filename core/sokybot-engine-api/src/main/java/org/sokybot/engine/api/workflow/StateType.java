package org.sokybot.engine.api.workflow;

/**
 * Enumeration of state types in a cycle.
 * Different state types have different behaviors.
 */
public enum StateType {
    /** Standard state with guard, action, and transitions */
    STANDARD,
    /** Loop state with iteration control */
    LOOP,
    /** Delay state for throttling */
    DELAY,
    /** Exit state for explicit cycle termination */
    EXIT,
    /** Conditional state with multiple transition paths */
    CONDITIONAL,
    /** Guard-only state (no action) */
    GUARD_ONLY,
    /** Retry state for retry logic */
    RETRY
}
