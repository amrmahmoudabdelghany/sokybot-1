package org.sokybot.social.api;

import java.util.Optional;
import reactor.core.publisher.Flux;

public interface ISocialModel {
    Optional<ISocialSnapshot> snapshot(String machineId);
    Flux<ChatLine> observeChat(String machineId);
    Flux<SocialAlert> observeAlerts(String machineId);
    Flux<ISocialSnapshot> observe(String machineId);
}
