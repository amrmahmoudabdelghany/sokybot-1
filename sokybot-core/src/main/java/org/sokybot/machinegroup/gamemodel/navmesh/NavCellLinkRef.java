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
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NavCellLinkRef implements Serializable {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	
	private Position min ; 
	private Position max ; 
	
	private byte lineFlag ; 
	private byte lineSource ; 
	private byte lineDestination ; 
	private short cellSourceIndex ; 
	private short cellDestinationIndex ;
		

	public boolean hasNeighbour() { 
	   if(cellSourceIndex == -1 || cellDestinationIndex == -1) return false ; 
	   
	   return true ; 
	}
	
	
	public static void main(String args[]) { 
		System.out.println(Byte.MAX_VALUE) ; 
	}
	
}
