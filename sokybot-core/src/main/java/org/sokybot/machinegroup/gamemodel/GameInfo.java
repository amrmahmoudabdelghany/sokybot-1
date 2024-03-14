package org.sokybot.machinegroup.gamemodel;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameInfo {


	@Id
	private String gamePath ; 
	
	private int port ; 
	
	private int version ; 
	
	@OneToOne(cascade = CascadeType.ALL)
	private DivisionInfo divisionInfo ; 
	
	@OneToOne(cascade = CascadeType.ALL)
	private SilkroadType silkroadType ; 
	
	
	
}
