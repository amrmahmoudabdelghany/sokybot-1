package org.sokybot.machine.actuator;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.service.ITrainerManager;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class PickingActuator implements Guard<MachineState, IMachineEvent> , Action<MachineState, IMachineEvent> {

	private Settings config ; 
	private ITrainerManager trainerManager ; 
	
	
	
	
	@Override
	public boolean evaluate(StateContext<MachineState, IMachineEvent> context) {
		
	//	System.out.println("Evaluate to picking") ; 
		return true;
	}

	@Override
	public void execute(StateContext<MachineState, IMachineEvent> context) {
	  System.out.println("Picking....") ; 
		
	}
	
	
}
