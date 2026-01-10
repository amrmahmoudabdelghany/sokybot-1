package org.sokybot.machine.gamemodel;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.event.DespawnEvent;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machinegroup.gamemodel.ISpawnable;
import org.sokybot.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Component;

@Component
@WithStateMachine
public class GameModel implements IMutableGameModel {

	private Map<Integer, ISpawnable> objs = new HashMap<>();
	private final Set<ISpawnListener> spawnListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	
	private final Trainer trainer = new Trainer();

	private int selectedId = -1;
	
	@Autowired
	Logger log  ;

	@Override
	public Optional<ISpawnable> find(int id) {
		return find(id, ISpawnable.class);
	}
 
	
	@Override
	public <T extends ISpawnable> Map<Integer, T> findAll(Class<T> type) {
	 
		Map<Integer, T> res = new HashMap<>() ; 
		
		this.objs.values()
		.stream()
		.filter((s)->type.isInstance(s)) 
		.forEach((v)->{
			res.put(v.getUniqueId(), type.cast(v)) ;
		});
		
		return res;
	}
	
	@Override
	public <T extends ISpawnable> Optional<T> find(int id, Class<T> type) {
		T res = null;

		ISpawnable obj = null;

		if (id == this.trainer.getUniqueId()) {
			obj = trainer;
		} else {
			obj = this.objs.get(id);
		}

		if (type.isInstance(obj)) {
			res = type.cast(obj);
		}

		return Optional.ofNullable(res);
	}

	

	
	
//	@EventListener
//	public void onMonsterSpawn(MonsterSpawnEvent event) {
//
//		System.out.println("Monster added to game model " + event.getMonster().getUniqueId()) ; 
//		add(event.getMonster());
//
//	}
//	

	
// 
//	@EventListener
//	public void onDespawn(DespawnEvent event) {
//		remove(event.getUniqueId());
//
//	}
	
	
	@StateEntry(target = MachineState.READY)
	public void onDisconnect() { 
		this.objs.keySet()
		.forEach((key)->remove(key));
	}
	
	@Override
	public void add(ISpawnable obj) {
		Objects.requireNonNull(obj, "Spawn object could not be null");
		this.objs.put(obj.getUniqueId(), obj);
		notifyAdd(obj);
	}

	@Override
	public ISpawnable remove(int id) {
		ISpawnable removedObj = this.objs.remove(id);

		if (id == selectedId)
			selectedId = -1;

		notifyRemove(removedObj);
		return removedObj;
	}

	@Override
	public void setSelectedSpawn(int id) {

		this.selectedId = id;

		getSelected().ifPresent((spawn) -> notifySpawnSelected(spawn));
	}

	@Override
	public Optional<ISpawnable> getSelected() {

		return find(selectedId);
	}

	@Override
	public void addSpawnListener(ISpawnListener spawnListener) {
		if (spawnListener == null)
			return;
		this.spawnListeners.add(spawnListener);
	}

	@Override
	public void removeSpawnListener(ISpawnListener spawnListener) {

		if (spawnListener == null)
			return;
		this.spawnListeners.remove(spawnListener);

	}

	private void notifyAdd(ISpawnable spawnObject) {

		for (ISpawnListener listener : this.spawnListeners) {
			listener.spawnAdded(spawnObject);
		}
	}

	private void notifyRemove(ISpawnable spawnObject) {
		for (ISpawnListener listener : this.spawnListeners) {
			listener.spawnRemoved(spawnObject);
		}
	}

	private void notifySpawnSelected(ISpawnable spawnObject) {
		for (ISpawnListener listener : spawnListeners) {
			listener.spawnSelected(spawnObject);
		}
	}

	@Override
	@Bean
	public Trainer getTrainer() {

		return this.trainer;
	}

	
}
