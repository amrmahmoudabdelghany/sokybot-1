package org.sokybot.machinegroup.navigationtree;

import org.springframework.context.ApplicationEvent;

public class NavTreeSelectionEvent extends ApplicationEvent {

	private String selectionPath ; 
	
	public NavTreeSelectionEvent(Object source , String path ) {
	 super(source);
	 this.selectionPath = path ; 
	}
	
	public String getSelectedPath() { 
		return this.selectionPath ; 
	}
}

