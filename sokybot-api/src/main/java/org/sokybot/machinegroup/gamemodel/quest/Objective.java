package org.sokybot.machinegroup.gamemodel.quest;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Objective {

	private byte id;
	private ObjectiveStatus status;
	private String name;
	private List<Integer> tasks = new ArrayList<Integer>();
	private byte achievementCount;

	public Objective(byte id) {
		this.id = id;
	}
	
	public void addTask(int taskId) { 
		this.tasks.add(taskId);
	}
	

}
