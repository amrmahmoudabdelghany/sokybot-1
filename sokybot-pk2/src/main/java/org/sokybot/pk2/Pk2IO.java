package org.sokybot.pk2;


import org.sokybot.pk2.exception.Pk2IOException;
import org.sokybot.pk2.io.EncryptedTextInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public final class Pk2IO {

	private Pk2IO() {
	}


	/**
	 * create an input stream for the target file .
	 * <br> 
	 * This factory may wrap the origin input stream if necessary 
	 * 
	 * @param target the target jmx file to read from 
	 * 
	 * @return {@link InputStream} for reading from {@code target} jmx file 
	 * @throws IllegalArgumentException if the target pk2 file is no longer exists 
	 * @throws Pk2IOException if could not establish channel with the target pk2 file 
	 */
	static InputStream getInputStream(final JMXFile target) { 

		String pkFilePath = target.getPkFilePath() ; 
		long position = target.getPosition() ; 
		int size = target.getSize() ; 
		
		
		try {
			
			Path filePath = Path.of(pkFilePath);

			if (!Files.exists(filePath)) {
				throw new IllegalArgumentException("The pk2file " + pkFilePath + " is no longer exists");
			}

		
			InputStream res = new JMXFileInputStream(pkFilePath, position, size);
			
			if ( target.getFileExtension().equals("txt") && Pk2CryptoUtils.isEncryptedTextFile(target)) {
				res = new EncryptedTextInputStream(res, size);
			}

			return res;
		} catch (IOException e) {
			throw new Pk2IOException(
					"Could not read from JMXFile " + target.getName() + " at " + pkFilePath,
					pkFilePath, e);
		}
	}
	
	static OutputStream getOutputStream(final JMXFile target) { 
		throw new UnsupportedOperationException() ; 
	}
}
