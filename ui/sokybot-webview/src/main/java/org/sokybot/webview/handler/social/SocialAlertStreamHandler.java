package org.sokybot.webview.handler.social;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.social.api.ISocialModel;
import org.sokybot.social.api.SocialAlert;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.dto.social.SocialAlertDto;

import reactor.core.publisher.Flux;

/**
 * Stream: {@code social.alerts} — social alerts with optional machine and kind filters.
 */
@Component(service = IRSocketStreamHandler.class, property = IRSocketStreamHandler.STREAM_PROPERTY + "=social.alerts")
public class SocialAlertStreamHandler implements IRSocketStreamHandler {

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
        return "social.alerts";
    }

    @Override
    public String getDescription() {
        return "Social alerts (per-machine or all machines) with optional kind filter";
    }

    @Override
    public Flux<Object> handleStream(RSocketRequest request) {
        ISocialModel m = socialModel;
        if (m == null) {
            return Flux.empty();
        }

        String machineId = strParam(request, "machineId");
        Set<SocialAlert.Kind> kinds = parseKinds(request);

        Flux<SocialAlert> base = (machineId == null || machineId.isEmpty())
                ? m.observeAllAlerts()
                : m.observeAlerts(machineId);

        return base
                .filter(a -> kinds.isEmpty() || kinds.contains(a.getKind()))
                .map(this::toDto)
                .cast(Object.class);
    }

    private static String strParam(RSocketRequest request, String key) {
        String v = request.getString(key);
        return v != null ? v.trim() : null;
    }

    private static Set<SocialAlert.Kind> parseKinds(RSocketRequest request) {
        String raw = request.getString("kinds");
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        EnumSet<SocialAlert.Kind> out = EnumSet.noneOf(SocialAlert.Kind.class);
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                out.add(SocialAlert.Kind.valueOf(t));
            } catch (IllegalArgumentException ex) {
                // skip unknown kind names
            }
        }
        return out.isEmpty() ? Collections.emptySet() : out;
    }

    private SocialAlertDto toDto(SocialAlert a) {
        return new SocialAlertDto(
                a.getMachineId(),
                a.getTimestampEpochMs(),
                a.getKind() != null ? a.getKind().name() : null,
                a.getSubject(),
                a.getAttributes());
    }
}
