package org.sokybot.webview.api.dto;

import java.util.Map;

public class ActionResponseDto {
    private final boolean success;
    private final String message;
    private final Map<String, Object> data;

    public ActionResponseDto(boolean success, String message, Map<String, Object> data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean success() { return success; }
    public String message() { return message; }
    public Map<String, Object> data() { return data; }
}
