package org.sokybot.machine.service;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.event.DespawnEvent;
import org.sokybot.machine.event.SpawnReachDestinationEvent;
import org.sokybot.machine.event.monsterevent.MonsterHPUpdateEvent;
import org.sokybot.machine.event.monsterevent.MonsterSelectedEvent;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.machine.event.trainerevent.TrainerReachDestinationEvent;
import org.sokybot.machine.event.trainerevent.TrainerStuckEvent;
import org.sokybot.machine.event.userevent.UserConfigUpdatedEvent;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.geo.Vector2D;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.setting.MonsterPreference;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.gamemodel.setting.TrainingArea;
import org.sokybot.machinegroup.gamemodel.setting.TrainingAreaSettings;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Service;

@Service
@WithStateMachine
public class ActiveAreaDriverService implements IMovingService {

	@Autowired
	private Trainer trainer;

	@Autowired
	private ITrainerManager trainerManager;

	@Autowired
	private TrainingAreaSettings areaSettings;

	@Autowired
	private ISroMaterialDAO dao;

	@Autowired
	private Logger log;

	private List<Vector2D> path = null;


	@EventListener
	public void onTrainerLoaded(TrainerLoadedEvent ev) {

		if (path != null)
			this.path.clear();
	}
	
	@EventListener(condition = "#event.eventType == 'SETACTIVEAREA'")
	public  void onActiveAreaChanged(UserConfigUpdatedEvent event) { 
		if(path != null) { 
			this.path.clear();  
		}
		
		TrainingArea activeArea = this.areaSettings.getActiveArea();
		int targetX = activeArea.getAreaX() ; 
		int targetY = activeArea.getAreaY() ; 
		
		this.path = this.dao.findPath(this.trainer.getX(), this.trainer.getY(), targetX,
				targetY);
		this.path.add(new Vector2D(targetX, targetY)) ; 
	}

	@StateEntry(target = MachineState.IDLE)
	public void onStopTraining() {
		if (path != null)
			this.path.clear();
	}

	@Override
	public boolean isReachDestination() {

		return this.areaSettings.getActiveArea().contains(this.trainer.getX(), this.trainer.getY()) ; 
	}

//	private void rndMove() {
//		
//		
//		Point rndPoint = this.areaSettings.getActiveArea().getRndPoint() ; 
//
//		lastMoveTime  = System.currentTimeMillis() +
//				Math.round(this.trainer.distance(rndPoint.x, rndPoint.y) / 
//						(this.trainer.getRunSpeed() * 0.1)) * 1000;
//		 
//		this.trainerManager.walk(rndPoint.x, rndPoint.y);
//		
//	}

//	@EventListener
//	public void onTrainerStuck(TrainerStuckEvent event) { 
//	   this.lastMoveTime = System.currentTimeMillis() ; 
//	   
//	}

	@Override
	public void move() {

		if (this.path == null || this.path.size() == 0) {

			TrainingArea activeArea = this.areaSettings.getActiveArea();
			int targetX = activeArea.getAreaX() ; 
			int targetY = activeArea.getAreaY() ; 
			
			this.path = this.dao.findPath(this.trainer.getX(), this.trainer.getY(), targetX,
					targetY);
			this.path.add(new Vector2D(targetX, targetY)) ; 
		}

		this.trainerManager.walk((int) this.path.get(0).x, (int) this.path.get(0).y);
	}

//	@Override
//	public void move(int x, int y) {
//	 
//		this.path = this.dao.findPath(this.trainer.getX(), this.trainer.getY(), x, y); 
//		this.path.add(new Vector2D(x, y)) ; 
//		System.out.println("Walking to " + x + " , " + y) ; 
//	}

	@Override
	public boolean isStillMoving() {
		return this.path != null && this.path.size() > 0;
	}

	@EventListener
	public void onTrainerReachDistination(TrainerReachDestinationEvent ev) {

		if (this.path != null && this.path.size() > 0) {
			// if(((int)ev.getX()) == ((int)this.path.get(0).x) && ((int)ev.getY() ==
			// ((int)this.path.get(0).y) )) {
			System.out.println("Trainer X " + ev.getX() + " , Y  " + ev.getY());
			System.out.println("Target X " + this.path.get(0).x + " Y " + this.path.get(0).y);
			if (Precision.equals(ev.getX(), this.path.get(0).x, 1f)
					&& Precision.equals(ev.getY(), this.path.get(0).y, 1f)) {
				System.out.println("Trainer Reach Destinaion : " + ev.getX() + " , " + ev.getY());
				this.path.remove(0);
			} else {
				System.out.println("Trainer could not  Reach Destinaion : " + ev.getX() + " , " + ev.getY());

			}

		}

	}

}
