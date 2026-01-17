package org.sokybot.machine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.app.AppConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

@Configuration
public class SimpleLoggerConfig {

    @Bean
    @Qualifier
    Logger machineLogger(@Value("${" + AppConstants.MACHINE_NAME + "}") String machineName) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(machineName);
        
        // In headless mode, we rely on existing appenders (e.g. root Console/File)
        // or we could add a specific file appender here if needed.
        // For now, just setting the level and returning the logger is sufficient.
        
        logger.setLevel(Level.INFO);
        // logger.setAdditive(false); // Keep additive true to bubble up to root unless we handle it

        return logger;
    }
}
