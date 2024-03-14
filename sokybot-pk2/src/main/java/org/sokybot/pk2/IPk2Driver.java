package org.sokybot.pk2;

import org.sokybot.pk2.exception.Pk2FileCorruptedException;

import org.sokybot.pk2.exception.Pk2IOException;
import org.sokybot.pk2.exception.Pk2InvalidJMXFileAttributeException;
import org.sokybot.pk2.exception.Pk2InvalidKeyException;

import java.io.Closeable;

import java.util.List;
import java.util.Optional;

public interface IPk2Driver extends Closeable {

	/**
	 *
	 * @return the root directory of the represented pk2 file
	 */

	default JMXDirectory getRootDirectory() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * discover the contents of the input jmx directory
	 * 
	 * 
	 * @param dir the directory to be discovered
	 * 
	 * @throws Pk2FileCorruptedException if the represented pk2 is misformatted and
	 *                                   could not travel within it properly
	 */
	default void explore(JMXDirectory dir) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Scan the represented pk2 file for a jmx file(s) that matches the given regex
	 * 
	 * if not matches file(s) exists then this method return empty list
	 * 
	 * you can pass the full path of jmx file without include any regex expressions
	 * into it or you can include regex expression more than one time
	 * 
	 * Example for valid expressions : regex (regex)/dir/(regex)
	 * 
	 * 
	 * 
	 * 
	 * @param regex expression
	 * @return file(s) with name matches input regex
	 * @throws Pk2FileCorruptedException           if the represented pk2 file is
	 *                                             misformatted and could not read
	 *                                             entries from it
	 *                                             
	 * @throws Pk2InvalidJMXFileAttributeException If encounter a jmx file stored
	 *                                             with invalid attributes
	 * 
	 * 
	 * 
	 */
	default List<JMXFile> find(String regex) {
		return find(regex, -1);
	}

	/**
	 * 
	 * this method return file(s) with name matches input regex
	 * 
	 * the result list size will not exceed the input limit
	 * 
	 * if there not exists file names matches the regex , this method will return
	 * empty list
	 * 
	 * @throws Pk2FileCorruptedException           if the represented pk2 file is
	 *                                             misformatted and could not read
	 *                                             entries from it
	 * 
	 * @throws Pk2InvalidJMXFileAttributeException If encounter a jmx file stored
	 *                                             with invalid attributes
	 * 
	 */
	List<JMXFile> find(String regex, int limit);

	default Optional<JMXFile> findFirst(String regex) {
		return find(regex, 1).stream().findFirst();
	}

	/**
	 * create an object that represent a pk2 file allow interaction with this file
	 * 
	 * @return the Pk2File representation
	 * @throws IllegalArgumentException  if {@code filePath} is not exists
	 * @throws Pk2IOException            if could not open channel with this pk2
	 *                                   file
	 * @throws Pk2FileCorruptedException if could not read pk2 file header
	 * @throws Pk2InvalidKeyException    TODO describe when throws this exception
	 * @author Amr
	 * 
	 */
	static IPk2Driver open(String filePath) {
		Pk2File file = new Pk2File(filePath);
		file.open();
		return file;
	}
}
