package org.sokybot.persistence.entities;


public enum SkillType {

	UNKNOWN((byte)0xff) , 
	Passive((byte) 0x00), Imbue((byte) 0x01), SpeedBuff((byte) 0x02), Buff((byte) 0x03), Heal((byte) 0x04),
	SelfHeal((byte) 0x05), ManaBuff((byte) 0x06), Resurrection((byte) 0x07), Debuff((byte) 0x08), Cure((byte) 0x09),
	KnockDownOnly((byte) 0x0A), Knockdown((byte) 0x0B), WarlockDOT((byte) 0x0C), Other((byte) 0x0F);

	private byte val;

	SkillType(byte val) {
		this.val = val;
	}

	public byte getValue() {
		return this.val;
	}

	public static SkillType parseType(String longId) {

		if (longId.contains("GIGONGTA") || longId.contains("POISONA_BLADE")) {
			return SkillType.Imbue;
		} else if (longId.contains("SKILL_CH_LIGHTNING_GYEONGGONG_A")
				|| longId.contains("SKILL_CH_LIGHTNING_GYEONGGONG_C") || longId.contains("BARD_SPEEDUPA_MSPEED")
				|| longId.contains("SPEED_UP")) {
			return SkillType.SpeedBuff;

		} else if (longId.contains("SKILL_CH_SWORD_SHIELD") // Blade
				|| longId.contains("SKILL_CH_SPEAR_SPIN") // Spear
				|| longId.contains("SKILL_CH_BOW_CALL") || longId.contains("SKILL_CH_BOW_NORMAL") // Bow
				|| longId.contains("SKILL_CH_COLD_GANGGI") || longId.contains("SKILL_CH_COLD_SHIELD") // Cold
				|| longId.contains("SKILL_CH_FIRE_GONGUP") || longId.contains("SKILL_CH_FIRE_GANGGI")
				|| longId.contains("SKILL_CH_FIRE_SHIELD") || longId.contains("SKILL_CH_FIRE_HWABYEOK") // Fire
				|| longId.contains("SKILL_CH_LIGHTNING_GWANTONG") || longId.contains("SKILL_CH_LIGHTNING_JIPJUNG") // Lightnining
				|| longId.contains("SKILL_EU_CLERIC_HEALA_TARGET") // Force
				|| longId.contains("SKILL_EU_WARRIOR_FRENZYA_HEALTH")
				|| longId.contains("SKILL_EU_WARRIOR_FRENZYA_PHYSICAL_BLOCK")
				|| longId.contains("SKILL_EU_WARRIOR_FRENZYA_MASICAL_BLOCK")
				|| longId.contains("SKILL_EU_WARRIOR_FRENZYA_DAMAGE") || longId.contains("SKILL_EU_WARRIOR_GUARDA") // Warrior
				|| longId.contains("SKILL_EU_ROG_POISONA_GUARD") || longId.contains("SKILL_EU_ROG_BOWP_MAD_BOW_UP")
				|| longId.contains("SKILL_EU_ROG_POIS_DAGP_DAGGAR_UP") // Rogue
				|| longId.contains("SKILL_EU_WIZARD_MENTALA_DAMAGEUP")
				|| longId.contains("SKILL_EU_WIZARD_EARTHA_GUARD")
				|| longId.contains("SKILL_EU_WIZARD_MENTALA_DAMAGEUP") // Wizard
				|| longId.contains("SKILL_EU_WARLOCK_SOULA_CHAOS") || longId.contains("SKILL_EU_WARLOCK_SOULA_STUNLINK")
				|| longId.contains("SKILL_EU_WARLOCK_SOULA_RETURN") // Warlock
				|| longId.contains("SKILL_EU_BARD_BATTLAA_GUARD") || longId.contains("EU_BARD_SPEEDUPA_HITRATE")
				|| longId.contains("SKILL_EU_BARD_RECOVERA_ABNORMAL") || longId.contains("SKILL_EU_BARD_DANCEA")
				|| longId.contains("SKILL_EU_BARD_FORGETA_ATTACK") // Bard
				|| longId.contains("SKILL_EU_CLERIC_SAINTA_INNOCENT")
				|| longId.contains("SKILL_EU_CLERIC_RECOVERYA_HEALSHIELD")
				|| longId.contains("SKILL_EU_CLERIC_SAINTA_ABNORMAL") // Cleric
				|| longId.contains("_DAMAGE_DIVIDE") // Chinese damage divide
				|| longId.contains("SKILL_OP_HARMONY")) {
			return SkillType.Buff; // for test
		}else if(longId.contains("SKILL_CH_WATER_HEAL") 
				|| longId.contains("SKILL_EU_CLERIC_HEALA_TARGET")
				|| longId.contains("SKILL_EU_CLERIC_HEALA_DIVIDE")
				|| longId.contains("SKILL_EU_CLERIC_HEALA_CYCLE")
				|| longId.contains("SKILL_EU_CLERIC_RECOVERYA_TARGET")) { 
			return SkillType.Heal ; 
		}else if(longId.contains("SN_SKILL_CH_WATER_SELFHEAL") 
				|| longId.contains("SKILL_EU_CLERIC_HEALA_GROUP")
				|| longId.contains("SKILL_EU_CLERIC_RECOVERYA_GROUP")
                || longId.contains("SKILL_EU_CLERIC_RECOVERYA_QUICK")) { 
			return SkillType.SelfHeal ; 
		}else if(longId.contains("SKILL_EU_BARD_RECOVERA_MPHEAL") || longId.contains("EU_BARD_RECOVERA_MANATRANS")) { 
			return SkillType.ManaBuff ; 
		}else if(longId.contains("SKILL_CH_WATER_RESURRECTION") || longId.contains("SN_SKILL_EU_CLERIC_REBIRTH")) { 
			return SkillType.Resurrection ; 
		}else if(  longId.contains("SKILL_CH_COLD_GIGONGJANG") // Coldwave
	            || longId.contains("SKILL_CH_COLD_BINGPAN") // Frost nova
	            || longId.contains("SKILL_CH_WATER_CANCEL") // Vital Spot
	            || longId.contains("SKILL_EU_WIZARD_COLDA_MANADRY") 
	            || longId.contains("SKILL_EU_WIZARD_EARTHA_ABNORMAL")
	            || longId.contains("SKILL_EU_WIZARD_PSYCHICA_UNTOUCH") // Wizard mana drain, root and fear
	            || longId.contains("SKILL_EU_WARLOCK_SOULA")
	            || longId.contains("SKILL_EU_WARLOCK_RAZEA") 
	            || longId.contains("SKILL_EU_WARLOCK_CONFUSIONA") // Warlock debuffs
	            || longId.contains("SKILL_EU_BARD_FORGETA_TARGET")
	            || longId.contains("SKILL_EU_BARD_BATTLAA_ROOT")) { 
			return SkillType.Debuff ; 
		}else if(longId.contains("SKILL_CH_WATER_CURE")||
				longId.contains("SKILL_EU_BARD_RECOVERA_ABNORMAL")
				|| longId.contains("SKILL_EU_CLERIC_SAINTA_INNOCENT")) { 
			
			return SkillType.Cure ; 
		}else if(longId.contains("SKILL_CH_SWORD_DOWNATTACK")) { 
			return SkillType.KnockDownOnly ; 
		}else if(  longId.contains("SKILL_EU_WARRIOR_TWOHANDA_CHARGE") 
	            || longId.contains("SKILL_EU_WARRIOR_TWOHANDA_CRY") 
	            || longId.contains("SKILL_EU_WARRIOR_TWOHANDA_CHARGE")// Warrior 2hand
	            || longId.contains("SKILL_EU_ROG_BOWA_POWER") 
	            || longId.contains("SN_SKILL_EU_ROG_DAGGERA_WOUND")) {
			return SkillType.Knockdown ; 
		}else if(longId.contains("SKILL_EU_WARLOCK_DOT")) { 
			return SkillType.WarlockDOT ; 
		}
		return SkillType.Other; // for test ;

	}


}
