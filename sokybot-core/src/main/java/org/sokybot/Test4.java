package org.sokybot;

public class Test4 {

	
	public static void main(String args[]) { 
		float xOffset = 1139.7435f; 
		short xSector = 135 ; 
		int res =(int) (((xSector - 135) * 192) + (xOffset / 10)) ;
		
		byte b = (byte) -121 ; 
		short t = (short)((b & 0xff) ); 
		
		System.out.println(t) ; 
	}
}
