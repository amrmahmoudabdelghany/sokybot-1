package org.sokybot.machine.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class DespawnEvent extends ApplicationEvent {

	private int uniqueId ; 
	
	public DespawnEvent(Object source , int uniqueId) {
		super(source) ; 
		this.uniqueId = uniqueId ; 
	}
}
