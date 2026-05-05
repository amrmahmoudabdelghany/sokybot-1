package org.sokybot.grid.api;

import java.util.List;

import reactor.core.publisher.Mono;

/**
 * Epic #25: asynchronously computes spatial formation assignment via Timefold.
 */
public interface IGridSolver {

    Mono<GridPlan> calculateOptimalFormation(List<GridBotDto> bots, List<GridNodeDto> nodes);
}
