package org.sokybot.scripting.api;

/**
 * Read-only projection of script progress for behaviors and UI.
 */
public interface IScriptExecutionSnapshot {

    String getScriptId();

    /** Next command index to execute (0-based). */
    int getCursor();

    ScriptPhase getPhase();

    long getPhaseStartedAtEpochMs();

    /** Optional human-readable fault; null when healthy. */
    String getLastError();
}
