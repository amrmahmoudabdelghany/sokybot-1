package org.sokybot.machine.event.trainerevent;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TrainerAttackedEvent {

	private int casterId ; 
	private int skillId ; 
	
}
