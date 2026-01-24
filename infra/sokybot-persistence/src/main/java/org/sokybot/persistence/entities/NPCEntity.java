package org.sokybot.persistence.entities;

import java.io.Serializable;

import javax.persistence.Entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class NPCEntity extends SilkroadEntity implements Serializable{
	
     private int level;
     private int HP;
     private NPCType Type;
     
     public int getLevel() { return level; }
     public int getHP() { return HP; }
     public NPCType getType() { return Type; }
     
     public void setLevel(int level) { this.level = level; }
     public void setHP(int HP) { this.HP = HP; }
     public void setType(NPCType Type) { this.Type = Type; }
     
     private String iconPath;
     public String getIconPath() { return iconPath; }
     public void setIconPath(String iconPath) { this.iconPath = iconPath; }
     
     
     
     

     public boolean  isMonster() { 
    	 return this.getLongId().contains("MOB_") ; 
     }

     public boolean isCharacter() { 
    	 return this.getLongId().contains("CHAR_") ; 
     }
     
}
