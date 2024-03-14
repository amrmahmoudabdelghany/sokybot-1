package org.sokybot.machinegroup.gamemodel.item;

import lombok.Data;

@Data
public class MagParam {

    private int INT;
	private int STR;
	private int HP;
	private int MP;
	private int durabilityParam;
	private int hitRatioParam;
	private int blockRatioParam;
	private int criticalBlockParam;
	private int parryRatioParam;
	private int burn;
	private int shock;
	private int freeze;
	private int poison;
	private int zombie;
	
	

	public void setMagParamValue(MagParamType magParamType, int magParamValue)
    {
        switch (magParamType)
        {
            case Str:
                STR = magParamValue;
                break;
            case Int:
                INT = magParamValue;
                break;
            case HP:
                HP = magParamValue;
                break;
            case MP:
                MP = magParamValue;
                break;
            case Durability:
                durabilityParam = magParamValue;
                break;
            case AttackRatio:
                hitRatioParam = magParamValue;
                break;
            case BlockRatio:
                blockRatioParam = magParamValue;
                break;
            case CriticalBlock:
                criticalBlockParam = magParamValue;
                break;
            case ParryRatio:
                parryRatioParam = magParamValue;
                break;
            case Burn:
                burn = magParamValue;
                break;
            case ElectricShock:
                shock = magParamValue;
                break;
            case Freeze:
                freeze = magParamValue;
                break;
            case Poison:
                poison = magParamValue;
                break;
            case Zombie:
                zombie = magParamValue;
                break;
            default:
                break;
        }
    }
}
