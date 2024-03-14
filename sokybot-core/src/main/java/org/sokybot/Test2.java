package org.sokybot;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Test2 {

	
	public static void main(String args[]) throws InterruptedException { 
		final int  angle = 17056 * 360/65536 ; 
		System.out.println("Angle : " + angle) ;
		// source
		final int x1 = -10737;
		final int y1 = 2663;

		// Destination
		int x2 = -10735;
		int y2 = 2642;

		final float speed = 50.0f ; // 5 units a second.
		final int ratio  = (int) (speed * 0.1) ; 
		
		System.out.println("Speed " + speed);

		int xdistance = x2 - x1;
		int ydistance = y2 - y1;
		System.out.println("XDistance : " + xdistance + "  , YDistance : " + ydistance);

		final float distance = (float) Math.sqrt(xdistance * xdistance + ydistance * ydistance);

		final float time = (float) (distance  / (speed * 0.1)) ;

		System.out.println("Time : " + time + " seconds , Distance : " + distance + " units");
 
		// 1 sec == 1000 milis
		int currentX = x1;
		int currentY = y1;

		Point currentPoint = new Point(currentX, currentY) ; 
		System.out.println("Start Point : (" + currentPoint.getX() + "," + currentPoint.getY() + ")" );
		

		Point2D.Double current = new Point.Double(x1, y1) ; 
		
		
			
		
			for(int i = 0 ; i < time ; i++) {
				
			current.x +=  ( 10 * Math.cos(Math.toRadians(93))) ; 
			current.y +=  ( 10 * Math.sin(Math.toRadians(93))) ; 
			
			System.out.println("Current Point : " + current) ; 
			
			Thread.sleep(TimeUnit.SECONDS.toMillis(1)) ; 
			
			}
			
		
		
		System.out.println("End Point  : (" + x2 + "," + y2 + ")" );
		
		
		
	}
}
