package org.sokybot.machine.model.item;

import org.sokybot.game.enums.AttributeType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class WhiteAttribute {

	private long attributeValue;

	private AttributeType type;

	private byte durabilityAttribute;

	private byte hitRatio;

	private byte parryRatio;

	private byte blockRatio;

	private byte criticalRatio;

	private byte phyReinforce;

	private byte magReinforce;

	private byte phyAttack;

	private byte magAttack;

	private byte phyDefense;

	private byte magDefense;

	private byte phyAbsorb;

	private byte magAbsorb;

	
	public WhiteAttribute(AttributeType type) {
		this(type , 0) ; 
	}
	
	public WhiteAttribute(AttributeType type , long value) {
		this.type = type ; 
		this.attributeValue = value ; 
		updateStats();
	}
	
	
	
	public void setAttributeValue(byte value) {
		this.attributeValue = value;
		updateStats();
	}

	public void setDurabilityAttribute(byte value) {
		if (value <= 31) {
			this.durabilityAttribute = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("Durability Value must be less or equal to 31 , but was " + value);
		}
	}

	public void setDurabilityAttrPercentage(byte value) {

		if (value <= 100) {
			setDurabilityAttribute((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"Durability percentage value must be less or equal to 100 , but was " + value);
		}

	}

	public byte getDurabilityPercentage() {
		return (byte) (this.durabilityAttribute * 100 / 31);
	}

	public void setHitRatio(byte value) {
		if (value <= 31) {
			this.hitRatio = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("HitRatio value must be less or equal to 31 , but was " + value);
		}
	}

	public void setHitRatioPercentage(byte value) {
		if (value <= 100) {
			setHitRatio((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"HitRatio percentage value must be less or equal to 100 , but was " + value);
		}

	}

	public byte getHitRatioPercentage() {
		return (byte) (this.hitRatio * 100 / 31);
	}

	public void setParryRatio(byte value) {

		if (value <= 31) {
			this.parryRatio = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("ParryRatio value must be less or equal to 31 , but was " + value);
		}
	}

	public byte getParryRatioPercentage() {
		return (byte) (this.parryRatio * 100 / 31);
	}

	public void setParryRatioPercentage(byte value) {
		if (value <= 100) {
			setParryRatio((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"ParryRatio percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public void setBlockRatio(byte value) {

		if (value <= 31) {
			this.blockRatio = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("BlockRatio value must be less or equal to 31 , but was " + value);
		}

	}

	public byte getBlockRatioPercentage() {
		return (byte) (this.blockRatio * 100 / 31);
	}

	public void setBlockRatioPercentage(byte value) {
		if (value <= 100) {
			setBlockRatio((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"BlockRatio percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public void setCriticalRatio(byte value) {

		if (value <= 31) {
			this.criticalRatio = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("CriticalRatio value must be less or equal to 31 , but was " + value);
		}

	}

	public byte getCriticalRatioPercentage() {
		return (byte) (this.criticalRatio * 100 / 31);
	}

	public void setCriticalRatioPercentage(byte value) {
		if (value <= 100) {
			setCriticalRatio((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"CriticalRatio percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public void setPhyReinforce(byte value) {

		if (value <= 31) {
			this.phyReinforce = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("PhyReinforce value must be less or equal to 31 , but was " + value);
		}

	}

	public byte getPhyReinforcePercentage() {
		return (byte) (this.phyReinforce * 100 / 31);
	}

	public void setPhyReinforcePercentage(byte value) {
		if (value <= 100) {
			setPhyReinforce((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"PhyReinforce percentage value must be less or equal to 100 , but was " + value);
		}
	}
	// MagReinforce

	public void setMagReinforce(byte value) {

		if (value <= 31) {
			this.magReinforce = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("MagReinforce value must be less or equal to 31 , but was " + value);
		}

	}

	public byte getMagReinforcePercentage() {
		return (byte) (this.magReinforce * 100 / 31);
	}

	public void setMagReinforcePercentage(byte value) {
		if (value <= 100) {
			setMagReinforce((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"MagReinforce percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public void setPhyAttack(byte value) {

		if (value <= 31) {
			this.phyAttack = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("PhyAttack value must be less or equal to 31 , but was " + value);
		}

	}

	public byte getPhyAttackPercentage() {
		return (byte) (this.phyAttack * 100 / 31);
	}

	public void setPhyAttackPercentage(byte value) {
		if (value <= 100) {
			setPhyAttack((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"PhyAttack percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public void setMagAttack(byte value) {

		if (value <= 31) {
			this.magAttack = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("MagAttack value must be less or equal to 31 , but was " + value);
		}

	}

	public void setMagAttackPercentage(byte value) {
		if (value <= 100) {
			setMagAttack((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"MagAttack percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public byte getMagAttackPercentage() {
		return (byte) (this.magAttack * 100 / 31);
	}
	
	public void setPhyDefense(byte value) {

		if (value <= 31) {
			this.phyDefense = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("PhyDefense value must be less or equal to 31 , but was " + value);
		}

	}

	public void setPhyDefensePercentage(byte value) {
		if (value <= 100) {
			setPhyDefense((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"PhyDefense percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public byte gePhyDefensePercentage() {
		return (byte) (this.phyDefense * 100 / 31);
	}

	
	public void setMagDefense(byte value) {

		if (value <= 31) {
			this.magDefense = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("MagDefense value must be less or equal to 31 , but was " + value);
		}

	}

	public void setMagDefensePercentage(byte value) {
		if (value <= 100) {
			setMagDefense((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"MagDefense percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public byte geMagDefensePercentage() {
		return (byte) (this.magDefense * 100 / 31);
	}

	public void setPhyAbsorb(byte value) {

		if (value <= 31) {
			this.phyAbsorb = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("PhyAbsorb value must be less or equal to 31 , but was " + value);
		}

	}

	public void setPhyAbsorbPercentage(byte value) {
		if (value <= 100) {
			setPhyAbsorb((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"PhyAbsorb percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public byte gePhyAbsorbPercentage() {
		return (byte) (this.phyAbsorb * 100 / 31);
	}

	
	public void setMagAbsorb(byte value) {

		if (value <= 31) {
			this.magAbsorb = value;
			updateValue();
		} else {
			throw new IllegalArgumentException("MagAbsorb value must be less or equal to 31 , but was " + value);
		}

	}

	public void setMagAbsorbPercentage(byte value) {
		if (value <= 100) {
			setMagAbsorb((byte) (31f / 100f * (float) value));

		} else {
			throw new IllegalArgumentException(
					"MagAbsorb percentage value must be less or equal to 100 , but was " + value);
		}
	}

	public byte geMagAbsorbPercentage() {
		return (byte) (this.magAbsorb * 100 / 31);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private void updateValue() {
        long variance = 0;
        switch (type)
        {
            case Weapon:
                variance |= durabilityAttribute;
                variance <<= 5;
                variance |= phyReinforce;
                variance <<= 5;
                variance |= magReinforce;
                variance <<= 5;
                variance |= hitRatio;
                variance <<= 5;
                variance |= phyAttack;
                variance <<= 5;
                variance |= magAttack;
                variance <<= 5;
                variance |= criticalRatio;
                break;
            case Equipment:
                variance |= durabilityAttribute;
                variance <<= 5;
                variance |= phyReinforce;
                variance <<= 5;
                variance |= magReinforce;
                variance <<= 5;
                variance |= phyDefense;
                variance <<= 5;
                variance |= magDefense;
                variance <<= 5;
                variance |= parryRatio;
                break;
            case Shield:
                variance |= durabilityAttribute;
                variance <<= 5;
                variance |= phyReinforce;
                variance <<= 5;
                variance |= magReinforce;
                variance <<= 5;
                variance |= blockRatio;
                variance <<= 5;
                variance |= phyDefense;
                variance <<= 5;
                variance |= magDefense;
                break;
            case Accessory:
                variance |= phyAbsorb;
                variance <<= 5;
                variance |= magAbsorb;
                break;
        }
        this.attributeValue = variance;
	}

	private void updateStats() {
		   long variance = this.attributeValue;
           byte counter = 0;
           switch (type)
           {
               case Weapon: //7 Slots           
                  
                   while (variance > 0)
                   {
                       byte stat = (byte)(variance & 0x1F);
                       switch (counter)
                       {
                           case 0: //Durability
                               durabilityAttribute = stat;
                               break;
                           case 1: //Physical Reinforce
                               phyReinforce = stat;
                               break;
                           case 2: //Magical Reinforce
                               magReinforce = stat;
                               break;
                           case 3: //Hit Ratio (Attack Ratio)
                               hitRatio = stat;
                               break;
                           case 4: //Physical Attack
                               phyAttack = stat;
                               break;
                           case 5: //Magical Attack
                               magAttack = stat;
                               break;
                           case 6: //Critical Ratio
                               criticalRatio = stat;
                               break;
                       }

                       //left shit by 5
                       variance >>= 5;
                       counter++;
                   }
                   break;
               case Equipment: //6 Slots
                  
                   while (variance > 0)
                   {
                       byte stat = (byte)(variance & 0x1F);
                       switch (counter)
                       {
                           case 0: //Durability
                               durabilityAttribute = stat;
                               break;
                           case 1: //Physical Reinforce
                               phyReinforce = stat;
                               break;
                           case 2: //Magical Reinforce
                               magReinforce = stat;
                               break;
                           case 3: //Physical Defense
                               phyDefense = stat;
                               break;
                           case 4: //Magical Defense
                               magDefense = stat;
                               break;
                           case 5: //Evasion Rate(Parry Rate)
                               parryRatio = stat;
                               break;
                       }

                       //left shit by 5
                       variance >>= 5;
                       counter++;
                   }
                   break;
               case Shield: //6 Slots
                  
                   while (variance > 0)
                   {
                       byte stat = (byte)(variance & 0x1F);
                       switch (counter)
                       {
                           case 0: //Durability
                               durabilityAttribute = stat;
                               break;
                           case 1: //Physical Reinforce
                               phyReinforce = stat;
                               break;
                           case 2: //Magical Reinforce
                               magReinforce = stat;
                               break;
                           case 3: //Block Ratio
                               blockRatio = stat;
                               break;
                           case 4: //Physical Defense
                               phyDefense = stat;
                               break;
                           case 5://Magical Defense
                               magDefense = stat;
                               break;
                       }

                       //left shit by 5
                       variance >>= 5;
                       counter++;
                   }
                   
                   break;
               case Accessory: //2 Slots
                   
            	   while (variance > 0)
                   {
                       byte stat = (byte)(variance & 0x1F);
                       switch (counter)
                       {
                           case 0: //Durability
                              phyAbsorb = stat;
                               break;
                           case 1: //Physical Reinforce
                               magAbsorb = stat;
                               break;
                       }

                       //left shit by 5
                       variance >>= 5;
                       counter++;
                   }
                   
                   break;
           }
	}

}
