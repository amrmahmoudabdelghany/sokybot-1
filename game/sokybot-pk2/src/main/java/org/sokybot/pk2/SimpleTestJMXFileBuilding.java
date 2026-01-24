package org.sokybot.pk2;

public class SimpleTestJMXFileBuilding {

	
	
	public static void main(String args[]) { 
		 
		
		JMXFile file = JMXFile.builder()
				.name("test")
				.pkFilePath("pkFilePathTest")
				.position(10)
				.size(20)
				.build() ; 
		System.out.println(file) ; 
		
		JMXFile file2 = JMXFile.builder()
				.name("JMXFileName")
				.position(10)
				.size(20)
				.build(); 
		
		System.out.println(file2);
		
	}
}
