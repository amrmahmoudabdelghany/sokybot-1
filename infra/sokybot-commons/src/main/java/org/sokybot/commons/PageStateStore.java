package org.sokybot.commons;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A global, thread-safe store for page state that needs to persist across
 * script reloads.
 * This is especially useful for Groovy scripts in OSGi environments where
 * static fields
 * within the script class are lost when the script is recompiled/reloaded.
 * 
 * By placing this in sokybot-commons, it is visible to both the script engine
 * and the
 * script loader, ensuring that the same state is accessible throughout the
 * application lifecycle.
 */
public class PageStateStore {
    private static final Map<String, Object> store = new ConcurrentHashMap<>();

    public static Object get(String key) {
        return store.get(key);
    }

    public static Object computeIfAbsent(String key, java.util.function.Function<String, Object> mappingFunction) {
        return store.computeIfAbsent(key, mappingFunction);
    }

    public static void put(String key, Object value) {
        store.put(key, value);
    }

    public static void remove(String key) {
        store.remove(key);
    }

    public static void clear() {
        store.clear();
    }
}
