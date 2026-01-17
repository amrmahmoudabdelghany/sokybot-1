package org.sokybot.machine.service;

import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.IProxyConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectionManager implements IConnectionManager {

	@Autowired
	private IProxyConnection proxyConnection;

	@Override
	public void writeToServer(MutablePacket packet) {
		if (proxyConnection != null) {
			proxyConnection.sendToServer(packet);
		}
	}

	@Override
	public void writeToClient(MutablePacket packet) {
		if (proxyConnection != null) {
			proxyConnection.sendToClient(packet);
		}
	}

}
