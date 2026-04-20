package org.sokybot.social.api;

import java.util.List;
import java.util.Set;

public interface ISocialSnapshot {
    IChatSnapshot getChat();
    List<SocialAlert> recentAlerts(int limit);
    Set<String> getActiveGmNames();
}
