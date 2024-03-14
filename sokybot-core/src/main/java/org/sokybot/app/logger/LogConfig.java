package org.sokybot.app.logger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.annotation.Resource;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.slf4j.LoggerFactory;
import org.sokybot.app.mainframe.WindowPreparedEvent;
import org.sokybot.common.ANSITextPane;
import org.sokybot.common.GuiAppender;
import org.sokybot.service.IMainFrameConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;


import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class LogConfig {

	@Autowired
	private IMainFrameConfigurator configurator;

	@Resource(name = "feed")
	Icon logIcon;

	@Bean
	PatternLayout patternLayout() {
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

	@Bean
	ANSITextPane ansiTextPane() {

		ANSITextPane atp = new ANSITextPane();
		atp.setBackground(Color.BLACK);
		atp.setFont(new Font("Consolas", Font.PLAIN, 15));
		atp.setEditable(false);

		return atp;
	}

	@Bean
	AppenderBase<ILoggingEvent> guiAppender() {
		return GuiAppender
				.builder()
				.pattern(patternLayout())
				.guiWriter(ansiTextPane())
				.build() ; 
	}

	@EventListener(WindowPreparedEvent.class)
	void installLogToolWindow(WindowPreparedEvent event) {

		log.debug("installing log tool window");
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JScrollPane(ansiTextPane()), BorderLayout.CENTER);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

		configurator.addExtraWindow("Log", "Sokybot log", this.logIcon, panel);

	}

}
