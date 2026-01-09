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
	
	/**
	 * Read a Unicode (UTF-16LE) string with the given length in code units.
	 * Default implementation reads 2*length bytes and converts to UTF-16LE string.
	 */
	default String getUnicodeString(int length) {
		byte[] bytes = getBytes(length * 2);
		return new String(bytes, java.nio.charset.StandardCharsets.UTF_16LE);
	}

}
