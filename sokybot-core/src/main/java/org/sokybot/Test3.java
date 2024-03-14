package org.sokybot;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Test3 {

	
	
	public static int x = 0 ; 
	public static int y = 0 ; 
	
	public static void main(String args[]) throws InterruptedException { 
		
		
		System.out.println("R : " + Math.toRadians(93)) ; 
		
		int x1 = -5179 ; 
		int y1 = 2993 ; 
		
		int x2 = -5159 ;
		int y2 = 2994 ; 
		
		int ratio = 10 ; 
		
		
		 x = x1 ; 
		 y = y1 ; 
		
		
		
	 final 	ScheduledExecutorService s = Executors.newScheduledThreadPool(1) ;
		
	   s.scheduleAtFixedRate(() -> {


				double angle =Math.toRadians( -1.5533430342749532);


				System.out.println("Angle : " +  angle ) ; 
			if(angle != 0) { 
				double xChange = Math.cos(angle); 
				double yChange = Math.sin(angle) ; 
				System.out.println("xChange : " + xChange) ; 
				System.out.println("yChange : " + yChange) ; 
				 x += (int) Math.round(xChange);
				 y += (int) Math.round(yChange);
				 System.out.println("X : " + x  + " Y : " + y ) ; 
				//log.info("Current Angle {} , XChange {} , YChange {}  ", angle, x, y);

				//fighter.translate(x, y);
				//log.info("Fighter Location ({} , {})", fighter.getX(), fighter.getY());
	
			}else { 
				System.out.println("Reatched to its destination ") ; 
				s.shutdown();  
			//	this.movements.remove(fighter.getUniqueId()).cancel(true);
			}
		
			
		}, 0, 1000 /10, TimeUnit.MILLISECONDS);

		
	}
}
