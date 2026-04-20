package org.sokybot.social.api;

import java.util.Collections;
import java.util.List;

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
}
