package org.sokybot.machine.event.trainerevent;

import org.sokybot.machine.gamemodel.Trainer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TrainerReachDestinationEvent {

	
	private Object source ; 
	
	private Trainer trainer ; 
	
	private double x ; 
	private double y ; 


}
