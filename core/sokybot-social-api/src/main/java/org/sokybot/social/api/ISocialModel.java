package org.sokybot.social.api;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import reactor.core.publisher.Flux;

public interface ISocialModel {
    Optional<ISocialSnapshot> snapshot(String machineId);
    Flux<ChatLine> observeChat(String machineId);
    Flux<SocialAlert> observeAlerts(String machineId);
    Flux<ISocialSnapshot> observe(String machineId);

    /**
     * Live chat lines merged across all machines. Default: empty until overridden by the projection.
     */
    default Flux<ChatLine> observeAllChat() {
        return Flux.empty();
    }

    /**
     * Social alerts merged across all machines. Default: empty until overridden by the projection.
     */
    default Flux<SocialAlert> observeAllAlerts() {
        return Flux.empty();
    }

    /**
     * Machine identifiers that currently have social projection state. Default: none.
     */
    default Set<String> knownMachineIds() {
        return Collections.emptySet();
    }
}
