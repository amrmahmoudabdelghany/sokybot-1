package org.sokybot.common;

public class ObjectTest {

	
	
	
	public static void main(String args[]) { 
		
		
		Object obj =  new Student2() ; 
		
		
		System.out.println(obj.getClass().getName()) ;
		
		
	}
	
	
	public static class Student2 { 
		
		
		private String name ; 
		private String age; 
		
		
	}
}
