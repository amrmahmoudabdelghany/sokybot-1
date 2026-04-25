package org.sokybot.behaviors.social.webhook;

import java.util.Collections;
import java.util.Set;

/**
 * Resolved outbound sink (derived from persisted {@link WebhookSettings.WebhookSinkConfig}).
 */
public final class WebhookSink {

    public enum Kind {
        DISCORD,
        TELEGRAM,
        GENERIC_JSON
    }

    private final Kind kind;
    private final String url;
    private final String authToken;
    private final Set<String> kindFilter;

    public WebhookSink(Kind kind, String url, String authToken, Set<String> kindFilter) {
        this.kind = kind;
        this.url = url != null ? url : "";
        this.authToken = authToken != null ? authToken : "";
        this.kindFilter = kindFilter != null ? kindFilter : Collections.emptySet();
    }

    public Kind getKind() {
        return kind;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Set<String> getKindFilter() {
        return kindFilter;
    }

    public boolean allowsKind(String alertKindName) {
        return kindFilter.isEmpty() || kindFilter.contains(alertKindName);
    }
}
