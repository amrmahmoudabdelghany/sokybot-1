package org.sokybot.network.packet;



public interface IPacketIterator {

	    public boolean hasNextWord() ; 
	    public boolean hasNextDword() ; 
	    public boolean hasNextQword() ; 
	    
	     
	   // public BYTE nextByte() ; 
	   // public Word nextWord() ; 
	   // public Dword nextDword() ; 
	   // public Qword nextQword() ; 
	    
	    public byte[] nextBytes(int length) ;
	    
	    
	    public int remainingBytes() ; 
}
