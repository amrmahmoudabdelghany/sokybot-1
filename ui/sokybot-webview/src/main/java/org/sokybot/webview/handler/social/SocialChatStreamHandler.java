package org.sokybot.webview.handler.social;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.social.api.ChatLine;
import org.sokybot.social.api.ISocialModel;
import org.sokybot.social.api.SocialChannel;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.dto.social.ChatLineDto;

import reactor.core.publisher.Flux;

/**
 * Stream: {@code social.chat} — live chat lines with server-side filtering.
 */
@Component(service = IRSocketStreamHandler.class, property = IRSocketStreamHandler.STREAM_PROPERTY + "=social.chat")
public class SocialChatStreamHandler implements IRSocketStreamHandler {

    private volatile ISocialModel socialModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void bindSocialModel(ISocialModel model) {
        this.socialModel = model;
    }

    protected void unbindSocialModel(ISocialModel model) {
        if (this.socialModel == model) {
            this.socialModel = null;
        }
    }

    @Override
    public String getStreamName() {
        return "social.chat";
    }

    @Override
    public String getDescription() {
        return "Live chat lines (per-machine or all machines) with optional channel / GM / self filters";
    }

    @Override
    public Flux<Object> handleStream(RSocketRequest request) {
        ISocialModel m = socialModel;
        if (m == null) {
            return Flux.empty();
        }

        String machineId = strParam(request, "machineId");
        Set<SocialChannel> channels = parseChannels(request);
        // Defaults: includeSelf=false, includeGm=true (RSocketRequest treats absent keys as false).
        boolean includeSelf =
                request.getParams().containsKey("includeSelf") && request.getBoolean("includeSelf");
        boolean includeGm =
                !request.getParams().containsKey("includeGm") || request.getBoolean("includeGm");

        Flux<ChatLine> base = (machineId == null || machineId.isEmpty())
                ? m.observeAllChat()
                : m.observeChat(machineId);

        return base
                .filter(c -> channels.isEmpty() || channels.contains(c.getChannel()))
                .filter(c -> includeSelf || !c.isFromSelf())
                .filter(c -> includeGm || !c.isFromGameMaster())
                .map(this::toDto)
                .cast(Object.class);
    }

    private static String strParam(RSocketRequest request, String key) {
        String v = request.getString(key);
        return v != null ? v.trim() : null;
    }

    private static Set<SocialChannel> parseChannels(RSocketRequest request) {
        Object raw = request.getParams().get("channels");
        if (raw == null) {
            return Collections.emptySet();
        }
        if (raw instanceof Collection<?>) {
            EnumSet<SocialChannel> out = EnumSet.noneOf(SocialChannel.class);
            for (Object o : (Collection<?>) raw) {
                if (o == null) {
                    continue;
                }
                try {
                    out.add(SocialChannel.valueOf(o.toString().trim()));
                } catch (IllegalArgumentException ex) {
                    // ignore unknown channel names
                }
            }
            return out.isEmpty() ? Collections.emptySet() : out;
        }
        return Collections.emptySet();
    }

    private ChatLineDto toDto(ChatLine c) {
        return new ChatLineDto(
                c.getMachineId(),
                c.getTimestampEpochMs(),
                c.getChannel() != null ? c.getChannel().name() : null,
                c.getSenderName(),
                c.getMessage(),
                c.isFromGameMaster(),
                c.isFromSelf());
    }
}
