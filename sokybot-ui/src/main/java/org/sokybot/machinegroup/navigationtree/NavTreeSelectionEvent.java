package org.sokybot.machinegroup.navigationtree;

import java.util.EventObject;
import lombok.Getter;

@Getter
public class NavTreeSelectionEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	private String selectedPath;

	public NavTreeSelectionEvent(Object source, String selectedPath) {
		super(source);
		this.selectedPath = selectedPath;
	}
	
	public String getSelectedPath() {
		return selectedPath;
	}
}
