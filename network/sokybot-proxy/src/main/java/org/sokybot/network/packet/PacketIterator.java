package org.sokybot.network.packet;

import java.nio.ByteBuffer;

// this class need refactor
public class PacketIterator  implements IPacketIterator{

	private ByteBuffer buffer ; 
	
	
	public PacketIterator(ByteBuffer buffer) { 
		this.buffer = buffer ; 
	}
	

    @Override
    public boolean hasNextWord() {
    //  return (remainingBytes() >= Word.SIZE) ; 
    	throw new UnsupportedOperationException() ; 
    }

    @Override
    public boolean hasNextDword() {
      // return (remainingBytes() >= Dword.SIZE) ; 
    	throw new UnsupportedOperationException() ; 
    }

    @Override
    public boolean hasNextQword() {
       // return (remainingBytes() >= Qword.SIZE) ; 
    	throw new UnsupportedOperationException() ; 
    }

    //@Override
    //public Word nextWord() {
       // if(hasNextWord()) {
        // return new Word(nextBytes(Word.SIZE)) ; //Word.newInstance(nextBytes(Word.SIZE)) ; 
        //}return null ;
    	//throw new UnsupportedOperationException() ; 
   // }

    //@Override
    //public Dword nextDword() {
      // if(hasNextDword()){
        //   return new Dword(nextBytes(Dword.SIZE)) ; 
      // }return null ; 
    	//throw new UnsupportedOperationException() ; 
    //}

    //@Override
    //public Qword nextQword() {
      // if(hasNextQword()) {
        //   return new Qword(nextBytes(Qword.SIZE)) ;
       //}return null ;
    	//throw new UnsupportedOperationException() ; 
    //}

    //@Override
   // public BYTE nextByte() {
     //   if(this.buffer.hasRemaining()){
       //    return new BYTE(buffer.get()) ;
        //}return null ; 
    //	throw new UnsupportedOperationException() ; 
   // }

    @Override
    public byte[] nextBytes(int length) {
        if(remainingBytes()>= length){
        byte [] bytes  = new byte[length] ; 
       
       this.buffer.get(bytes) ; 
       return bytes ; 
        }return null ; 
    }

    @Override
    public int remainingBytes() {
        return this.buffer.remaining() ; 
    }

	
}
