package org.sokybot.machinegroup.service;

import java.util.Optional;

import org.sokybot.machinegroup.gamemodel.DivisionInfo;
import org.sokybot.machinegroup.gamemodel.GameInfo;
import org.sokybot.machinegroup.gamemodel.SilkroadType;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.ShopEntity;
import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.persistence.entities.TeleportEntity;
import org.sokybot.persistence.entities.SkillEntity;

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
