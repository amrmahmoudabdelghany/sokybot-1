package org.sokybot.machinegroup.gamemodel.npc;

public enum HKSlotType {

	//(37 = COS Command, 70 = InventoryItem, 71 = EquipedItem, 73 = Skill, 74 = Action, 78 = EquipedAvatar)
	
	COS_Command(37) , 
	InventoryItem(70) , 
	EquipedItem(71) , 
	Skill(73) , 
	Action(74) , 
	EquipedAvatar(78) , 
	UNKNOWN(-1); 
	
	private byte type ; 
	private HKSlotType(int val) {
		this.type =(byte) val ; 
	}
	
	
	public static HKSlotType of(byte val) { 
		
		for(HKSlotType t : values()) 
			if(t.type == val) return t ; 
		
		return UNKNOWN; 
	}
	
	
}
