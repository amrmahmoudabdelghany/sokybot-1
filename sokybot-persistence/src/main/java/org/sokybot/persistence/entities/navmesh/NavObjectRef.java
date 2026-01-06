package org.sokybot.persistence.entities.navmesh;

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
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NavObjectRef implements Serializable {

	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	private int objId ; 
	
	private Position position ; 
	private short collisionFlag ;  //0x00 = No, 0xFFFF = Yes
	private float yaw ; 
	private short uniqueId ; 
	private short scale ; 
	private short eventZoneFlag ; 
	private short regionID ;
	
	//private ObjectNavMesh navMesh ; 
	
	
	
	
}
