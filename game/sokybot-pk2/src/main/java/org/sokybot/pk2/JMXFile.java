/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.pk2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import java.nio.file.Paths;

import org.sokybot.pk2.exception.Pk2IOException;
import org.sokybot.pk2.exception.Pk2InvalidJMXFileAttributeException;

import lombok.*;

/**
 * @author AMROO
 */
@Getter
@ToString
@Builder
public class JMXFile {

	@NonNull
	private final String pkFilePath;

	@NonNull
	private final String name;

	private final long position;
	private final int size;

	private final JMXDirectory parent = null;

	/**
	 * TODO write description for this method
	 * 
	 * @param jmxFile
	 * 
	 * @return InputStream for reading from this JMXFile
	 * 
	 * @throws IllegalArgumentException if the target pk2 file is no longer exists 
	 * @throws Pk2IOException if could not establish channel with the target pk2 file 
	 */
	public InputStream getInputStream() {
		// delegate creation of the InputStream  to a static factory  ,
		//this provide a chance to auto-wrapping the origin InputStream 
		//without touching this model class
		return Pk2IO.getInputStream(this);
	}

	/**
	 * Currently this operation is not supported 
	 * 
	 * @throws UnsupportedOperationException
	 */
	public OutputStream getOutputStream() {
		return Pk2IO.getOutputStream(this) ; 
	}

	/**
	 * return the extension of the target file or empty string if it is not declared
	 * . <br>
	 * The '.' separator not included .
	 * 
	 * @return the file extension or empty string if the extension is not declared
	 */
	public String getFileExtension() {
		int index = this.name.lastIndexOf('.');

		if (index == -1) {

			return "";
		}

		return this.name.substring(index + 1);
	}

	static JMXFileBuilder builder() {
		return new _Builder();
	}



	private static class _Builder extends JMXFileBuilder {

		private Pk2InvalidJMXFileAttributeException newException(String message) {
			return new Pk2InvalidJMXFileAttributeException(message, super.name, super.size, super.position);
		}

		@Override
		public JMXFile build() {

			if (super.pkFilePath.isBlank())
				throw newException("Invalid JMXFile Attribute (Pk2 File Path  is blank)");

			if (super.name.isBlank())
				throw newException("Invalid JMXFile Attribute (name is blank)");

			if (super.size < 0)
				throw newException("Invalid JMXFile Attribute ( size : " + super.size + " )");

			if (super.position < 0)
				throw newException("Invalid JMXFile Attribute ( location : " + super.position + " ) ");

			try {
				long pk2FileSize = Files.size(Paths.get(super.pkFilePath));
				long fileUb = (super.position + super.size) - 1;

				if (fileUb <= -1 || fileUb >= pk2FileSize) {
					throw newException("Invalid JMXFile boundaries start  " + super.position + " , end "
							+ (super.position + super.size) + "Where file size " + pk2FileSize);
				}
			} catch (IOException e) {
				throw new Pk2IOException("An exception happened while trying to determine size of" + super.pkFilePath,
						super.pkFilePath, e);
			}

			return super.build();
		}

	}

}