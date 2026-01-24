package org.sokybot.network.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.sokybot.network.NetworkPeer;

public class ImmutablePacket extends Packet{

	private IPacketReader reader = null  ; 
	
	
	public ImmutablePacket(ByteBuffer buffer) {
	 super(buffer) ; 
	}
	
	
	public byte[] toBytes() { 
		
		byte [] buffer = this.buffer.array() ; 
		return Arrays.copyOf(buffer, buffer.length) ; 
		
	}
	
	
	public IPacketIterator getIterator() { 
		return new PacketIterator(buffer) ; 
	}
	
	public IPacketReader getPacketReader() { 
		if(reader == null) reader = new PacketReader(this.buffer) ; 
		return reader ; 
	}
	
	public IStreamReader getStreamReader() { 
		return getPacketReader().asStreamReader() ; 
	}
	public static ImmutablePacket wrap(byte[] buffer) { 
		return new ImmutablePacket(ByteBuffer.wrap(Arrays.copyOf(buffer, buffer.length))) ; 
	}
	
	public static ImmutablePacket wrap(byte[] buffer , Encoding dataEncoding) { 
		ImmutablePacket packet = new ImmutablePacket(ByteBuffer.wrap(buffer)) ;
		packet.dataEncoding = dataEncoding ; 
		return packet ; 
	}
	public static ImmutablePacket wrap(byte[] buffer  ,  Encoding dataEncoding , NetworkPeer source) { 
		ImmutablePacket packet = new ImmutablePacket(ByteBuffer.wrap(buffer)) ;
		packet.dataEncoding = dataEncoding ; 
		packet.source = source ; 
		return packet ; 
	}
	
}
