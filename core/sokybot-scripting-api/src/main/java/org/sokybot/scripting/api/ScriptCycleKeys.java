package org.sokybot.scripting.api;

/**
 * Workflow ids and persistent-data keys for scripted travel.
 */
public final class ScriptCycleKeys {

    private ScriptCycleKeys() {
    }

    public static final String BEHAVIOR_EXECUTE_SCRIPT = "execute-travel-script";

    public static final String KEY_ACTIVE_SCRIPT_ID = "scripting.activeScriptId";

    public static final String KEY_LAST_STEP_AT_MS = "scripting.lastStepAtEpochMs";
}
