package org.sokybot.persistence.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class LvlEXP {

	@Id
	private int level;

	private long exp ; 
	
	public int getLevel() {
		return level;
	}
	
	public long getExp() {
		return exp;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public void setExp(long exp) {
		this.exp = exp;
	}
}
