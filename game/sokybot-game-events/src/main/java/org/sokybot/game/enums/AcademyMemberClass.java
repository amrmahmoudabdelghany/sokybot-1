package org.sokybot.game.enums;

public enum AcademyMemberClass {
	 
	 APPRENTICE(0x01)   ,
	 ASSISTANT(0x02) , 
	 GUARDIAN(0x03)  , 
	 UNKNOWN(0x00) ; 
	 
	 private byte code  ; 
	 
	 private AcademyMemberClass(int code) { 
		 this.code =(byte) code ; 
	 }
	 
	 public static AcademyMemberClass getAMC(int code) { 
		 for(AcademyMemberClass amc : values()) { 
			 if(amc.code == code) return amc ; 
		 }
		 return UNKNOWN ; 
	 }
}
