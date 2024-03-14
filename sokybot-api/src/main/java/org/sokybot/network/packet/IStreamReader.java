package org.sokybot.network.packet;

import java.nio.charset.Charset;

public interface IStreamReader {

	
	byte getByte();

	short getUnsignedByte() ; 
	
	short getShort();

	int getInt();

	long getLong();

	byte[] getBytes(int len);

	float getFloat() ; 
	
	boolean getBoolean();
	
	void skip(int len);

	String getString();
	String getString(Charset charset) ; 

}
