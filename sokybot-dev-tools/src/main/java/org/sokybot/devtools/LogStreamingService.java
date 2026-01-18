package org.sokybot.devtools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ILoggerFactory;
import reactor.core.publisher.Sinks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for streaming and querying application logs.
 * Reads from log files and provides log streaming via RSocket.
 * Works without logback - reads log files directly from filesystem.
 */
public class LogStreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogStreamingService.class);
    
    private final Sinks.Many<Map<String, Object>> logEventSink;
    
    // Pattern to parse log lines (assuming standard Logback format)
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+\\[(.*?)\\]\\s+(.*?)\\s+-\\s+(.*)$"
    );
    
    // Alternative pattern for simpler format: HH:mm:ss.SSS [thread] LEVEL logger - message
    private static final Pattern SIMPLE_LOG_PATTERN = Pattern.compile(
        "^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[(.*?)\\]\\s+(\\w+)\\s+(.*?)\\s+-\\s+(.*)$"
    );
    
    public LogStreamingService() {
        // Create event sink for log streaming
        this.logEventSink = Sinks.many().multicast().onBackpressureBuffer(1000);
        
        // Try to detect log files from common locations
        logger.info("LogStreamingService initialized - will search for log files in common locations");
    }
    
    /**
     * Get list of log files from common locations.
     * Checks standard log file locations and also tries to detect from Logback if available.
     */
    public List<Map<String, Object>> getLogFiles() {
        List<Map<String, Object>> logFiles = new ArrayList<>();
        
        // Try to get log files from Logback appenders if available (optional)
        try {
            ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
            if (loggerFactory != null && loggerFactory.getClass().getName().contains("logback")) {
                // Use reflection to access Logback classes without requiring them at compile time
                try {
                    Object loggerContext = loggerFactory;
                    Object rootLogger = loggerContext.getClass().getMethod("getLogger", String.class)
                        .invoke(loggerContext, Logger.ROOT_LOGGER_NAME);
                    
                    java.util.Iterator<?> appenderIterator = (java.util.Iterator<?>) 
                        rootLogger.getClass().getMethod("iteratorForAppenders").invoke(rootLogger);
                    
                    while (appenderIterator.hasNext()) {
                        Object appender = appenderIterator.next();
                        if (appender.getClass().getName().contains("FileAppender")) {
                            try {
                                String filePath = (String) appender.getClass().getMethod("getFile").invoke(appender);
                                if (filePath != null) {
                                    File logFile = new File(filePath);
                                    if (logFile.exists()) {
                                        Map<String, Object> logFileInfo = new ConcurrentHashMap<>();
                                        logFileInfo.put("path", filePath);
                                        logFileInfo.put("name", logFile.getName());
                                        logFileInfo.put("exists", true);
                                        logFileInfo.put("size", logFile.length());
                                        logFileInfo.put("lastModified", logFile.lastModified());
                                        logFiles.add(logFileInfo);
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore reflection errors
                            }
                        }
                    }
                } catch (Exception e) {
                    // Logback not available or reflection failed, use fallback
                    logger.debug("Could not access Logback appenders via reflection", e);
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking for Logback log files", e);
        }
        
        // Fallback: check common log locations
        String[] commonLogPaths = {
            "sokybot-dev.log",
            "sokybot-errors.log",
            "logs/sokybot.log",
            "logs/application.log",
            "felix-cache/logs/*.log"
        };
        
        for (String logPath : commonLogPaths) {
            try {
                File logFile = new File(logPath);
                if (logFile.exists() && logFile.isFile()) {
                    Map<String, Object> logFileInfo = new ConcurrentHashMap<>();
                    logFileInfo.put("path", logPath);
                    logFileInfo.put("name", logFile.getName());
                    logFileInfo.put("exists", true);
                    logFileInfo.put("size", logFile.length());
                    logFileInfo.put("lastModified", logFile.lastModified());
                    logFiles.add(logFileInfo);
                }
            } catch (Exception e) {
                // Ignore individual file errors
            }
        }
        
        // Also search in current directory for .log files
        try {
            File currentDir = new File(".");
            File[] logFilesInDir = currentDir.listFiles((dir, name) -> 
                name.endsWith(".log") && new File(dir, name).isFile()
            );
            if (logFilesInDir != null) {
                for (File logFile : logFilesInDir) {
                    boolean alreadyAdded = logFiles.stream()
                        .anyMatch(info -> logFile.getAbsolutePath().equals(info.get("path")));
                    if (!alreadyAdded) {
                        Map<String, Object> logFileInfo = new ConcurrentHashMap<>();
                        logFileInfo.put("path", logFile.getAbsolutePath());
                        logFileInfo.put("name", logFile.getName());
                        logFileInfo.put("exists", true);
                        logFileInfo.put("size", logFile.length());
                        logFileInfo.put("lastModified", logFile.lastModified());
                        logFiles.add(logFileInfo);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error searching for log files in current directory", e);
        }
        
        return logFiles;
    }
    
    /**
     * Read log entries from a file (tail-like behavior).
     */
    public List<Map<String, Object>> readLogEntries(String logFilePath, int maxLines, String level, String searchTerm) {
        List<Map<String, Object>> entries = new ArrayList<>();
        
        File logFile = new File(logFilePath);
        if (!logFile.exists() || !logFile.isFile()) {
            return entries;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            List<String> lines = reader.lines().collect(Collectors.toList());
            
            // Take last maxLines
            int startIndex = Math.max(0, lines.size() - maxLines);
            List<String> recentLines = lines.subList(startIndex, lines.size());
            
            for (String line : recentLines) {
                Map<String, Object> entry = parseLogLine(line);
                if (entry != null) {
                    // Apply filters
                    if (level != null && !level.equals("ALL")) {
                        String entryLevel = (String) entry.get("level");
                        if (!level.equals(entryLevel)) {
                            continue;
                        }
                    }
                    
                    if (searchTerm != null && !searchTerm.isEmpty()) {
                        String message = (String) entry.get("message");
                        if (message == null || !message.toLowerCase().contains(searchTerm.toLowerCase())) {
                            continue;
                        }
                    }
                    
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading log file: " + logFilePath, e);
        }
        
        return entries;
    }
    
    /**
     * Parse a log line into structured data.
     */
    private Map<String, Object> parseLogLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> entry = new ConcurrentHashMap<>();
        
        try {
            // Try to parse standard Logback format with full date
            // Format: 2024-01-18 10:30:45.123 INFO  [main] org.sokybot.Class - Message
            java.util.regex.Matcher matcher = LOG_PATTERN.matcher(line);
            
            if (matcher.matches()) {
                entry.put("timestamp", matcher.group(1));
                entry.put("level", matcher.group(2));
                entry.put("thread", matcher.group(3));
                entry.put("logger", matcher.group(4));
                entry.put("message", matcher.group(5));
            } else {
                // Try simpler format: HH:mm:ss.SSS [thread] LEVEL logger - message
                matcher = SIMPLE_LOG_PATTERN.matcher(line);
                if (matcher.matches()) {
                    entry.put("timestamp", matcher.group(1));
                    entry.put("thread", matcher.group(2));
                    entry.put("level", matcher.group(3));
                    entry.put("logger", matcher.group(4));
                    entry.put("message", matcher.group(5));
                } else {
                    // Fallback: simple parsing
                    entry.put("raw", line);
                    entry.put("message", line);
                    
                    // Try to extract level if present
                    String upperLine = line.toUpperCase();
                    if (upperLine.contains(" ERROR ") || upperLine.contains("[ERROR]") || upperLine.startsWith("ERROR")) {
                        entry.put("level", "ERROR");
                    } else if (upperLine.contains(" WARN ") || upperLine.contains("[WARN]") || upperLine.startsWith("WARN")) {
                        entry.put("level", "WARN");
                    } else if (upperLine.contains(" INFO ") || upperLine.contains("[INFO]") || upperLine.startsWith("INFO")) {
                        entry.put("level", "INFO");
                    } else if (upperLine.contains(" DEBUG ") || upperLine.contains("[DEBUG]") || upperLine.startsWith("DEBUG")) {
                        entry.put("level", "DEBUG");
                    } else {
                        entry.put("level", "INFO");
                    }
                    
                    // Try to extract timestamp if present
                    java.util.regex.Pattern timestampPattern = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}|\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");
                    java.util.regex.Matcher tsMatcher = timestampPattern.matcher(line);
                    if (tsMatcher.find()) {
                        entry.put("timestamp", tsMatcher.group(1));
                    }
                }
            }
            
            entry.put("raw", line);
        } catch (Exception e) {
            // Fallback: return raw line
            entry.put("raw", line);
            entry.put("message", line);
            entry.put("level", "INFO");
        }
        
        return entry;
    }
    
    /**
     * Emit log event to stream.
     */
    public void emitLogEvent(Map<String, Object> event) {
        if (logEventSink != null) {
            logEventSink.tryEmitNext(event);
        }
    }
    
    /**
     * Get log event sink for streaming.
     */
    public Sinks.Many<Map<String, Object>> getLogEventSink() {
        return logEventSink;
    }
}
