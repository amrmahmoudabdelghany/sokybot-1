package org.sokybot.webview.api.dto;

import java.util.Map;

public class ActionRequestDto {
    private final String action;
    private final Map<String, Object> data;

    public ActionRequestDto(String action, Map<String, Object> data) {
        this.action = action;
        this.data = data != null ? new java.util.LinkedHashMap<>(data) : null;
    }

    public String action() { return action; }
    public Map<String, Object> data() { return data != null ? java.util.Collections.unmodifiableMap(data) : null; }
}
