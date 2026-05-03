package org.sokybot.warroom.api;

import java.util.List;

import org.sokybot.warroom.domain.PartyRosterSolution;
import org.sokybot.warroom.domain.SwarmBotEntity;

import reactor.core.publisher.Mono;

/**
 * Epic #23: asynchronously computes an optimal party roster via Timefold.
 */
public interface IWarRoomSolver {

    Mono<PartyRosterSolution> calculateOptimalRoster(List<SwarmBotEntity> availableBots, int numberOfParties);
}
