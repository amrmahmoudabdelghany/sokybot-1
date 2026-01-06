package org.sokybot.machine.service;

import org.sokybot.network.packet.MutablePacket;

public interface IConnectionManager {

	
	 void writeToClient(MutablePacket packet) ; 
	 void writeToServer(MutablePacket packet) ; 
}

