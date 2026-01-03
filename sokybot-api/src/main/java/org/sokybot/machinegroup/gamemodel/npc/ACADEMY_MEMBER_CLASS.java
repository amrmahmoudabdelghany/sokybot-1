package org.sokybot.machinegroup.gamemodel.npc;
 public  enum ACADEMY_MEMBER_CLASS{
	 
	 APPRENTICE(0x01)   ,
	 ASSISTANT(0x02) , 
	 GUARDIAN(0x03)  , 
	 UNKNOWN(0x00) ; 
	 
	 private byte code  ; 
	 
	 private ACADEMY_MEMBER_CLASS(int code) { 
		 this.code =(byte) code ; 
	 }
	 
	 public static ACADEMY_MEMBER_CLASS getAMC(int code) { 
		 for(ACADEMY_MEMBER_CLASS amc : values()) { 
			 if(amc.code == code) return amc ; 
		 }
		 return UNKNOWN ; 
	 }
 } 
   
