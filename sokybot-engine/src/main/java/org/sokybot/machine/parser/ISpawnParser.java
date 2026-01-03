package org.sokybot.machine.parser;

import org.sokybot.machinegroup.gamemodel.item.DropItem;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.sokybot.machinegroup.gamemodel.npc.Pet;
import org.sokybot.machinegroup.gamemodel.npc.Player;
import org.sokybot.machinegroup.gamemodel.portal.Portal;
import org.sokybot.machinegroup.gamemodel.portal.PortalEntity;
import org.sokybot.network.packet.IStreamReader;

public interface ISpawnParser extends IStreamReader {

	
	DropItem readDropItem(ItemEntity entity) ; 
	
	Player readPlayer(NPCEntity entity) ; 
	
	Portal readPortal(PortalEntity entity) ; 
	
	Monster readMonster(NPCEntity entity) ; 
	
	Pet readPet(NPCEntity entity) ; 
	
}
