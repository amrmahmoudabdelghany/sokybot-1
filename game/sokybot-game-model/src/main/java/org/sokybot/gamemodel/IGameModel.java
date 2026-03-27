package org.sokybot.gamemodel;

import java.util.Map;
import java.util.Optional;
import reactor.core.publisher.Flux;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;

public interface IGameModel {

    Optional<ISpawn> find(int id);

    <T extends ISpawn> Map<Integer, T> findAll(Class<T> type);

    <T extends ISpawn> Optional<T> find(int id, Class<T> type);

    Optional<ISpawn> getSelected();

    ITrainer getTrainer();

    LoginState getLoginState();

    <T extends ISpawn> Flux<T> observe(int id, Class<T> type);

    <T extends ISpawn> Flux<ModelUpdate<T>> observeAll(Class<T> type);
}
