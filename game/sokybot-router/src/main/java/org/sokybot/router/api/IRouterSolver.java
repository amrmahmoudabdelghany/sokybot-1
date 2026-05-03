package org.sokybot.router.api;

import java.util.List;

import org.sokybot.router.domain.FieldItemEntity;
import org.sokybot.router.domain.RouterSolution;
import org.sokybot.router.domain.SwarmBotEntity;

import reactor.core.publisher.Mono;

/**
 * Epic #24: asynchronously computes field logistics (mule + item assignment) via Timefold.
 */
public interface IRouterSolver {

    Mono<RouterSolution> calculateFieldLogistics(List<SwarmBotEntity> bots, List<FieldItemEntity> items);
}
