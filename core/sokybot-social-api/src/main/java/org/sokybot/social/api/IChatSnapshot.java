package org.sokybot.social.api;

import java.util.List;
import java.util.Optional;

public interface IChatSnapshot {
    List<ChatLine> recent(SocialChannel channel, int limit);
    Optional<ChatLine> lastFromSender(String senderName);
    boolean isGmActive();
}
