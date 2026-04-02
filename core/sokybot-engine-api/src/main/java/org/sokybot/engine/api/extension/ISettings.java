package org.sokybot.engine.api.extension;

import java.util.Map;

public interface ISettings {
    String getString(String key, String defaultValue);
    int getInt(String key, int defaultValue);
    long getLong(String key, long defaultValue);
    boolean getBoolean(String key, boolean defaultValue);
    void put(String key, Object value);
    Map<String, Object> asMap();
    void save();
}
