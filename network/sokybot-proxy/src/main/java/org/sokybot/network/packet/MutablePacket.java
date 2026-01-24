package org.sokybot.network.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.network.NetworkPeer;


public class MutablePacket extends Packet {

	
	public MutablePacket(ByteBuffer buffer) {
	   super(buffer) ; 
	}
	
	
	public void setPacketSize(short packetSize) { 
		short size = this.buffer.getShort(0) ; 
		size = (short)((size & 0x8000) | packetSize) ; 
		this.buffer.putShort(0 ,size ) ;
	    //System.out.println("This.setPacketSize is : " + Integer.toBinaryString(this.buffer.getShort(0) & 0xffff)) ; 
		
	}
	
	public void setOpcode(short opcode) { 
		this.buffer.putShort(2,opcode ) ;
	}
	
	public void setCRCByte(byte crcByte) { 
		this.buffer.put( 5, crcByte);
	}
	
	public void setCountByte(byte countByte) { 
		this.buffer.put(4 , countByte) ; 
	}
	
	public void setPacketEncoding(Encoding packetEnc) { 
		short packetSize = this.buffer.getShort(0) ; 
	    packetSize =(short) ((packetEnc == Encoding.ENCRYPTED) ? (packetSize | 0x8000) : (packetSize & 0x7fff)) ;  
	   // this.setPacketSize(packetSize);
	     this.buffer.putShort(0 , packetSize);
	   // System.out.println("This.setPacketEncoding is : " + Integer.toBinaryString(this.buffer.getShort(0) & 0xffff)) ; 
	}
	public void setDataEncoding(Encoding dataEnc) { 
		this.dataEncoding = dataEnc ; 
	}
	
	public byte[] unwrap() { 
		return this.buffer.array() ; 
	}
	
	
	public void setPacketSource(NetworkPeer dir) { 
		this.source = dir ; 
	}
    
	
	public static MutablePacket wrap(byte[] buffer) { 
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer) ; 
		MutablePacket packet = new MutablePacket(byteBuffer);
		// here we must check 
	    
		return packet ; 
	}
	
	public static IPacketBuilder getBuilder(int capacity , int opcode) { 
		ByteBuffer buffer = ByteBuffer.allocate(capacity + 6) ;
		 buffer.order(ByteOrder.LITTLE_ENDIAN) ; 
		return new FixedPacketBuilder(buffer ,(short) opcode) ; 
		
	}
	
	
	public static void main(String args[]) { 
		
		byte[] data = { (byte) 0x00, (byte) 0x80, (byte) 0x56, (byte) 0xc9, (byte) 0x64, (byte) 0xd7, (byte) 0x29,
				(byte) 0x29, (byte) 0x74, (byte) 0x77 };

		MutablePacket packet = MutablePacket.wrap(data) ; 
		
		System.out.println("Packet Size : " + packet.getPacketSize()) ; 
		System.out.println("Packet Encoding : " + packet.getPacketEncoding()) ; 
		
		packet.setPacketSize((short)12);
	
		System.out.println("Packet Size : " + packet.getPacketSize()) ; 
		System.out.println("Packet Encoding : " + packet.getPacketEncoding()) ; 
	
		packet.setPacketEncoding(Encoding.PLAIN);
		
	
		System.out.println("Packet Size : " + packet.getPacketSize()) ; 
		System.out.println("Packet Encoding : " + packet.getPacketEncoding()) ; 
	

		packet.setPacketSize((short)14);

		System.out.println("Packet Size : " + packet.getPacketSize()) ; 
		System.out.println("Packet Encoding : " + packet.getPacketEncoding()) ; 
	
		packet.setPacketEncoding(Encoding.ENCRYPTED);

		System.out.println("Packet Size : " + packet.getPacketSize()) ; 
		System.out.println("Packet Encoding : " + packet.getPacketEncoding()) ; 
		
		packet.setPacketEncoding(Encoding.PLAIN);
		packet.setPacketSize((short) 15);
		System.out.println("Packet Size : " + packet.getPacketSize()) ; 
		System.out.println("Packet Encoding : " + packet.getPacketEncoding()) ; 
		
		
		
		
		
	
	}
}
