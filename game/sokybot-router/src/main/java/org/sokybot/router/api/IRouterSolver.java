package org.sokybot.router.api;

import java.util.List;

import reactor.core.publisher.Mono;

/**
 * Epic #24: asynchronously computes field logistics (mule + item assignment) via Timefold.
 */
public interface IRouterSolver {

    Mono<RouterPlan> calculateFieldLogistics(List<LogisticsBotDto> bots, List<LogisticsItemDto> items);
}
