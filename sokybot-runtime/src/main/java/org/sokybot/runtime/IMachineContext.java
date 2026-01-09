package org.sokybot.runtime;

import org.sokybot.IContextAdapter;
import org.sokybot.ui.api.IMachinePageViewer;
import org.sokybot.network.IPacketPublisher;

public interface IMachineContext extends IContextAdapter {

	IPacketPublisher packetPublisher();
	
	IMachinePageViewer machinePageViewer();
	
	String fullName();
	
	boolean isRunning();
}
