package org.sokybot.runtime;


import org.sokybot.ui.api.IMachinePageViewer;
import org.sokybot.engine.IEngine;

public interface IMachineContext extends IContextAdapter {

	IMachinePageViewer machinePageViewer();
	
	String fullName();
	
	boolean isRunning();
	
	IEngine getEngine();
    
    org.sokybot.proxy.IProxyConnection getProxyConnection();
    
    org.sokybot.gamemodel.IGameModel getGameModel();
    
    String getGroupName();
    
    String getMachineName();
}
