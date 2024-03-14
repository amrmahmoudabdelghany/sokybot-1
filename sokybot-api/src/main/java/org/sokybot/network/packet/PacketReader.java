/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.network.packet;

import java.nio.ByteBuffer;


/**
 *
 * @author AMROO
 */
public class PacketReader implements IPacketReader {

	private ByteBuffer buffer;

	PacketReader(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int remainingBytes(int offset) {
		offset+= 6 ; 
		return (this.buffer.limit() - offset);
	}

	@Override
	public byte[] readFully() {
	
		return readBytes(0) ; 
	}
	
	@Override
	public byte[] readBytes(int pos)  {

		
		if(pos < 0) throw new IndexOutOfBoundsException(pos);
		
		pos += 6 ; 
		int remainingbytes = this.buffer.limit() - pos ;
		if(remainingbytes == 0) return new byte[0] ; 
		
		if ((remainingbytes >= 1)) {
			byte[] bytes = new byte[remainingbytes];
			
			for(int i = 0 ; i < remainingbytes ; i++ ) { 
				bytes[i] = this.buffer.get(pos); 
 				
				pos ++ ; 
			}
			
			return bytes;
		}
		throw new IndexOutOfBoundsException(pos) ; 
		
	}
	@Override
	public byte[] readBytes(int pos, int length)  {
		
		if(pos < 0) throw new IndexOutOfBoundsException(pos);
		
		pos += 6 ; 
		int remainingbytes = this.buffer.limit() - pos ;
		
		
		if ((remainingbytes >= length)) {
			byte[] bytes = new byte[length];
			int count = length - 1;
			for (int i = (pos + length - 1); i >= pos; i--) { // coping byte by byte to keep the buffer pointer
				bytes[count--] = this.buffer.get(i);
			}
			return bytes;
		}
		throw new IndexOutOfBoundsException(pos);
	}

	
	@Override
	public byte readByte(int pos) {
		pos += 6 ; 
		return this.buffer.get(pos);
	
	}

	@Override
	public short readShort(int pos)  {
		pos += 6 ; 
		return this.buffer.getShort(pos);
	}

	@Override
	public int readInt(int pos) {
		pos += 6 ; 
		return this.buffer.getInt(pos);
	}

	@Override
	public long readLong(int pos) {
		pos += 6 ; 
		return this.buffer.getLong(pos) ; 
	}
	
	@Override
	public float readFloat(int pos) {
		pos += 6 ;
		return this.buffer.getFloat(pos);
	}

	@Override
	public IStreamReader asStreamReader() {
	  return new PacketStreamReader(this) ; 
	}

}
