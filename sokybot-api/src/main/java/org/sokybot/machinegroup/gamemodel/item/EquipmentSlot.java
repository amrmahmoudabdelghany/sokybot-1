package org.sokybot.machinegroup.gamemodel.item;

public enum EquipmentSlot {

	Helm(0),

	Mail(1),

	Shoulder(2),

	Gauntlet(3),

	Pants(4),

	Boots(5),

	Primary(6),

	Secondary(7),

	Earring(9), // previously 8

	
	Necklace(10), // previously 9

	/**
	 * Left ring
	 */
	Ring1(11), // previously 10

	/**
	 * Right ring
	 */
	Ring2(12), // previously 11

	/**
	 * PVPCape, JobSuit
	 */
	Extra(13);

	private byte slot;

	private EquipmentSlot(int val) {
		this.slot = (byte) val;
	}
	
	public byte getSlot() { 
		return this.slot ; 
	}
	
	

}
