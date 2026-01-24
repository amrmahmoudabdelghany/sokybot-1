package org.sokybot.pk2extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.sokybot.pk2extractor.exception.Pk2InvalidResourceFormatException;
import org.sokybot.pk2.JMXFile;

/**
 * Utility methods for PK2 file extraction.
 * This class is intended for internal use only.
 * 
 * @author Amr
 */
public class Pk2ExtractorUtils {

    public static byte[] toByteArray(JMXFile jmx) {
        try (InputStream in = jmx.getInputStream()) {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new Pk2InvalidResourceFormatException(
                    "Could not convert joymax file  " + jmx.getName() + " properly ", jmx.getName(), e);
        }
    }

    public static String toText(JMXFile jmx) {
        return toString(jmx, "UTF-8");
    }

    public static String toString(JMXFile jmx, String charset) {
        try {
            return new String(toByteArray(jmx), charset);
        } catch (UnsupportedEncodingException e) {
            throw new Pk2InvalidResourceFormatException(
                    "Could not convert joymax file " + jmx.getName() + " , unsupported encoding  : " + charset,
                    jmx.getName(), e);
        }
    }

    public static int toInteger(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            throw new Pk2InvalidResourceFormatException("Could not convert " + str + " to integer value ", ex);
        }
    }

    public static Stream<String> toLines(String theContent) {
        return Stream.of(StringUtils.split(theContent, System.getProperty("line.separator")));
    }

    public static Stream<CSVRecord> toCSVRecordStream(JMXFile jmx, Charset charset) {
        try {
            return CSVFormat.MYSQL.builder()
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .setCommentMarker('/')
                    .build()
                    .parse(new BufferedReader(new InputStreamReader(jmx.getInputStream(), charset)))
                    .stream();
        } catch (IOException e) {
            throw new Pk2InvalidResourceFormatException(
                    "Could not convert JmxFile " + jmx.getName() + " at " + jmx.getPkFilePath() + " to CSV stream",
                    jmx.getName(), e);
        }
    }

    public static Stream<CSVRecord> toCSVRecordStream(JMXFile jmx) {
        return toCSVRecordStream(jmx, StandardCharsets.UTF_16LE);
    }

    public static byte[] firstChunk(JMXFile file) {
        try (InputStream in = file.getInputStream()) {
            int len = EndianUtils.readSwappedInteger(in);
            byte bytes[] = IOUtils.readFully(in, len);
            return bytes;
        } catch (IOException e) {
            throw new Pk2InvalidResourceFormatException("Unexpected jmx file format [ " + file.getName() + " ]",
                    file.getName(), e);
        }
    }
}
