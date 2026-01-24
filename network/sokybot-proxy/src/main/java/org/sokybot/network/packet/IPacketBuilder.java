package org.sokybot.network.packet;

import org.sokybot.network.NetworkPeer;

public interface IPacketBuilder {

	public IPacketBuilder packetSource(NetworkPeer source) ; 
	public IPacketBuilder packetEncoding(Encoding encoding) ; 
	public IPacketBuilder dataEncoding(Encoding encoding) ;
	public IPacketBuilder put(byte value) ; 
	public IPacketBuilder putShort(short value) ;
	public IPacketBuilder putInt(int value) ; 
	public IPacketBuilder putLong(long value) ; 
	public IPacketBuilder putBytes(byte[] bytes) ; 
	
	
	public MutablePacket build() ; 
}
