package org.sokybot.persistence.entities;

import javax.persistence.Entity;

import org.hibernate.annotations.DynamicInsert;
import org.sokybot.persistence.entities.Gender;
import org.sokybot.persistence.entities.Race;
import org.sokybot.persistence.entities.SilkroadEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static org.sokybot.persistence.entities.ItemType.* ; 

@Entity
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class ItemEntity extends SilkroadEntity {

	private boolean isMallItem;
	private ItemType itemType;
	private Race race;

	private Gender gender;
	private boolean isSOX;
	private int level;
	private int degree;
	private int maxStacks;
	private boolean isSortable;
	private String iconPath;



	public boolean isConsumable() {
		return (this.itemType.getValue() >> 8) == 3;
	}

	public boolean isEquipmentItem() {

		return (this.itemType.getValue() >> 8) == 1;
	}

	public boolean isTradeEquipment() { 
		return (this.itemType.getValue() >= 0x171 && this.itemType.getValue() <= 0x173) ;
	}
	public boolean isWeapon() {
		return (this.itemType.getValue() & 0x160) == 0x160;
	}

	public boolean isShield() {
		return (this.itemType.getValue() & 0x140) == 0x140;
	}

	public boolean isClothing() {
		int value = this.itemType.getValue();
		return isEquipmentItem() && !((value & 0x140) == 0x140 || (value & 0x150) == 0x150 || (value & 0x160) == 0x160
				|| (value & 0x1C0) == 0x1C0);
	}

	public boolean isGarment() {
		int value = this.itemType.getValue();
		return isClothing() && ((value >= 0x191 && value <= 0x196) || (value >= 0x111 && value <= 0x116)) ;
	}

	public boolean isProtector() {
		int value = this.itemType.getValue();
		return isClothing() && ((value >= 0x1A1 && value <= 0x1A6) || (value >= 0x121 && value <= 0x126));
	}

	public boolean isArmor() {
		int value = this.itemType.getValue();
		return isClothing() && ((value >= 0x1B1 && value <= 0x1B6) || (value >= 0x131 && value <= 0x136));
	}

	public boolean isHead() {
		return (itemType == GarmentHead || itemType == ProtectorHead || itemType == ArmorHead
				|| itemType == AvatarHead || itemType == RobeHead
				|| itemType == LightArmorHead || itemType == HeavyArmorHead);

	}

	public boolean isShoulder() {
		 
		return (itemType == GarmentShoulder ||
				itemType == ProtectorShoulder || 
				itemType == ArmorShoulder || 
				itemType == RobeShoulder || 
				itemType == LightArmorShoulder || 
				itemType == HeavyArmorShoulder ) ; 
	}

	public boolean isChest() {
		return (itemType == GarmentBody ||
				itemType == ProtectorBody || 
				itemType == ArmorBody || 
				itemType == RobeBody || 
				itemType == LightArmorBody || 
				itemType == HeavyArmorBody
				) ;
	}
	
	public boolean isBoot() {
		 
		return (itemType == GarmentFeet || 
				itemType == ProtectorFeet || 
				itemType == ArmorFeet ||
				itemType == RobeFeet || 
				itemType == LightArmorFeet || 
				itemType == HeavyArmorFeet) ; 
	}

	public boolean isLeg() {
	 
		return (itemType == GarmentLegs || 
				itemType == ProtectorLegs || 
				itemType == ArmorLegs || 
				itemType == RobeLegs || 
				itemType == LightArmorLegs ||
				itemType == HeavyArmorLegs) ; 
	}

	public boolean isHand() {
		
		return (itemType == GarmentGloves || 
				itemType == ProtectorGloves || 
				itemType == ArmorGloves || 
				itemType == RobeGloves || 
				itemType == LightArmorGloves || 
				itemType == HeavyArmorGloves) ; 
	}
		
	public boolean isAccessory() {
		return (itemType.getValue() & 0x150) == 0x150 || (this.itemType.getValue() & 0x1C0) == 0x1C0;
	}

	public boolean isMagicStone() {
		return itemType.getValue() >> 4 == 0x3B1;
	}

	public boolean isAttributeStone() {
		return itemType.getValue() >> 4 == 0x3B2;
	}

	public boolean isJadeTablet() {
		return itemType.getValue() >> 4 == 0x3B30;
	}

	public boolean isRubyTablet() {
		return itemType.getValue() >> 4 == 0x3B31;
	}



	

}
