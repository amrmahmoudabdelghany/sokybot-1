package org.sokybot.packetsniffer.trafficmonitor;

import java.awt.event.ActionEvent;


public interface IPacketMonitorHandler {
	
	public void handle(ActionEvent userAction);

	public void onSelectPacket(TablePacket packet);

	//public void onRecivePacket(String name , ImmutablePacket packet);
}
