package org.sokybot.app.logger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.LoggerFactory;
import org.sokybot.swing.ANSITextPane;
import org.sokybot.swing.GuiAppender;
import org.sokybot.service.IMainFrameConfigurator;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;

@Component(immediate = true)
public class LogToolWindow {

    private IMainFrameConfigurator mainFrameConfigurator;

    @Reference
    public void setMainFrameConfigurator(IMainFrameConfigurator mainFrameConfigurator) {
        this.mainFrameConfigurator = mainFrameConfigurator;
    }

    private Icon logIcon;

    @Activate
    public void activate() {
        // Initialize Icon
        try {
            FlatSVGIcon icon = new FlatSVGIcon("icons/feed.svg", getClass().getClassLoader());
            icon = icon.derive(0.40f);
            ColorFilter filter = ColorFilter.getInstance();
            filter.add(Color.black, Color.DARK_GRAY, Color.LIGHT_GRAY);
            icon.setColorFilter(filter);
            this.logIcon = icon;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Install log window when activated
        installLogToolWindow();
    }

    private void installLogToolWindow() {
        JPanel panel = new JPanel(new BorderLayout());
        ANSITextPane textPane = ansiTextPane();
        panel.add(new JScrollPane(textPane), BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // Configure Appender
        configureAppender(textPane);

        mainFrameConfigurator.addExtraWindow("Log", "Sokybot log", this.logIcon, panel);
    }

    private ANSITextPane ansiTextPane() {
        ANSITextPane atp = new ANSITextPane();
        atp.setBackground(Color.BLACK);
        atp.setFont(new Font("Consolas", Font.PLAIN, 15));
        atp.setEditable(false);
        return atp;
    }

    private void configureAppender(ANSITextPane textPane) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        PatternLayout pattern = new PatternLayout();
        pattern.setContext(context);
        pattern.setPattern("%cyan([%date{dd MMM ;HH:mm:ss.SSS}]) %highlight(%-5level) %magenta(%logger{15}) - %green(%msg) %n");
        pattern.start();

        AppenderBase<ILoggingEvent> appender = GuiAppender.builder()
                .pattern(pattern)
                .guiWriter(textPane)
                .build();
        
        appender.setContext(context);
        appender.start();
        
        // Add to root logger? Or allow configuration? 
        // For now, let's add to root
        ch.qos.logback.classic.Logger root = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
    }
}
