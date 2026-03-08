package org.sokybot.engine.api.scripting;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for the scripting engine.
 * Allows executing scripts dynamically.
 */
public interface IScriptEngine {

    /**
     * Execute a script source code.
     * 
     * @param script  the source code
     * @param context variables to bind to the script
     * @return the result of the script execution
     * @throws ScriptException if compilation or execution fails
     */
    Object execute(String script, Map<String, Object> context) throws ScriptException;

    /**
     * Execute a script asynchronously.
     * 
     * @param script  the source code
     * @param context variables to bind
     * @return future with the result
     */
    CompletableFuture<Object> executeAsync(String script, Map<String, Object> context);

    /**
     * Get the name of the scripting language (e.g., "Groovy").
     */
    String getLanguageName();

    /**
     * Validate a script without executing it (compile-only).
     *
     * @param script the source code
     * @return list of error messages (empty if valid)
     */
    default java.util.List<String> validate(String script) {
        return java.util.Collections.emptyList();
    }
}
