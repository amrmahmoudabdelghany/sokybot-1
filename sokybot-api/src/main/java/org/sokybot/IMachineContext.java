package org.sokybot;


import org.sokybot.network.IPacketPublisher;

public interface IMachineContext extends IContextAdapter {

	
	

	IPacketPublisher packetPublisher() ; 
	
	IMachinePageViewer machinePageViewer() ; 
	
	
	String fullName() ; 
	
	boolean isRunning() ;; 
}
