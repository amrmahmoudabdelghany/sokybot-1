package org.sokybot.engine.test.util.mocks;

import org.mockito.Mockito;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mock implementation of IGameModel for testing.
 * Provides configurable trainer and spawn data.
 */
public class MockGameModel implements IGameModel {
    
    private ITrainer trainer;
    private final Map<Integer, ISpawn> spawns = new HashMap<>();
    private ISpawn selected;
    
    /**
     * Creates a new MockGameModel.
     */
    public MockGameModel() {
    }
    
    /**
     * Sets the trainer for this game model.
     */
    public MockGameModel withTrainer(ITrainer trainer) {
        this.trainer = trainer;
        return this;
    }
    
    /**
     * Creates a mock trainer with the given unique ID.
     */
    public MockGameModel withMockTrainer(int uniqueId) {
        ITrainer mockTrainer = Mockito.mock(ITrainer.class);
        Mockito.when(mockTrainer.getUniqueId()).thenReturn(uniqueId);
        this.trainer = mockTrainer;
        return this;
    }
    
    /**
     * Adds a spawn to the game model.
     */
    public MockGameModel addSpawn(ISpawn spawn) {
        spawns.put(spawn.getUniqueId(), spawn);
        return this;
    }
    
    /**
     * Adds a mock monster with the given unique ID.
     */
    public MockGameModel addMockMonster(int uniqueId) {
        IMonster monster = Mockito.mock(IMonster.class);
        Mockito.when(monster.getUniqueId()).thenReturn(uniqueId);
        spawns.put(uniqueId, monster);
        return this;
    }
    
    /**
     * Sets the selected spawn.
     */
    public MockGameModel setSelected(ISpawn spawn) {
        this.selected = spawn;
        return this;
    }
    
    @Override
    public Optional<ISpawn> find(int id) {
        return Optional.ofNullable(spawns.get(id));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ISpawn> Map<Integer, T> findAll(Class<T> type) {
        Map<Integer, T> result = new HashMap<>();
        for (Map.Entry<Integer, ISpawn> entry : spawns.entrySet()) {
            if (type.isInstance(entry.getValue())) {
                result.put(entry.getKey(), (T) entry.getValue());
            }
        }
        return result;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ISpawn> Optional<T> find(int id, Class<T> type) {
        ISpawn spawn = spawns.get(id);
        if (spawn != null && type.isInstance(spawn)) {
            return Optional.of((T) spawn);
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<ISpawn> getSelected() {
        return Optional.ofNullable(selected);
    }
    
    @Override
    public ITrainer getTrainer() {
        return trainer;
    }
}
