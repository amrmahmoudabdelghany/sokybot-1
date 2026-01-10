package org.sokybot.machine.actuator;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machine.service.IMovingService;
import org.sokybot.settings.TrainingArea;
import org.sokybot.settings.TrainingAreaSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
public class ActiveAreaNavActuator implements Guard<MachineState, IMachineEvent>, Action<MachineState, IMachineEvent> {

	

	
	
	@Autowired
	@Qualifier("activeAreaDriverService")
	private IMovingService walkingService ; 
	
	
	@Override
	public boolean evaluate(StateContext<MachineState, IMachineEvent> context) {
		
		return this.walkingService.isReachDestination() ; 
	//	return areaSettings.getActiveArea().contains(trainer.getX(), trainer.getY());
	}
	
	@Override
	public void execute(StateContext<MachineState, IMachineEvent> context) {
	 
		this.walkingService.move();
	}
}
