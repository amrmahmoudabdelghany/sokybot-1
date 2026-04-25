package org.sokybot.behaviors.social.webhook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class WebhookSettings {

    private boolean enabled = false;
    private List<WebhookSinkConfig> sinks = new ArrayList<>();
    private Set<String> enabledAlertKinds = new HashSet<>(Arrays.asList(
            "GM_NEARBY", "GM_WHISPER", "UNIQUE_SPAWNED", "NOTICE_GM_BROADCAST"));
    private long dedupeCooldownMs = 60_000L;
    private int requestTimeoutSeconds = 8;

    @Data
    public static class WebhookSinkConfig {
        /** "DISCORD" / "TELEGRAM" / "GENERIC_JSON" */
        private String kind;
        private String url;
        /** e.g. Telegram chat_id; never logged */
        private String authToken;
        /** empty = inherit parent {@link WebhookSettings#enabledAlertKinds} */
        private Set<String> kindFilter = new HashSet<>();
    }
}
