package org.sokybot.machinegroup.gamemodel.skill;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Data
public class Buff {

	private int targetID;
	private int refSkillID;
	private int buffDuration;
	private int refSkillParam;
	private boolean isCreator;
	private int uniqueID;

	@Delegate
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private SkillEntity skillEntity;

	public Buff(SkillEntity entity) {
		this.skillEntity = entity;
	}

	public boolean isTransferableBuff() {
		String longId = getLongId();

		return longId.contains("SKILL_EU_CLERIC_RECOVERYA_QUICK_B") 
				|| longId.contains("SKILL_EU_CLERIC_RECOVERYA_GROUP")
				|| longId.contains("BATTLAA_GUARD") 
				|| longId.contains("BARD_DANCEA")
				|| longId.contains("BARD_SPEEDUPA_HITRATE");
	}

}
