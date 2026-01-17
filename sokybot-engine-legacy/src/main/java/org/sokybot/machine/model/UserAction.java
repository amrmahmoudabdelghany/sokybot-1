package org.sokybot.machine.model;

import org.sokybot.machine.IMachineEvent;

public enum UserAction  implements IMachineEvent{

	CONFIG_MODIFIED ,
	CONFIG_COMMIT , 
	DISCONNECT , 
	CONNECT , 
	KILL_CLIENT  ,
	CLIENT_ATTACHED,
	START_TRAINING , 
	STOP_TRAINING 
	
}
