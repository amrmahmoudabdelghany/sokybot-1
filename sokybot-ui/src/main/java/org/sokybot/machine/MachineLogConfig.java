package org.sokybot.machine;

import java.awt.Color;
import java.awt.Font;

import javax.annotation.PostConstruct;
import javax.swing.JScrollPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.app.AppConstants;
import org.sokybot.common.ANSITextPane;
import org.sokybot.common.GuiAppender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import ch.qos.logback.classic.Level;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;

@Configuration
public class MachineLogConfig {

	@Autowired
	private IMachinePage logPage ; 
	
	
//	@PostConstruct
	//private void wire() { 
	//	this.logPage.addTab("Logs", machineLogTextPane());
	//}
	
	
	@Bean
	@Qualifier("logTextPane")
	ANSITextPane machineLogTextPane() {
		ANSITextPane atp = new ANSITextPane();
		atp.setBackground(Color.BLACK);
		atp.setFont(new Font("Consolas", Font.PLAIN, 15));
		atp.setEditable(false);
		logPage.addTab("Logs", new JScrollPane(atp));
		return atp;

	}

	private PatternLayout patternLayout() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		PatternLayout pattern = new PatternLayout();
		pattern.setContext(context);

		pattern.setPattern(
				"%cyan([%date{dd MMM ;HH:mm:ss.SSS}]) %highlight(%-5level) %magenta(%logger{15}) - %green(%msg) %n");
		// pattern.setPattern("[%date{dd MMM ;HH:mm:ss.SSS}]%highlight(%logger{10}) :
		// %msg");
		pattern.start();

		return pattern;

	}

	private AppenderBase<ILoggingEvent> machineLogAppender() {
		return GuiAppender.builder().pattern(patternLayout()).guiWriter(machineLogTextPane()).build();
	}

	@Bean
	@Qualifier
	Logger machineLogger(@Value("${" + AppConstants.MACHINE_NAME + "}") String machineName) {

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		ch.qos.logback.classic.Logger logger = loggerContext.getLogger(machineName);

		Appender<ILoggingEvent> appender = machineLogAppender();
		appender.start();
		appender.setContext(loggerContext);

		logger.addAppender(appender);
		logger.setLevel(Level.INFO);
		logger.setAdditive(false);

		return logger;
	}
}
