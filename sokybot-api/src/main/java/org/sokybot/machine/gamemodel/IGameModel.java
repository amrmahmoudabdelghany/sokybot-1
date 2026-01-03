package org.sokybot.machine.gamemodel;

import java.util.Map;
import java.util.Optional;

import org.sokybot.machinegroup.gamemodel.ISpawnable;

public interface IGameModel {

	
	// adding spawn object
	// removing spawn object  
	//find spawn ob 
	
	
	Optional<ISpawnable> find(int id ) ; 
	
	<T extends ISpawnable> Map<Integer , T> findAll(Class<T> type) ; 
	
	<T extends ISpawnable> Optional<T> find(int id ,Class<T> type ) ; 
	
	Optional<ISpawnable> getSelected() ; 
	
	
	Trainer getTrainer() ; 
	
	public void addSpawnListener(ISpawnListener spawnListener) ; 
	public void removeSpawnListener(ISpawnListener spawnListener) ; 
	
}
