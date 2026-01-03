package org.sokybot.machine.actuator;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.service.IMonsterTargetService;
import org.sokybot.machine.service.IMovingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

//@Component
public class MovingActuator implements Guard<MachineState, IMachineEvent>, Action<MachineState, IMachineEvent> {

	

	
	
	
	@Autowired
	private IMovingService trainingService ; 
	
	@Autowired
	private IMonsterTargetService monsterTargetService ; 
	
	
	
	@Override
	public boolean evaluate(StateContext<MachineState, IMachineEvent> context) {
		
//		if(monsterTargetService.getAreaMonsterCount() == 0 && !trainingService.isStillMoving() ) { 
//			 
//			return false ; 
//		}
//	
//		return true ; 
		//return gameModel.getAreaMonsterCount() > 0 && (System.currentTimeMillis() < lastMoveTime);
	
		return this.trainingService.isReachDestination() ; 
	}

	
	@Override
	public void execute(StateContext<MachineState, IMachineEvent> context) {
	 
		
		this.trainingService.move();
		
	}
}
