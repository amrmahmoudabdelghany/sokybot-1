package org.sokybot.machinegroup.gamemodel.skill;

import java.io.Serializable;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
 
	public boolean isEnabled() { 
		return isEnabled == 0x1 ; 
	}
	public byte getSkillLvl() {
		String longId = skillEntity.getLongId();
		int lastPart = longId.lastIndexOf('_') + 1;
		byte lvl = Byte.parseByte(longId.substring(lastPart ));
		return lvl;
	}

}
