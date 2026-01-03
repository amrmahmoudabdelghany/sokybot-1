package org.sokybot.machine.actuator;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.service.IAttackingService;
import org.sokybot.machine.service.IMonsterTargetService;
import org.sokybot.machine.service.IMovingService;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;


@Component
public class AttackingActuator
		implements Guard<MachineState, IMachineEvent>, Action<MachineState, IMachineEvent> {


	@Autowired
	private Settings config;
	
	@Autowired
	private IAttackingService attackingService ; 
	
	@Autowired
	private IMonsterTargetService trainingService ;
	
	@Autowired
	Logger log ; 

	

	@Override
	public boolean evaluate(StateContext<MachineState, IMachineEvent> context) {

		if (config.isDoNotAttack())
			return true;

		// System.out.println("Evaluate to attack ") ;
			// return true ( don`t work ) if there no selected live monster
		return !this.trainingService.isSelectLiveMonster() ; 

	}

	@Override
	public void execute(StateContext<MachineState, IMachineEvent> context) {

		
		this.attackingService.attack();
		
	}

	
	
	
	


}
