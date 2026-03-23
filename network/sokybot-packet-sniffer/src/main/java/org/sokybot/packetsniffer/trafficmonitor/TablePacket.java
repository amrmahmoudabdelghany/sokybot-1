package org.sokybot.packetsniffer.trafficmonitor;

import org.sokybot.network.packet.ImmutablePacket;

public class TablePacket {

	private String name;
	private ImmutablePacket packet;
	private int index;

	private TablePacket(String name, ImmutablePacket packet, int index) {
		this.name = name;
		this.packet = packet;
		this.index = index;
	}

	public static TablePacket createTablePacket(String name, ImmutablePacket packet, int index) {
		return new TablePacket(name, packet, index);
	}

	public int getIndex() {
		return index;
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
