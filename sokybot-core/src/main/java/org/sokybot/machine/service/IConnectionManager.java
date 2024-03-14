package org.sokybot.machine.service;

import org.sokybot.machine.model.SecurityConfig;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;

public interface IConnectionManager {

	
	 void writeToClient(MutablePacket packet) ; 
	 void writeToServer(MutablePacket packet) ; 
	 SecurityConfig setupSecurity(ImmutablePacket packet) ; 
	 void authenticateConnection(SecurityConfig secConfig) ; 
}

