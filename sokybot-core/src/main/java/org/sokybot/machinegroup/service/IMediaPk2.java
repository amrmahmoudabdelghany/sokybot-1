package org.sokybot.machinegroup.service;

import java.util.Optional;

import org.sokybot.machinegroup.gamemodel.DivisionInfo;
import org.sokybot.machinegroup.gamemodel.GameInfo;
import org.sokybot.machinegroup.gamemodel.SilkroadType;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.sokybot.machinegroup.gamemodel.npc.ShopEntity;
import org.sokybot.machinegroup.gamemodel.portal.PortalEntity;
import org.sokybot.machinegroup.gamemodel.portal.TeleportEntity;
import org.sokybot.machinegroup.gamemodel.skill.SkillEntity;

public interface IMediaPk2 {

	Optional<SilkroadType> findType();

	Optional<DivisionInfo> findDivisionInfo();

	Optional<SkillEntity> findSkillEntity(int refId);

	Optional<ItemEntity> findItemEntity(int refId);

	Optional<ShopEntity> findShop(int npcRefId);

	Optional<Long> getLvlEXP(int lvl);

	Optional<TeleportEntity> findTeleport(int refId);

	Optional<PortalEntity> findPortal(int refId);

	Optional<NPCEntity> findNPC(int refId);

	// Optional<SilkroadEntity> findEntity(int refId);

	Optional<String> findMasteryName(int masteryId);

}
