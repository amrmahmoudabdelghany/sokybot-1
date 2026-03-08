package org.sokybot.gameevents.dto;

import org.sokybot.persistence.entities.SkillEntity;

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
	
	public int getTargetID() { return targetID; }
	public int getRefSkillID() { return refSkillID; }
	public int getBuffDuration() { return buffDuration; }
	public int getRefSkillParam() { return refSkillParam; }
	public boolean isCreator() { return isCreator; }
	public int getUniqueID() { return uniqueID; }
	
	public void setTargetID(int targetID) { this.targetID = targetID; }
	public void setRefSkillID(int refSkillID) { this.refSkillID = refSkillID; }
	public void setBuffDuration(int buffDuration) { this.buffDuration = buffDuration; }
	public void setRefSkillParam(int refSkillParam) { this.refSkillParam = refSkillParam; }
	public void setCreator(boolean isCreator) { this.isCreator = isCreator; }
	public void setUniqueID(int uniqueID) { this.uniqueID = uniqueID; }
	
	// Delegated methods from SkillEntity/SilkroadEntity
	public int getRefId() { return skillEntity.getRefId(); }
	public String getLongId() { return skillEntity.getLongId(); }
	public String getName() { return skillEntity.getName(); }
	public SkillEntity getSkillEntity() { return skillEntity; }


	public boolean isTransferableBuff() {
		String longId = getLongId();

		return longId.contains("SKILL_EU_CLERIC_RECOVERYA_QUICK_B") 
				|| longId.contains("SKILL_EU_CLERIC_RECOVERYA_GROUP")
				|| longId.contains("BATTLAA_GUARD") 
				|| longId.contains("BARD_DANCEA")
				|| longId.contains("BARD_SPEEDUPA_HITRATE");
	}
}
