package org.sokybot;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedRateTest {

	
	public void transfer(int x1 ,int y1 ,int x2 ,int y2 , float speed) { 
		int xdistance = x2 - x1;
		int ydistance = y2 - y1;
		final float distance = (float) Math.sqrt(xdistance * xdistance + ydistance * ydistance);
		final float time = (float) (distance * 1000 / (speed * 0.1)) ; // time in milis 
		
		int currentX = x1 ; 
		int currentY = y1 ; 
		
		for (int i = 0; i < time; i+=500) {

			
			 double xv = (((x2 - currentX) * 1000) / (time - i)) ; 
			 double yv = (((y2 - currentY) * 1000) / (time - i))  ;
			 
			 currentX+=xv ; 
			 currentY+=yv ; 
		
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		
		
		
	}
	public static void main(String args[]) {

		// source
		int x1 = -40;
		int y1 = 40;

		// Destination
		int x2 = 40;
		int y2 = 40;

		final float speed = 50.0f ; // 5 units a second.

		System.out.println("Speed " + speed);

		int xdistance = x2 - x1;
		int ydistance = y2 - y1;
		System.out.println("XDistance : " + xdistance + "  , YDistance : " + ydistance);

		final float distance = (float) Math.sqrt(xdistance * xdistance + ydistance * ydistance);

		final float time = (float) (distance * 1000 / (speed * 0.1)) ;

		System.out.println("Time : " + time + " miliseconds , Distance : " + distance + " units");
 
		// 1 sec == 1000 milis
		int currentX = x1;
		int currentY = y1;

		Point currentPoint = new Point(currentX, currentY) ; 
		System.out.println("Start Point : (" + currentPoint.getX() + "," + currentPoint.getY() + ")" );
		
		for (int i = 0; i < time; i+=500) {

			//double v = (currentPoint.distance(x2, y2) *1000) / (time - i);
			
			 double xv = (((x2 - currentPoint.x) * 1000) / (time - i)) ; 
			 double yv = (((y2 - currentPoint.y) * 1000) / (time - i))  ;
			 
			 currentPoint.x+=xv ; 
			 currentPoint.y+=yv ; 
		//	if(currentPoint.x < x2) { 
			//	currentPoint.x+= v ; 
			//}
			
			//if(currentPoint.y < y2) { 
			//	currentPoint.y+=v ;
		//	}
			System.out.println("XV : " + xv + "YV : " + yv) ; 
			
			System.out.println("Move On : (" + currentPoint.getX() + "," + currentPoint.getY() + ")" );
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}
		System.out.println("End Point  : (" + x2 + "," + y2 + ")" );
		

	}
}
