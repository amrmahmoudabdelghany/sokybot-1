package org.sokybot.packetsniffer.packetanalyzer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TEST {

	
	public static void main(String args[]) { 

     String str = "01 4f 6e 10 \n02 ae 00   19 \n01 01 4f 6e 10 02 ae 00 19 \n01" ; 
     
     System.out.println("Str Len : " + str.length()) ; 
     
     String keyword = "01 4f 6e 10    \n02  ae 00 \n19 01";
     
   //  keyword = keyword.replace("\n", "") ; 
   //  keyword = keyword.replaceAll("(.(?!$))", "$1\n?");
     
     //^(?:([A-Fa-f0-9]{2})\\s*)+$
       System.out.println("Key Word before Modify : " + keyword.length()) ; 
       keyword = keyword.replaceAll("(?:([A-Fa-f0-9]{2})\\s*)", "$1 *?\n?") ; 
       System.out.println("KeyWord : " + keyword) ; 
       System.out.println("Keyword Len : " + keyword.length()) ; 
     Pattern p = Pattern.compile(keyword) ; 
     Matcher matcher = p.matcher(str) ; 
     
     while(matcher.find()) { 
    	 System.out.println("Start : " + matcher.start()) ; 
    	 System.out.println("End : " + matcher.end()) ; 
     }
     
	}
	
	
	public static boolean isHexString(String str) { 
	 
		
		// ^(?:([a-f0-9]{2})\s*)+$
	
		return str.matches("^(?:([A-Fa-f0-9]{2})\\s*)+$") ; 
		
		//return str.matches("^(0[xX])? ([A-Fa-f0-9]{2})\\s* +$") ; 
	}
	
	
	private static boolean isHexByte(String strByte) { 
		
		if(strByte.length() == 2) { 
			return strByte.matches("^(0[xX])?[A-Fa-f0-9]+$"); 
		}
	
	return false ; 		
	}
}
