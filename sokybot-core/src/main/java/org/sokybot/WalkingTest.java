package org.sokybot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.sokybot.machinegroup.gamemodel.npc.IFighter;


public class WalkingTest {
 
	public static Map<Integer, ScheduledFuture<?>> movements = new HashMap<>() ; 
	public static ScheduledExecutorService s = Executors.newScheduledThreadPool(1) ; 
	
	public static void translate(IFighter fighter) { 
		
		ScheduledFuture<?> movement = movements.remove(fighter.getUniqueId()) ;
		if(movement != null) {
			movement.cancel(true) ; 			
		}
		movement = s.scheduleAtFixedRate(()->{
		 
			if(fighter.isHasDestination()) { 
				
			}else {
			movements.remove(fighter.getUniqueId()).cancel(true) ; 
			}
		}, 0, 1, TimeUnit.SECONDS) ; 
		
		movements.put(fighter.getUniqueId(), movement) ; 
		
	}
	
	public static int counter = 0 ; 
	public static void main(String args[]) { 
		
	
		
	final ScheduledFuture<?> movment = 	s.scheduleAtFixedRate(()->{
		 	if(counter < 5) {
			System.out.println("Test") ; 
			counter ++ ; 
		 	}else { 
		 		
		 		System.out.println("Cancel movement 1") ; 
		 		movements.remove(1).cancel(true); 
		 		System.out.println("Test cancel") ; 
		 	}
			
		}, 0, 1, TimeUnit.SECONDS);
		
		movements.put(1, movment) ; 
		
		s.schedule(()->{
			System.out.println("Movements : " );
			movements.forEach((k , v)->{
				System.out.println("K " + k ) ; 
			});
			
			System.out.println("Movement canceld " + 
			movment.isCancelled()) ; 
			System.out.println("Movement done " + 
					movment.isDone()) ; 
					
		}, 10, TimeUnit.SECONDS) ;
		
	}
}
