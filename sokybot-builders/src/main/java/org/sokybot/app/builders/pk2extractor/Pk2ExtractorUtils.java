import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.sokybot.app.builders.pk2extractor.exception.Pk2ExtractionException;
import org.sokybot.app.builders.pk2extractor.exception.Pk2InvalidResourceFormatException;
import org.sokybot.pk2.JMXFile;



/**
 * This class is intended for internal use only
 * 
 * @author Amr
 *
 */
public class Pk2ExtractorUtils {

public	static byte[] toByteArray(JMXFile jmx) {
		try (InputStream in = jmx.getInputStream()) {

			return IOUtils.toByteArray(in);
		} catch (IOException e) {
			// TODO write appropriate message
			throw new Pk2InvalidResourceFormatException(
					"Could not convert joymax file  " + jmx.getName() + " properly ", jmx.getName(), e);
		}

	}

public static String toText(JMXFile jmx) {
		return toString(jmx, "UTF-8");
	}

public	static String toString(JMXFile jmx, String charset) {
		try {
			return new String(toByteArray(jmx), charset);
		} catch (UnsupportedEncodingException e) {
			// TODO write appropriate message
			throw new Pk2InvalidResourceFormatException(
					"Could not convert joymax file " + jmx.getName() + " , unsupported encoding  : " + charset,
					jmx.getName(), e);

		}
	}
	

	// TODO write docs
public	static int toInteger(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException ex) {
			throw new Pk2InvalidResourceFormatException("Could not convert " + str + " to integer value ", ex);
		}
	}

	// TODO write docs
public	static Stream<String> toLines(String theContent) {
		// TODO check input validation
		return Stream.of(StringUtils.split(theContent, System.getProperty("line.separator")));
	}

	

	// because we aim to read all records , so the parser will close the channel
	// after reach to the end
public	static Stream<CSVRecord> toCSVRecordStream(JMXFile jmx , Charset charset) {
		try {

			return CSVFormat.MYSQL.builder()
					.setIgnoreEmptyLines(true)
					.setTrim(true)
					.setCommentMarker('/')
					.build()
					.parse(new BufferedReader(new InputStreamReader(jmx.getInputStream(), charset)))
					.stream();
		} catch (IOException e) {

			throw new Pk2InvalidResourceFormatException("Could not convert JmxFile " + jmx.getName() + " at " + jmx.getPkFilePath() +  " to CSV stream", jmx.getName(), e);
		}
	}
public static Stream<CSVRecord> toCSVRecordStream(JMXFile jmx) { 
	  return toCSVRecordStream(jmx, StandardCharsets.UTF_16LE) ; 
  }

public	static byte[] firstChunk(JMXFile file) {
		try (InputStream in = file.getInputStream()) {
			int len = EndianUtils.readSwappedInteger(in);
			byte bytes[] = IOUtils.readFully(in, len);
			return bytes;
		} catch (IOException e) {
			throw new Pk2InvalidResourceFormatException("Unexpected jmx file format [ " + file.getName() + " ]",
					file.getName(), e);
		}
	}
/*
	// this method does not work properly within OSGI env 
public	static <T> IPK2EntityExtractor<T> getExtractorForEntity(Class<T> entity) {
		String pack = Pk2ExtractorUtils.class.getPackage().getName().replaceAll("[.]", "/"); //

		return new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(pack))).lines()
				.filter((line) -> line.endsWith(".class"))
				.map((line) -> {
					try {
						return Class
								.forName(pack.replaceAll("[/]", ".") + "." + line.substring(0, line.lastIndexOf(".")));
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				})
				.filter(c -> TypeUtils.isAssignable(c, IPK2EntityExtractor.class))
				.filter((c) -> TypeUtils.getTypeArguments(c, IPK2EntityExtractor.class)
						.values()
						.stream()
						.anyMatch(type -> TypeUtils.equals(type, entity)))
				.findFirst()
				.map((clazz) -> {
					try {
						// return IEntityExtractor.class.cast(clazz.getConstructor().newInstance()) ;
						System.out.println("Founded Class : " + clazz.getName());
						return (IPK2EntityExtractor<T>) clazz.getConstructor().newInstance();
					} catch (InstantiationException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				})
				.orElseThrow(() -> new Pk2ExtractionException("No Entity Extractor for " + entity.getName()));

	}
*/
}




