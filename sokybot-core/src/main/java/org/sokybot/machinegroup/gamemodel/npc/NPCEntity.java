package org.sokybot.machinegroup.gamemodel.npc;

import java.io.Serializable;

import javax.persistence.Entity;

import org.sokybot.machinegroup.gamemodel.SilkroadEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;



//https://gitlab.com/opport/Lobot

@Entity
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class NPCEntity extends SilkroadEntity implements Serializable{
	
     private int level;
     private int HP;
     private NPCType Type;
     
     
     
     

     public boolean  isMonster() { 
    	 return this.getLongId().contains("MOB_") ; 
     }

     public boolean isCharacter() { 
    	 return this.getLongId().contains("CHAR_") ; 
     }
     
}
