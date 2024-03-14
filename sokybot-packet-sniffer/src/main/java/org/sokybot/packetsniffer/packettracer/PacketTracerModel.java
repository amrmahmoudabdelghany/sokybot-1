package org.sokybot.packetsniffer.packettracer;

import org.dizitart.no2.objects.Id;
import org.sokybot.network.NetworkPeer;

public class PacketTracerModel {

	
	private NetworkPeer source ; 
	private String name ; 
	private boolean ignored ;
	@Id
	String id ; 
	
	private int opcode ; 
	
	private transient int count ;
	private String description ; 
	
	
	
	public PacketTracerModel() {
	}
	
	public PacketTracerModel(NetworkPeer source  , int opcode) {
	 this.source = source ; 
	 this.opcode = opcode ; 
	 this.id  = source.name() + "." + Integer.toHexString(opcode) ; 
	}
	
	
	public NetworkPeer getSource() {
		return source;
	}
	public void setSource(NetworkPeer source) {
		this.source = source;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isIgnored() {
		return ignored;
	}
	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}
	public int getOpcode() {
		return opcode;
	}
	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@Override
	public String toString() {
		return "PacketTracerModel [source=" + source + ", name=" + name + ", ignored=" + ignored + ", opcode=" + opcode
				+ ", description=" + description + "]";
	} 
	
	
	
	
	
}
