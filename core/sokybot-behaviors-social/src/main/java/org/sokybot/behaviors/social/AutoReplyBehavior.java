package org.sokybot.behaviors.social;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.behaviors.social.net.SocialPackets;
import org.sokybot.behaviors.social.settings.SocialSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.social.api.ChatLine;
import org.sokybot.social.api.ISocialModel;
import org.sokybot.social.api.ISocialSnapshot;
import org.sokybot.social.api.SocialChannel;

import lombok.extern.slf4j.Slf4j;

@Component(service = IBehavior.class, property = { "order=5" }, scope = org.osgi.service.component.annotations.ServiceScope.PROTOTYPE)
@Slf4j
public class AutoReplyBehavior implements IBehavior<SocialSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISocialModel socialModel;

    // Track the last time we replied to each sender (transient per behavior instance)
    private final Map<String, Long> lastReplyTimes = new ConcurrentHashMap<>();
    private long lastGlobalReplyTime = 0;

    @Override
    public String id() {
        return "social-autoreply";
    }

    @Override
    public Class<SocialSettings> settingsType() {
        return SocialSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, SocialSettings settings) {
        if (!settings.isAutoReplyEnabled() || socialModel == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastGlobalReplyTime < settings.getAutoReplyCooldownMs()) {
            return false;
        }

        return socialModel.snapshot(context.getMachineId())
                .map(snap -> hasUnansweredPrivateMessages(snap, settings, now))
                .orElse(false);
    }

    private boolean hasUnansweredPrivateMessages(ISocialSnapshot snap, SocialSettings settings, long now) {
        for (ChatLine msg : snap.getChat().recent(SocialChannel.PRIVATE, 50)) {
            if (msg.getChannel() == SocialChannel.PRIVATE && !msg.isFromSelf() && !msg.isFromGameMaster()) {
                if (settings.getAutoReplyBlacklist() != null && settings.getAutoReplyBlacklist().contains(msg.getSenderName())) {
                    continue;
                }
                long lastReply = lastReplyTimes.getOrDefault(msg.getSenderName(), 0L);
                if (msg.getTimestampEpochMs() > lastReply && now - lastReply >= settings.getAutoReplyPerSenderCooldownMs()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, SocialSettings settings) {
        if (socialModel == null) return BehaviorStatus.SKIPPED;

        long now = System.currentTimeMillis();
        socialModel.snapshot(context.getMachineId()).ifPresent(snap -> {
            for (ChatLine msg : snap.getChat().recent(SocialChannel.PRIVATE, 50)) {
                if (!msg.isFromSelf() && !msg.isFromGameMaster()) {
                    if (settings.getAutoReplyBlacklist() != null && settings.getAutoReplyBlacklist().contains(msg.getSenderName())) {
                        continue;
                    }
                    long lastReply = lastReplyTimes.getOrDefault(msg.getSenderName(), 0L);
                    if (msg.getTimestampEpochMs() > lastReply && now - lastReply >= settings.getAutoReplyPerSenderCooldownMs()) {
                        
                        // Send the auto-reply
                        log.info("[{}] Sending auto-reply to {}", context.getMachineId(), msg.getSenderName());
                        context.getDispatcher().sendToServer(
                                SocialPackets.chatRequest(context.getMachineId(), (byte) 2, msg.getSenderName(), settings.getAutoReplyMessage()));
                        
                        lastReplyTimes.put(msg.getSenderName(), now);
                        lastGlobalReplyTime = now;
                        break; // Only reply to one person per cycle to avoid flooding
                    }
                }
            }
        });

        return BehaviorStatus.EXECUTED;
    }
}
