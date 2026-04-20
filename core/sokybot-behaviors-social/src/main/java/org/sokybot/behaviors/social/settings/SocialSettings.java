package org.sokybot.behaviors.social.settings;

import java.util.ArrayList;
import java.util.List;

import org.sokybot.social.api.ISocialPolicy;

import lombok.Data;

@Data
public class SocialSettings {
    private boolean autoReplyEnabled = false;
    private String autoReplyMessage = "I am currently busy. Please leave a message.";
    private long autoReplyCooldownMs = 60000;
    private long autoReplyPerSenderCooldownMs = 300000;
    private List<String> autoReplyBlacklist = new ArrayList<>();

    private boolean gmEvasionEnabled = true;
    private boolean disconnectOnGm = true;
    private long gmReloginCooldownMs = 1800000;

    public ISocialPolicy toPolicy() {
        return new ISocialPolicy() {
            @Override
            public boolean isAutoReplyEnabled() {
                return autoReplyEnabled;
            }

            @Override
            public String getAutoReplyMessage() {
                return autoReplyMessage;
            }

            @Override
            public long getAutoReplyCooldownMs() {
                return autoReplyCooldownMs;
            }

            @Override
            public long getAutoReplyPerSenderCooldownMs() {
                return autoReplyPerSenderCooldownMs;
            }

            @Override
            public List<String> getAutoReplyBlacklist() {
                return autoReplyBlacklist;
            }

            @Override
            public boolean isGmEvasionEnabled() {
                return gmEvasionEnabled;
            }

            @Override
            public boolean isDisconnectOnGm() {
                return disconnectOnGm;
            }

            @Override
            public long getGmReloginCooldownMs() {
                return gmReloginCooldownMs;
            }
        };
    }
}
