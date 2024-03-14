/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.network.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.network.NetworkPeer;



/**
 *
 * @author AMROO
 */
public  class Packet{
  

	ByteBuffer  buffer ; 
	 
	protected Encoding dataEncoding  = Encoding.PLAIN; 
	
	protected NetworkPeer source  = NetworkPeer.BOT; 
	
	
	
	public Packet(ByteBuffer buffer) { 
		this.buffer = buffer ; 
		this.buffer.order(ByteOrder.LITTLE_ENDIAN) ; 
	}
	
	

	public short getPacketSize() { 
	    //System.out.println("This.getPacketSize is : " + Integer.toBinaryString(this.buffer.getShort(0) & 0xffff)) ; 
		
		return (short) (this.buffer.getShort(0) & 0x7fff );
	}
	
	public int getOpcode() { 
		return this.buffer.getShort(2)& 0xffff ; 
	}
	
	public byte getCount() { 
		return this.buffer.get(4) ; 
	}
	
	public byte getCRC() { 
		return this.buffer.get(5);
	}
	
	
	
	public Encoding getPacketEncoding() { 
	  //  System.out.println("This.getPacketEncoding is : " + Integer.toBinaryString(this.buffer.getShort(0) & 0xffff)) ; 
		
	  return ((this.buffer.getShort(0) & 0x8000) == 0x8000) ? Encoding.ENCRYPTED : Encoding.PLAIN ; 
				
	}
	
	public Encoding getDataEncoding() { 
	   return this.dataEncoding ; 
	}
	
	public NetworkPeer getPacketSource() { 
		return this.source ; 
	}
	
	
    @Override
    public String toString() {
     
    	StringBuilder sb = new StringBuilder() ; 
    	sb.append("Packet 0x" + Integer.toHexString(getOpcode()) + " { ")
    	.append("\nSize : " + getPacketSize()) 
    	.append("\nSource : " + getPacketSource())
    	.append("\nEncoding : " + getPacketEncoding());
    	
    	char[] hexArray = "0123456789ABCDEF".toCharArray();
    	int len = this.buffer.limit() ; 
    	byte [] bytes = null ; 
    	
    	if(this.buffer.hasArray()) { 
    		bytes = this.buffer.array() ; 
    	}else { 
    		bytes = new byte[len] ; 
    		this.buffer.mark() ; 
    		this.buffer.get(bytes);
    		this.buffer.reset() ; 
    	}
    	
    	char[] hexChars = new char[len * 2];
		for (int j = 0; j < len; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		sb.append("\nPayload : ".concat(new String(hexChars)))
		.append("\n}") ; 
		
		return sb.toString() ; 
    }
    
    
}
