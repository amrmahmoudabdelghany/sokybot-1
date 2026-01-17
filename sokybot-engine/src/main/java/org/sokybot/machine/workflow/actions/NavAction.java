package org.sokybot.machine.workflow.actions;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
public class NavAction implements Action<MachineState, IMachineEvent> {

    @Autowired
    private org.sokybot.machine.service.IMovingService movingService;

    @Override
    public void execute(StateContext<MachineState, IMachineEvent> context) {
        movingService.move();
    }
}
