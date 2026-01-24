package org.sokybot.packetsniffer.trafficmonitor;

import org.sokybot.network.packet.ImmutablePacket;

public class TablePacket {

	private String name;
	private ImmutablePacket packet;

	private TablePacket(String name, ImmutablePacket packet) {
		this.name = name;
		this.packet = packet;
	}

	public static TablePacket createTablePacket(String name, ImmutablePacket packet) {
		return new TablePacket(name, packet);
	}

	public String getName() {
		return name;
	}

	public ImmutablePacket getPacket() {
		return packet;
	}

	@Override
	public String toString() {
		return "TablePacket [name=" + name + ", packet=" + packet + "]";
	}

	
}
