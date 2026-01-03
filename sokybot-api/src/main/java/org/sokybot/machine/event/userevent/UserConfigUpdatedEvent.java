package org.sokybot.machine.event.userevent;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class UserConfigUpdatedEvent {

	
	private Object source ; 
	private String eventType ; 
	
	public UserConfigUpdatedEvent(Object source , String eventType ) {
	   this.source = source ; 
	   this.eventType = eventType ; 
	}
}
