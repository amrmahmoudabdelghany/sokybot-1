package org.sokybot.machine.workflow.actions;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.service.IAttackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
public class AttackingAction implements Action<MachineState, IMachineEvent> {

    @Autowired
    private IAttackingService attackingService;

    @Override
    public void execute(StateContext<MachineState, IMachineEvent> context) {
        attackingService.attack();
    }
}
