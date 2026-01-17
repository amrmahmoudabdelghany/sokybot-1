package org.sokybot.machine.workflow.guards;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
public class TargetingGuard implements Guard<MachineState, IMachineEvent> {

    @Autowired
    private org.sokybot.machine.service.IMonsterTargetService targetService;

    @Override
    public boolean evaluate(StateContext<MachineState, IMachineEvent> context) {
        return !targetService.isSelectLiveMonster() && targetService.getAreaMonsterCount() > 0;
    }
}
