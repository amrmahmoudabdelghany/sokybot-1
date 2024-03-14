package org.sokybot.machinegroup.gamemodel.item;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Equipment extends Item {

	private byte optLvl;

	@Delegate
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private WhiteAttribute attributes;

	private int durability ; 
	
	private byte magParamNum ; 
	

	@Delegate
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private MagParam bluse = new MagParam() ;
	
	@Delegate
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)	
	private Option option  = new Option(); 
	
	public Equipment(Item item) {
		super(item);
	}

	public byte getEffectiveLevel() {
		return (byte) (this.optLvl + super.getLevel());
	}

	public void setAttributeValue(long val) {

		if (isWeapon()) {
			this.attributes = new WhiteAttribute(AttributeType.Weapon, val);
		} else if (isShield()) {
			this.attributes = new WhiteAttribute(AttributeType.Shield, val);
		} else if (isAccessory()) {
			this.attributes = new WhiteAttribute(AttributeType.Accessory, val);
		} else {
			this.attributes = new WhiteAttribute(AttributeType.Equipment, val);
		}
	}
	
	
	

	

}
