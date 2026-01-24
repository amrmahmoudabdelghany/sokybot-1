package org.sokybot.persistence.entities;


//https://gitlab.com/opport/Lobot

public enum NPCType  {
        Other(0x00),
        PlayerCh(10000),
        PlayerEU(10010),
        Monster(21100),
        MonsterUniqueA(21110),
        MonsterUniqueB(21113),
        MonsterEvent(21130), // BossA
        MonsterBossB(21131),
        MonsterBossC(21133),
        MonsterBossD(21136),
        MonsterEventStrong(21137), // BossE
        MonsterRaidBoss(21138), // BossF


        MonsterTradeThief(21230),
        MonsterTradeHunter(21330),
        MonsterQuestCH(21400),
        MonsterQuestEU(21410),
        MonsterQuestUnique(21430),
        MonsterSummon(21530),
        NPCInteractive(22030),
        NPCFortressStructure(22130),
        FortressSpecial1(0x23A32),
        FortressSpecial2(0x23B32),
        PetAbility(23330),
        PetPickupStart(23400),
        PetPickup(23430),
        PetGuildCH(23500),
        PetGuildEU(23510),
        PetGuardianB(24130),
        PetFortressHangar(24330),
        PetVehicle(23130),
        PetTransport(23231),
        PetTransportMall(23232)  , 
        UNKNOWN(-1); 
        
        
        private int type  ; 
        
        
        private NPCType(int val) {
		  this.type = val ; 
        }
        
        public static NPCType of(int val) { 
        	for(NPCType t : values()) {
        		if(t.type == val) return t; 
        	}
        	return UNKNOWN ; 
        }
    }
