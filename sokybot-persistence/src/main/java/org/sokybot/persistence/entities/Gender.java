package org.sokybot.persistence.entities;

public enum Gender {

	Male((byte)0x00),
	Female((byte)0x01), 
	Unisex((byte)0x02) ; 
	
	
	private byte value ; 
	
	 Gender(byte value) {
		 this.value = value ; 
	}
	
	 public byte getValue() { 
	
		 return this.value; 
	 }
	 
	 public static Gender parseType(byte  val) { 
		 for(Gender g : values()) { 
			 if(g.getValue() == val ) return g ; 
		 }
		 return Gender.Unisex ; 
	 }
	
}
