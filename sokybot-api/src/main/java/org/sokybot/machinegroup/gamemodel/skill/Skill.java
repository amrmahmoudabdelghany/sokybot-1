package org.sokybot.machinegroup.gamemodel.skill;

import java.io.Serializable;

import org.sokybot.persistence.entities.SkillEntity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Data
public class Skill implements Serializable {

	@Delegate
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private SkillEntity skillEntity;

	private byte isEnabled ;

	public Skill(SkillEntity skillEntity) {
		this.skillEntity = skillEntity;

	}
    
    public void setIsEnabled(byte isEnabled) {
        this.isEnabled = isEnabled;
    }
 
	public boolean isEnabled() { 
		return isEnabled == 0x1 ; 
	}
	public byte getSkillLvl() {
		String longId = skillEntity.getLongId();
		int lastPart = longId.lastIndexOf('_') + 1;
		byte lvl = Byte.parseByte(longId.substring(lastPart ));
		return lvl;
	}
    
    // Manual delegates as fallback
    public int getRefId() {
        return skillEntity.getRefId();
    }
    
    public String getName() {
        return skillEntity.getName();
    }

    public int getCooldown() {
        return skillEntity.getCooldown();
    }

}
