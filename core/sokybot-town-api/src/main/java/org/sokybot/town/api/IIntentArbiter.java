package org.sokybot.town.api;

/**
 * Collapses multiple {@link IIntentSource} services into a single priority-ordered decision.
 * Exposed as an OSGi service; consumers (e.g. combat entry guards) reference it optionally.
 */
public interface IIntentArbiter {

    /**
     * Effective domain for this tick. Implementations define priority (typically DEATH over TOWN over COMBAT).
     */
    IntentKind decide(String machineFullName);

    /** True when the given kind currently owns the workflow (exclusive sections / preemption). */
    boolean isActive(String machineFullName, IntentKind kind);

    /**
     * Clears ownership for a domain after a coordinated hand-off (e.g. town loop finished town work).
     */
    void release(String machineFullName, IntentKind kind);
}
