package org.sokybot.network.packet;

import java.nio.ByteBuffer;

import org.sokybot.network.NetworkPeer;


public class FixedPacketBuilder implements IPacketBuilder {

	private ByteBuffer buffer;
	private NetworkPeer networkPeer  = NetworkPeer.BOT; 
	private Encoding packetEncoding  = Encoding.PLAIN; 
	private Encoding dataEncoding  = Encoding.PLAIN; 
	private Short opcode ;  
	

	public FixedPacketBuilder(ByteBuffer buffer , Short opcode) {

		this.buffer = buffer;
		this.buffer.position(6);
		this.opcode = opcode ; 
		
	}

	@Override
	public MutablePacket build() {

		MutablePacket packet =  new MutablePacket(buffer);
		packet.setPacketSize((short)(this.buffer.limit() - 6));
		packet.setOpcode(opcode);
		packet.setPacketSource(networkPeer);
		packet.setPacketEncoding(packetEncoding);
		packet.setDataEncoding(Encoding.PLAIN);
		
		return packet ; 
	}

	@Override
	public IPacketBuilder put(byte value) {
		this.buffer.put(value);
		return this;
	}

	@Override
	public IPacketBuilder putBytes(byte[] bytes) {
		this.buffer.put(bytes);
		return this;
	}

	@Override
	public IPacketBuilder putInt(int value) {
		this.buffer.putInt(value);
		return this;
	}

	@Override
	public IPacketBuilder putLong(long value) {

		this.buffer.putLong(value);
		return this;
	}

	@Override
	public IPacketBuilder putShort(short value) {
		this.buffer.putShort(value);
		return this;
	}

	@Override
	public IPacketBuilder packetSource(NetworkPeer source) {
		this.networkPeer = source ; 
		return this;
	}


	@Override
	public IPacketBuilder packetEncoding(Encoding encoding) {
		this.packetEncoding = encoding;
		return this ; 
	}

	@Override
	public IPacketBuilder dataEncoding(Encoding encoding) {
		this.dataEncoding = encoding ; 
		return this ; 
	}
}
