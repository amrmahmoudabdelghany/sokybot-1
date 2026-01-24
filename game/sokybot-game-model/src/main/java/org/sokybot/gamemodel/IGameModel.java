package org.sokybot.gamemodel;

import java.util.Map;
import java.util.Optional;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;

public interface IGameModel {

    Optional<ISpawn> find(int id);
    
    <T extends ISpawn> Map<Integer, T> findAll(Class<T> type);
    
    <T extends ISpawn> Optional<T> find(int id, Class<T> type);
    
    Optional<ISpawn> getSelected();
    
    ITrainer getTrainer();
}
