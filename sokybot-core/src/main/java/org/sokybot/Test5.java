package org.sokybot;

import org.apache.commons.math3.util.Precision;

public class Test5 {

	
	
	public static void main(String args[]) { 
			float x = 20.3658f ; 
			float y = 20.36581f ;
			
			System.out.println(Precision.equals(x, y, 0.001f)) ; 
			
	}
}
