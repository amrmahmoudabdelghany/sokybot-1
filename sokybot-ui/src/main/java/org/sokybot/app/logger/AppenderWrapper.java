package org.sokybot.app.logger;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class AppenderWrapper extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private List<ILoggingEvent> buffer = new ArrayList<>(256);
	private Appender<ILoggingEvent> origin;

	
	
	@Override
	protected void append(ILoggingEvent eventObject) {

		if (this.origin == null) {
			this.buffer.add(eventObject);
		} else {
			this.origin.doAppend(eventObject);
		}

	}

	public void origin(Appender<ILoggingEvent> appender) {
		synchronized (this.buffer) {
			this.origin = appender;
			for (ILoggingEvent e : this.buffer) {
				this.origin.doAppend(e);
			
			}
			this.buffer.clear();
			

		}

	}

}
