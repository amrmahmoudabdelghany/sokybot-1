package org.sokybot.runtime;

import org.sokybot.IContextAdapter;
import org.sokybot.ui.api.IMachinePageViewer;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.engine.IEngine;


import org.sokybot.settings.Settings;

public interface IMachineContext extends IContextAdapter {

	IPacketPublisher packetPublisher();
	
	IMachinePageViewer machinePageViewer();
	
	String fullName();
	
	boolean isRunning();
	
	IEngine getEngine();

    Settings getSettings();
}
