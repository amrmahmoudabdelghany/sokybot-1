package org.sokybot.persistence.entities;

import javax.persistence.Entity;

import org.sokybot.persistence.entities.SilkroadEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SkillEntity extends SilkroadEntity {

	
	private SkillType type;
    private String name ; 
    
	private int castTime ;
	private int cooldown ; 
	private int duration ; 
	private int masteryId ; 
	private int MP ; 
	
	//private ItemType requiredWeapon ;
	
	private boolean targetRequired ; 
	



	
}
