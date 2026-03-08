package org.sokybot.gameevents.script;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import groovy.lang.Closure;

/**
 * Collects translator definitions registered by Groovy DSL scripts via
 * the {@code translator(opcode) { machine, packet -> ... }} method.
 *
 * An instance of this class is placed in the script binding and populated
 * during script execution. Closures are stored so that at translate time
 * the delegate can be set to a TranslatorContext (with lookup, singleEvent, etc.).
 */
public class TranslatorDslRegistry {

    private final Map<Integer, Closure<?>> translators = new LinkedHashMap<>();

    /**
     * Called from Groovy scripts:
     * <pre>
     * translator(0x3054) { machine, packet ->
     *     singleEvent(new LevelUpEvent(machine, packet.streamReader.getInt()))
     * }
     * </pre>
     */
    public void translator(int opcode, Closure<?> handler) {
        translators.put(opcode, handler);
    }

    public Map<Integer, Closure<?>> getTranslators() {
        return Collections.unmodifiableMap(translators);
    }

    public Set<Integer> getOpcodes() {
        return Collections.unmodifiableSet(translators.keySet());
    }

    public int size() {
        return translators.size();
    }
}
