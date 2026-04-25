package org.sokybot.webview.api.dto;

import java.util.Map;

public class PageSchemaDto {
    private final Map<String, Object> value;

    public PageSchemaDto(Map<String, Object> value) {
        this.value = value != null ? new java.util.LinkedHashMap<>(value) : null;
    }

    public Map<String, Object> value() {
        return value != null ? java.util.Collections.unmodifiableMap(value) : null;
    }
}
