package org.sokybot.social.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public interface ISocialPolicy {

    default boolean isAutoReplyEnabled() {
        return false;
    }

    default String getAutoReplyMessage() {
        return "I am currently busy. Please leave a message.";
    }

    default long getAutoReplyCooldownMs() {
        return 60000;
    }

    default long getAutoReplyPerSenderCooldownMs() {
        return 300000;
    }

    default List<String> getAutoReplyBlacklist() {
        return Collections.emptyList();
    }

    default boolean isGmEvasionEnabled() {
        return true;
    }

    default boolean isDisconnectOnGm() {
        return true;
    }

    default long getGmReloginCooldownMs() {
        return 1800000;
    }

    /** Bot-wide hook: disable all outbound webhook dispatch without touching persisted {@code WebhookSettings}. */
    default boolean isWebhookDispatchEnabled() {
        return true;
    }

    /** Default dedupe window when only an {@code ISocialPolicy} is available (ms). */
    default long getWebhookDedupeCooldownMs() {
        return 60_000L;
    }

    /** Kinds allowed for webhook dispatch when only an {@code ISocialPolicy} is available. */
    default Set<SocialAlert.Kind> getWebhookEnabledKinds() {
        return EnumSet.of(
                SocialAlert.Kind.GM_NEARBY,
                SocialAlert.Kind.GM_WHISPER,
                SocialAlert.Kind.UNIQUE_SPAWNED,
                SocialAlert.Kind.NOTICE_GM_BROADCAST);
    }
}
