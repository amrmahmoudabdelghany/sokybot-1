package org.sokybot.gamemodel;

import java.util.List;
import java.util.Optional;

import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;

public interface IGameModelQuery {

    Optional<ISpawn> find(int id);

    <T extends ISpawn> Optional<T> findLive(int id, Class<T> type);

    <T extends ISpawn> Optional<T> snapshot(int id, Class<T> type);

    <T extends ISpawn> List<T> snapshotAll(Class<T> type);

    Optional<ISpawn> getSelected();

    ITrainer getTrainer();

    LoginState getLoginState();
}
