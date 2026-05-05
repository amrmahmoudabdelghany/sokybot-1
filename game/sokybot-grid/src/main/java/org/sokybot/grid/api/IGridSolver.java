package org.sokybot.grid.api;

import java.util.List;

import org.sokybot.grid.domain.GridBotEntity;
import org.sokybot.grid.domain.GridNode;
import org.sokybot.grid.domain.GridSolution;

import reactor.core.publisher.Mono;

/**
 * Epic #25: asynchronously computes spatial formation assignment via Timefold.
 */
public interface IGridSolver {

    Mono<GridSolution> calculateOptimalFormation(List<GridBotEntity> bots, List<GridNode> nodes);
}
