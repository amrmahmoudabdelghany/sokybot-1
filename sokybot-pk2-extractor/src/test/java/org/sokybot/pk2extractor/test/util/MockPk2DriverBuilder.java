package org.sokybot.pk2extractor.test.util;

import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builder for creating mock PK2 drivers for unit tests.
 * 
 * <p>This builder makes it easy to create mock PK2 drivers with test data.
 * 
 * <p>Example:
 * <pre>
 * IPk2Driver driver = new MockPk2DriverBuilder()
 *     .withFile("itemdata.txt", "CSV content here")
 *     .withFile("itemdata2.txt", "More CSV content")
 *     .build();
 * </pre>
 * 
 * @author sokybot
 */
public class MockPk2DriverBuilder {

    private final List<MockFile> files = new ArrayList<>();

    /**
     * Add a file to the mock driver.
     * 
     * @param fileName File name (can include regex pattern)
     * @param content File content as string
     * @return This builder
     */
    public MockPk2DriverBuilder withFile(String fileName, String content) {
        return withFile(fileName, content, StandardCharsets.UTF_16LE);
    }

    /**
     * Add a file to the mock driver with specific charset.
     * 
     * @param fileName File name
     * @param content File content as string
     * @param charset Charset for encoding
     * @return This builder
     */
    public MockPk2DriverBuilder withFile(String fileName, String content, Charset charset) {
        files.add(new MockFile(fileName, content, charset));
        return this;
    }

    /**
     * Add a file to the mock driver with raw bytes.
     * 
     * @param fileName File name
     * @param content File content as bytes
     * @return This builder
     */
    public MockPk2DriverBuilder withFile(String fileName, byte[] content) {
        files.add(new MockFile(fileName, content));
        return this;
    }

    /**
     * Build the mock PK2 driver.
     * 
     * @return Mock IPk2Driver instance
     */
    public IPk2Driver build() {
        IPk2Driver driver = mock(IPk2Driver.class);

        // Mock find method - returns files matching the pattern
        when(driver.find(anyString())).thenAnswer(invocation -> {
            String pattern = invocation.getArgument(0);
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            return files.stream()
                .filter(f -> regex.matcher(f.fileName).matches())
                .map(MockFile::toJMXFile)
                .collect(Collectors.toList());
        });

        when(driver.find(anyString(), any(Integer.class))).thenAnswer(invocation -> {
            String pattern = invocation.getArgument(0);
            int limit = invocation.getArgument(1);
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern,
                java.util.regex.Pattern.CASE_INSENSITIVE);
            List<JMXFile> matches = files.stream()
                .filter(f -> regex.matcher(f.fileName).matches())
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .map(MockFile::toJMXFile)
                .collect(Collectors.toList());
            return matches;
        });

        when(driver.findFirst(anyString())).thenAnswer(invocation -> {
            String pattern = invocation.getArgument(0);
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern,
                java.util.regex.Pattern.CASE_INSENSITIVE);
            return files.stream()
                .filter(f -> regex.matcher(f.fileName).matches())
                .findFirst()
                .map(MockFile::toJMXFile);
        });

        return driver;
    }

    /**
     * Internal class representing a mock file.
     */
    private static class MockFile {
        final String fileName;
        final byte[] content;

        MockFile(String fileName, String content, Charset charset) {
            this.fileName = fileName;
            this.content = content.getBytes(charset);
        }

        MockFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }

        JMXFile toJMXFile() {
            JMXFile jmxFile = mock(JMXFile.class);
            when(jmxFile.getName()).thenReturn(fileName);
            when(jmxFile.getSize()).thenReturn(content.length);
            when(jmxFile.getPkFilePath()).thenReturn("test/" + fileName);
            
            try {
                when(jmxFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create input stream for mock file: " + fileName, e);
            }

            return jmxFile;
        }
    }
}
