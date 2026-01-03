package org.sokybot.machine.event;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpawnReachDestinationEvent  {


	private Object source ; 
	private int uniqueId ; 

	private double x ; 
	private double y ; 
	
	
	
	
	
	
}
