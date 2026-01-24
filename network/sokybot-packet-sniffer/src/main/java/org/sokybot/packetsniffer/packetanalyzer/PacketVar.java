package org.sokybot.packetsniffer.packetanalyzer;

public class PacketVar {

	private String packet;
	private String varName;
	private String formatedValue;
	private String originValue;
	private String comment;

	public String getPacket() {
		return packet;
	}

	public void setPacket(String packet) {
		this.packet = packet;
	}

	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

	public String getFormatedValue() {
		return formatedValue;
	}

	public void setFormatedValue(String formatedValue) {
		this.formatedValue = formatedValue;
	}

	public String getOriginValue() {
		return originValue;
	}

	public void setOriginValue(String originValue) {
		this.originValue = originValue;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

}
