package org.sokybot.machine.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import javax.annotation.PostConstruct;


import org.slf4j.Logger;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateChanged;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.event.DespawnEvent;
import org.sokybot.machine.event.SpawnReachDestinationEvent;
import org.sokybot.machine.event.monsterevent.MonsterDespawnEvent;
import org.sokybot.machine.event.monsterevent.MonsterHPUpdateEvent;
import org.sokybot.machine.event.monsterevent.MonsterSelectedEvent;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machine.event.trainerevent.TrainerAttackedEvent;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.machine.event.userevent.UserConfigUpdatedEvent;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.settings.MonsterPreference;
import org.sokybot.settings.Settings;
import org.sokybot.settings.TrainingAreaSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MonsterTargetService implements IMonsterTargetService {

	// private Map<Integer, IMonster> monsters = new HashMap<>();

	private Map<Integer, IMonster> outAreaMonsters = new HashMap<>();

	private PriorityQueue<IMonster> monsterQueue = new PriorityQueue<>(new MonsterComparator());

	private Set<Integer> avoidedMonsters = new HashSet<>();

	private Set<Integer> attackerMonsters = new HashSet<>();

	@Autowired
	private IGameModel gameModel;

	@Autowired
	private Settings settings;

	@Autowired
	private TrainingAreaSettings trainingAreaSettings;

	@Autowired
	private ITrainerManager trainerManager;

	@Autowired
	private ITrainer trainer;

	@Autowired
	private Logger log;

	private IMonster selectedMonster;
 
	@PostConstruct
	private void init() { 
	  // this.gameModel.addSpawnListener(this); // Removed
	}
	public void targetNextMonster() {

		if (this.monsterQueue.isEmpty()) {
			throw new IllegalStateException("No monsters exists to target");
		}

		if (this.selectedMonster != null && this.selectedMonster.isAlive()) {
			this.monsterQueue.add(selectedMonster);
		}

		IMonster nextMonster = this.monsterQueue.poll();
		this.trainerManager.select(nextMonster.getUniqueId());
	}

	@StateChanged(source = MachineState.IDLE, target = MachineState.PLAYING)
	public void onStartTraining(TrainerLoadedEvent event) {
		update();
	}

	@Override
	public boolean isSelectLiveMonster() {

		if (this.selectedMonster == null)
			return false;

		return this.selectedMonster.getCurrentHP() > 0;

	}

	@Override
	public int getAreaMonsterCount() {
		return this.monsterQueue.size();
	}

	private void traceMonster(IMonster m) {
		if (this.settings.getMonsterPreference(m.getMonsterType(), m.getName()) != org.sokybot.settings.MonsterPreference.AVOID) {
			int dis = (int) m.distance(this.trainingAreaSettings.getActiveArea().getAreaX(),
					this.trainingAreaSettings.getActiveArea().getAreaY());

			if (dis <= this.trainingAreaSettings.getActiveArea().getAreaR()) {
				this.monsterQueue.add(m);
			} else {
				this.outAreaMonsters.put(m.getUniqueId(), m);
			}
		} else {
			this.avoidedMonsters.add(m.getUniqueId());
		}
	}

	private void update() {
		this.outAreaMonsters.clear();
		this.monsterQueue.clear();
		this.avoidedMonsters.clear();
		this.gameModel.findAll(IMonster.class).forEach((id, m) -> {
			traceMonster(m);
		});
//		this.monsters.forEach((id, m) -> {
//			traceMonster(m);
//		});

	}

	@EventListener(condition = "#event.eventType == 'SETAREAX' or #event.eventType == 'SETAREAY' or #event.eventType == 'SETAREAR' or #event.eventType == 'SETMONSTERPREFERENCE'")
	public void onUserSettingsChanged(UserConfigUpdatedEvent event) {

		update();

	}



//	@EventListener
//	public void onMonsterSpawn(MonsterSpawnEvent event) {
//
//		//Monster m = event.getMonster();
//		//this.monsters.put(m.getUniqueId(), m);
//
//		traceMonster(event.getMonster());
//
//	}

	private void removeMonster(IMonster monster) {
		// Monster monster = this.monsters.remove(uniqueId);
		int uniqueId = monster.getUniqueId();

		if (monster != null) {
			this.monsterQueue.remove(monster);
			this.outAreaMonsters.remove(uniqueId);
			this.avoidedMonsters.remove(uniqueId);
			this.attackerMonsters.remove(uniqueId);
		}

		if (this.selectedMonster != null && this.selectedMonster.getUniqueId() == uniqueId) {
			this.selectedMonster = null;
		}

	}

	private boolean isIgnoredMonster(int id) {
		return (this.outAreaMonsters.containsKey(id) || this.avoidedMonsters.contains(id));
	}

	@EventListener
	public void onTrainerAttackedEvent(TrainerAttackedEvent event) {
		int caster = event.getCasterId();

		this.gameModel.find(caster, IMonster.class).ifPresent((m) -> {
			this.attackerMonsters.add(caster);

			if (this.settings.isPreferAttackerMonster() && isIgnoredMonster(caster)) {
				this.monsterQueue.add(m);
			}
		});

	}

	private MonsterPreference getMonsterPreference(IMonster m) {
		int id = m.getUniqueId();
		if (this.settings.isPreferAttackerMonster() && this.attackerMonsters.contains(id)) {
			return MonsterPreference.PREFER;
		}
		return this.settings.getMonsterPreference(m.getName());
	}

	@EventListener
	public void onSpawnReachDistination(SpawnReachDestinationEvent event) {

		if (this.outAreaMonsters.containsKey(event.getUniqueId())) {
			IMonster m = this.outAreaMonsters.get(event.getUniqueId());

			if (this.trainingAreaSettings.getActiveArea().contains(m.getDestX(), m.getDestY())) {
				m = this.outAreaMonsters.remove(event.getUniqueId());
				this.monsterQueue.add(m);
			}
//			int dis = (int) m.distance(this.trainingAreaSettings.getActiveArea().getAreaX(),
//					this.trainingAreaSettings.getActiveArea().getAreaY());
//
//			if (dis <= this.trainingAreaSettings.getActiveArea().getAreaR()) {
//			
//			}

			// Monster m = this.outAreaMonsters.remove(event.getUniqueId()) ;

		}
	}

	@EventListener
	public void onMonsterHPUpdate(MonsterHPUpdateEvent event) {
		if (event.isDead()) {

			removeMonster(event.getMonster());
		}

	}


//	@EventListener
//	public void onMonsterSelected(MonsterSelectedEvent event) {
//		this.selectedMonster = event.getSelectedMonster();
//
//	}

//	@EventListener
//	public void onMonsterDespawn(MonsterDespawnEvent event) {
//
//		removeMonster(event.getMonster());
//
//	}



	private final class MonsterComparator implements Comparator<IMonster> {

		@Override
		public int compare(IMonster o1, IMonster o2) {
			if (o1 == null || o2 == null)
				return 0;

			MonsterPreference p1 = getMonsterPreference(o1);
			MonsterPreference p2 = getMonsterPreference(o2);
			if (p1 == p2) {
				int dis1 = (int) o1.distance(trainer.getX(), trainer.getY());
				int dis2 = (int) o2.distance(trainer.getX(), trainer.getY());

				return Integer.compare(dis1, dis2);
			} else {
				if (p1 == MonsterPreference.PREFER)
					return -1;
				else
					return 1;
			}
		}
	}

}
