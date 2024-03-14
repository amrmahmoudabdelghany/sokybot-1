package org.sokybot.security;

public interface ICRCSecurity  {

	 public  byte calculate(byte[] data) ; 
	 
	 public void configur(long seed) ; 
}
