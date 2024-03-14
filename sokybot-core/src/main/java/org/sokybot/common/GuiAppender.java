package org.sokybot.common;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.Builder;



@Builder
public class GuiAppender extends AppenderBase<ILoggingEvent> implements SmartLifecycle{

	
	
	private Appendable guiWriter ; 
	
	
	private PatternLayout pattern ; 

	
	@Override
	public boolean isRunning() {
		  
		return super.isStarted();
	}

	@Override
	protected void append(ILoggingEvent logEvent) {
		 
	String message = 	this.pattern.doLayout(logEvent) ;
		this.guiWriter.appendAnsi(message );
	}

	
	
  
	
	
	
}
