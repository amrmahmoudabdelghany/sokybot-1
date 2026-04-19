package org.sokybot.scripting.api;

import java.util.Objects;

/**
 * Immutable {@link IScriptExecutionSnapshot}.
 */
public final class ScriptExecutionSnapshot implements IScriptExecutionSnapshot {

    private final String scriptId;
    private final int cursor;
    private final ScriptPhase phase;
    private final long phaseStartedAtEpochMs;
    private final String lastError;

    public ScriptExecutionSnapshot(String scriptId, int cursor, ScriptPhase phase, long phaseStartedAtEpochMs,
            String lastError) {
        this.scriptId = scriptId;
        this.cursor = cursor;
        this.phase = Objects.requireNonNull(phase, "phase");
        this.phaseStartedAtEpochMs = phaseStartedAtEpochMs;
        this.lastError = lastError;
    }

    @Override
    public String getScriptId() {
        return scriptId;
    }

    @Override
    public int getCursor() {
        return cursor;
    }

    @Override
    public ScriptPhase getPhase() {
        return phase;
    }

    @Override
    public long getPhaseStartedAtEpochMs() {
        return phaseStartedAtEpochMs;
    }

    @Override
    public String getLastError() {
        return lastError;
    }
}
