package org.sokybot.social.projections.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import org.sokybot.social.api.ChatLine;
import org.sokybot.social.api.ISocialSnapshot;
import org.sokybot.social.api.SocialAlert;
import org.sokybot.social.api.SocialChannel;

import reactor.core.publisher.Sinks;

class MachineSocialState {
    
    final EnumMap<SocialChannel, Deque<ChatLine>> ring = new EnumMap<>(SocialChannel.class);
    final Deque<SocialAlert> alerts = new ArrayDeque<>(64);
    
    volatile long lastGmSeenAtMs;
    
    final Sinks.Many<ChatLine> chatSink = Sinks.many().multicast().onBackpressureBuffer(256, false);
    final Sinks.Many<SocialAlert> alertSink = Sinks.many().multicast().onBackpressureBuffer(64, false);
    final Sinks.Many<ISocialSnapshot> snapshotSink = Sinks.many().multicast().onBackpressureBuffer(64, false);
    
    MachineSocialState() {
        for (SocialChannel ch : SocialChannel.values()) {
            ring.put(ch, new ArrayDeque<>(256));
        }
    }
}
