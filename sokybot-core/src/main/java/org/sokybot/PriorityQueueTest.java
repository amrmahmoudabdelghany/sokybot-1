package org.sokybot;

import java.util.Comparator;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.lang3.tuple.Pair;

public class PriorityQueueTest {

	public static void main(String args[]) { 
		
		Queue<Pair<Byte, Short>> myQueue = new PriorityBlockingQueue<Pair<Byte , Short>>(11 ,
				
				new Comparator<Pair<Byte , Short>>() {
				public int compare(Pair<Byte,Short> o1, Pair<Byte,Short> o2) {
					
					System.out.println("Comparing " + o1 + " With " + o2);
					// provide priority to spawn packets 
					int value = o1.getLeft() - o2.getLeft() ; 
					
					if(value == 0) { 
						if(o2.getLeft() == 2) { 
							return -1 ; 
						}
					}
					
					return value  ; 
				};
		}); 
		
		
		//Pair<Byte, Short> p = Pair.of((byte)2 ,(short) 20) ; 
		//myQueue.add(p) ; 
		//p = Pair.of((byte)2 ,(short) 20) ; 
		//myQueue.add(p) ; 
		//p = Pair.of((byte)1 ,(short) 20) ; 
		//myQueue.add(p) ; 
		
		//System.out.println(myQueue.poll()); 
		//System.out.println(myQueue.poll()); 
		//System.out.println(myQueue.poll()); 
		
	//	Pair<Byte, Short> p = Pair.of((byte)2 ,(short) 10) ; 
	//	myQueue.add(p) ; 
	//	p = Pair.of((byte)2 ,(short) 20) ; 
	//	myQueue.add(p) ; 
	//	p = Pair.of((byte)1 ,(short) 40) ; 
	//	myQueue.add(p) ; 
		
	//	System.out.println(myQueue.poll()); 
	//	System.out.println(myQueue.poll()); 
	//	System.out.println(myQueue.poll()); 
		
		Pair<Byte, Short> p = Pair.of((byte)1 ,(short) 10) ; 
		myQueue.add(p) ; 
		p = Pair.of((byte)2 ,(short) 20) ; 
		myQueue.add(p) ; 
		
		p = Pair.of((byte)2 ,(short) 30) ; 
		myQueue.add(p) ; 
		
		
		System.out.println(myQueue.poll()); 
		System.out.println(myQueue.poll()); 
		System.out.println(myQueue.poll()); 
		
		 
		
		
		
		
	}
}
