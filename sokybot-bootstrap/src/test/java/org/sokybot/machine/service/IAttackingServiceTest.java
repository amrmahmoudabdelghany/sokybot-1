package org.sokybot.machine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.npc.MonsterType;
import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.sokybot.machinegroup.gamemodel.setting.MonsterPreference;
import org.sokybot.machinegroup.gamemodel.setting.Settings;

@ExtendWith(MockitoExtension.class)
class IAttackingServiceTest {


	@Mock
	Settings settings;
	
	@Mock
	ITrainerManager trainerManager ;
	
	@Captor
	ArgumentCaptor<Integer> captor ; 
	
	@Spy
	@InjectMocks
	MonsterTargetService monsterTargetService;

	private int areaX = 100 ; 
	private int areaY = 100 ; 
	private int areaR = 10 ; 
	
	@Test
	void test() {


		Mockito.when(this.settings.getAreaX()).thenReturn(this.areaX) ; 
		Mockito.when(this.settings.getAreaY()).thenReturn(this.areaY) ; 
		Mockito.when(this.settings.getAreaR()).thenReturn(this.areaR) ; 
		
		Mockito.when(this.settings.getMonsterPreference(MonsterType.Normal)).thenReturn(MonsterPreference.NONE) ; 
		Mockito.when(this.settings.getMonsterPreference(MonsterType.Champion)).thenReturn(MonsterPreference.PREFER) ; 
		Mockito.when(this.settings.getMonsterPreference(MonsterType.Giant)).thenReturn(MonsterPreference.PREFER) ; 
		

		monsterList().forEach((m) -> {
			monsterTargetService.onMonsterSpawn(new MonsterSpawnEvent(this, m));

		});
		

		monsterTargetService.targetNextMonster();
		
		Mockito.verify(this.trainerManager).select(captor.capture());
		  
		
		assertEquals(5 , captor.getValue());
		
	}

	private List<Monster> monsterList() {
		List<Monster> res = new ArrayList<>();
		// add 3 monster genreal and 2 champion and 1 giant
		Monster m = new Monster(NPCEntity.builder().name("General1").build());
		m.setStrengthLevel((byte) 0);
		m.setUniqueId(0);
		m.setLocation(areaX + 1, areaY + 1);
		res.add(m);

		m = new Monster(NPCEntity.builder().name("General2").build());
		m.setStrengthLevel((byte) 0);
		m.setUniqueId(1);
		m.setLocation(areaX + 1, areaY + 1);

		res.add(m);

		m = new Monster(NPCEntity.builder().name("General3").build());
		m.setStrengthLevel((byte) 0);
		m.setUniqueId(2);
		m.setLocation(areaX + 2, areaY + 2);
		res.add(m);
		

		m = new Monster(NPCEntity.builder().name("Champion1").build());
		m.setStrengthLevel((byte) 1);
		m.setUniqueId(3);
		m.setLocation(areaX + 3, areaY + 3);

		res.add(m);

		m = new Monster(NPCEntity.builder().name("Champion2").build());
		m.setStrengthLevel((byte) 1);
		m.setUniqueId(4);
		m.setLocation(areaX + 4, areaY + 4);
		res.add(m);

		m = new Monster(NPCEntity.builder().name("Giant").build());
		m.setStrengthLevel((byte) 4);
		m.setUniqueId(5);
		m.setLocation(areaX + 5, areaY + 5);

		res.add(m);

		return res;
	}

}
