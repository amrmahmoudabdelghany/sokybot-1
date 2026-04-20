package org.sokybot.social.projections.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sokybot.social.api.ChatLine;
import org.sokybot.social.api.IChatSnapshot;
import org.sokybot.social.api.ISocialSnapshot;
import org.sokybot.social.api.SocialAlert;
import org.sokybot.social.api.SocialChannel;

public class SocialSnapshotImpl implements ISocialSnapshot, IChatSnapshot {

    private final EnumMap<SocialChannel, List<ChatLine>> chatHistory;
    private final List<SocialAlert> recentAlerts;
    private final Set<String> activeGmNames;
    private final long lastGmSeenAtMs;
    private final long gmStaleAfterMs = 60_000L; // default 1 min

    public SocialSnapshotImpl(MachineSocialState state, Set<String> activeGmNames) {
        this.chatHistory = new EnumMap<>(SocialChannel.class);
        for (SocialChannel ch : SocialChannel.values()) {
            List<ChatLine> lines = new ArrayList<>(state.ring.get(ch));
            this.chatHistory.put(ch, Collections.unmodifiableList(lines));
        }
        this.recentAlerts = Collections.unmodifiableList(new ArrayList<>(state.alerts));
        this.activeGmNames = Set.copyOf(activeGmNames);
        this.lastGmSeenAtMs = state.lastGmSeenAtMs;
    }

    @Override
    public IChatSnapshot getChat() {
        return this;
    }

    @Override
    public List<SocialAlert> recentAlerts(int limit) {
        if (recentAlerts.isEmpty() || limit <= 0) return Collections.emptyList();
        int size = recentAlerts.size();
        return recentAlerts.subList(Math.max(0, size - limit), size);
    }

    @Override
    public Set<String> getActiveGmNames() {
        return activeGmNames;
    }

    @Override
    public List<ChatLine> recent(SocialChannel channel, int limit) {
        List<ChatLine> lines = chatHistory.get(channel);
        if (lines == null || lines.isEmpty() || limit <= 0) return Collections.emptyList();
        int size = lines.size();
        return lines.subList(Math.max(0, size - limit), size);
    }

    @Override
    public Optional<ChatLine> lastFromSender(String senderName) {
        // Search backwards across all channels (or just PRIVATE if needed, but signature says general)
        ChatLine latest = null;
        for (List<ChatLine> lines : chatHistory.values()) {
            for (int i = lines.size() - 1; i >= 0; i--) {
                ChatLine line = lines.get(i);
                if (senderName.equals(line.getSenderName())) {
                    if (latest == null || line.getTimestampEpochMs() > latest.getTimestampEpochMs()) {
                        latest = line;
                    }
                    break;
                }
            }
        }
        return Optional.ofNullable(latest);
    }

    @Override
    public boolean isGmActive() {
        return (System.currentTimeMillis() - lastGmSeenAtMs) <= gmStaleAfterMs;
    }
}
