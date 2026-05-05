package org.sokybot.warroom.api;

import java.util.List;

import reactor.core.publisher.Mono;

/**
 * Epic #23: asynchronously computes an optimal party roster via Timefold.
 */
public interface IWarRoomSolver {

    Mono<WarRoomPlan> calculateOptimalRoster(
            List<SwarmBotDto> availableBots,
            int numberOfParties,
            WarRoomPlan previousPlan);
}
