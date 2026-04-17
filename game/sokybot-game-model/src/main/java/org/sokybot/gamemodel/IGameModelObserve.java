package org.sokybot.gamemodel;

import org.sokybot.gamemodel.model.ISpawn;

import reactor.core.publisher.Flux;

public interface IGameModelObserve {

    <T extends ISpawn> Flux<T> observe(int id, Class<T> type);

    <T extends ISpawn> Flux<ModelUpdate<T>> observeAll(Class<T> type);
}
