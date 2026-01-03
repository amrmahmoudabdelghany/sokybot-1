package org.sokybot.service;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;

public interface IBotStateController {

    MachineState getCurrentState();

    boolean sendEvent(IMachineEvent event);
    
    // We can use IMachineListener or a specific listener if needed.
    // For now, let's keep it simple.
}
