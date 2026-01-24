/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.network.packet;


/**
 *
 * @author AMROO
 */
public interface IPacketReader {
    
   
	
	public byte[] readFully() ; 
	public byte[] readBytes(int pos ) ; 
    public byte[] readBytes(int pos  , int length) ; 
    public byte readByte(int pos ); 
    public short readShort(int pos) ; 
    public int readInt(int pos); 
    public long readLong(int pos) ;
    public int remainingBytes(int offset) ; 
    
    public float readFloat(int pos) ; 
    
    public IStreamReader asStreamReader() ; 
    
    
    
}
