package org.sokybot.swing;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.Builder;

/**
 * A Logback appender that writes log events to a Swing Appendable component.
 */
@Builder
public class GuiAppender extends AppenderBase<ILoggingEvent> {

    private Appendable guiWriter;
    private PatternLayout pattern;

    @Override
    protected void append(ILoggingEvent logEvent) {
        if (guiWriter != null && pattern != null) {
            String message = this.pattern.doLayout(logEvent);
            this.guiWriter.appendAnsi(message);
        }
    }
}
