package org.sokybot.game.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sokybot.game.enums.QuestStatus;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Quest {

	@Setter(value = AccessLevel.NONE)
	private int refID;

	private byte achievementCount;
	private byte requiresSharePt;
	private byte type;
	private QuestStatus status;

	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private Map<Byte, Objective> objectives = new HashMap<>();

	private byte objectiveCount;
	private byte taskCount;

	private int remainingTime ;
	
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private List<Integer> taskRefObjID = new ArrayList<Integer>(); // (=> NPCs to deliver to, when complete you get

	public Quest(int refId) {
		this.refID = refId ; 
	}
    
    public int getRefID() {
        return this.refID;
    }
	

	public void addTaskRefObjId(int val) {
		this.taskRefObjID.add(val);
	}

	public void addQuestObjective(Objective objective) {
		if (objective != null) {
			this.objectives.put(objective.getId(), objective);
		}
	}

}
