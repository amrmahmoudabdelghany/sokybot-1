package org.sokybot.machinegroup.gamemodel.navmesh;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Entity
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavBorderRef implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	
	private Position min ; 
	private Position max ; 
	
	private byte lineFlag ; 
	private byte lineSource ; 
	private byte lineDestination ; 
	
	private short regionSourceYX ; 
	private short cellSourceIndex ;
	
	private short regionDestionationYX ;
	private short cellDestinationIndex ; 
	
	
	



	
	
	
	public boolean hasNeighbour() { 
		
		return !(cellSourceIndex == 0xffffffff || cellDestinationIndex == 0xffffffff) ; 
		
	}
	
}
