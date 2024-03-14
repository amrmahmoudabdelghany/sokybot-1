package org.sokybot.security;

public interface IBlowfish {

	
	//public byte[] transformBytes(byte[] PacketData) ; 
	
	public void configur(byte [] key) ; 
	public byte[] encode(int offset , byte[] data) ; 
	public byte[] decode(int offset , byte[] data) ; 
	//public byte[] encodePacket(byte[] packetData) ; 
	//public byte[] decodePacket(byte[] packetData) ; 
}

