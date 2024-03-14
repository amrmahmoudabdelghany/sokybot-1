package org.sokybot.machinegroup.gamemodel;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LvlEXP {

	@Id
	private int level;

	private long exp ; 
	
}
