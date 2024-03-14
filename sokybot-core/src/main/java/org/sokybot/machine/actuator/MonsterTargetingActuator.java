package org.sokybot.machine.actuator;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.slf4j.Logger;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machine.event.trainerevent.TrainerStuckEvent;
import org.sokybot.machine.gamemodel.IGameModel;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machine.service.IAttackingService;
import org.sokybot.machine.service.IMonsterTargetService;
import org.sokybot.machine.service.ITrainerManager;
import org.sokybot.machine.service.IMovingService;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
public class MonsterTargetingActuator
		implements Guard<MachineState, IMachineEvent>, Action<MachineState, IMachineEvent> {

	private IGameModel gameModel;
	private Trainer trainer;
	private ITrainerManager trainerManager;
	
	private Settings userConfig;

	private boolean isTrainerStuck = false;

	@Autowired
	private IMonsterTargetService trainingService ; 
	
	
	@Autowired
	Logger log;


	public MonsterTargetingActuator(IGameModel gameModel, ITrainerManager trainerManager, Settings config) {

		this.trainerManager = trainerManager;
		this.userConfig = config;
		this.gameModel = gameModel;
		this.trainer = gameModel.getTrainer();
	}

	@Override
	public boolean evaluate(StateContext<MachineState, IMachineEvent> context) {
 
		// t t  select live monster and no other exists  ----- return true 
		// f t  no selection and no other exists    ---- return true 
		// t f  select live monster and there exists other --- return true 
		// f f no selection and there exists other
		
		if(this.trainingService.isSelectLiveMonster()) { 
			
			return true ; 
		}
		
		if(this.trainingService.getAreaMonsterCount() == 0) { 
			
			return true ; 
			
		}
		
		 
		return false ; 
		
	}

	@EventListener
	public void onTrainerStuck(TrainerStuckEvent event) {
		this.isTrainerStuck = true;

	}

	

	@Override
	public void execute(StateContext<MachineState, IMachineEvent> context) {
 
		trainingService.targetNextMonster();

	}



}
