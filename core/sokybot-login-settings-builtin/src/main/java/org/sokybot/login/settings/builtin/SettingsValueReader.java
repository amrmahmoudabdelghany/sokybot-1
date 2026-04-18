package org.sokybot.login.settings.builtin;

import java.lang.reflect.Method;
import java.util.Map;

final class SettingsValueReader {

    private SettingsValueReader() {
    }

    static Object read(Object source, String fieldName) {
        if (source == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        if (source instanceof Map) {
            return ((Map<?, ?>) source).get(fieldName);
        }
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method getter = method(source.getClass(), "get" + suffix);
        if (getter == null) {
            getter = method(source.getClass(), "is" + suffix);
        }
        if (getter == null) {
            return null;
        }
        try {
            return getter.invoke(source);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method method(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (Exception ignored) {
            return null;
        }
    }

    static String asString(Object source, String key, String fallback) {
        Object value = read(source, key);
        if (value == null) {
            return fallback;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? fallback : s;
    }

    static boolean asBoolean(Object source, String key, boolean fallback) {
        Object value = read(source, key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    static int asInt(Object source, String key, int fallback) {
        Object value = read(source, key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static long asLong(Object source, String key, long fallback) {
        Object value = read(source, key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
