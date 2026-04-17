package org.sokybot.gamemodel.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.gameevents.dto.ItemData;
import org.sokybot.gameevents.dto.MonsterData;
import org.sokybot.gameevents.dto.PlayerData;
import org.sokybot.gamemodel.factory.IEntityFactory;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.IPlayer;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gamemodel.spec.ItemSpec;
import org.sokybot.gamemodel.spec.MonsterSpec;
import org.sokybot.gamemodel.spec.PlayerSpec;
import org.sokybot.gamemodel.spec.TrainerSpec;

@Component(service = IEntityFactory.class)
public class DefaultEntityFactory implements IEntityFactory {

    @Override
    public ITrainer createTrainer(TrainerSpec spec) {
        PlayerData data = PlayerData.builder()
                .uniqueId(spec.getUniqueId())
                .refId(spec.getRefId())
                .name(spec.getName())
                .xSector(spec.getXSector())
                .ySector(spec.getYSector())
                .xOffset(spec.getXOffset())
                .yOffset(spec.getYOffset())
                .zOffset(spec.getZOffset())
                .angle(spec.getAngle())
                .guildName(null)
                .build();
        return new Trainer(data);
    }

    @Override
    public IMonster createMonster(MonsterSpec spec) {
        MonsterData data = MonsterData.builder()
                .uniqueId(spec.getUniqueId())
                .refId(spec.getRefId())
                .name(spec.getName())
                .xSector(spec.getXSector())
                .ySector(spec.getYSector())
                .xOffset(spec.getXOffset())
                .yOffset(spec.getYOffset())
                .zOffset(spec.getZOffset())
                .angle(spec.getAngle())
                .position(spec.getPosition())
                .monsterType(spec.getMonsterType())
                .level(spec.getLevel())
                .currentHp(spec.getCurrentHp())
                .maxHp(spec.getMaxHp())
                .build();
        return new Monster(data);
    }

    @Override
    public IPlayer createPlayer(PlayerSpec spec) {
        PlayerData data = PlayerData.builder()
                .uniqueId(spec.getUniqueId())
                .refId(spec.getRefId())
                .name(spec.getName())
                .xSector(spec.getXSector())
                .ySector(spec.getYSector())
                .xOffset(spec.getXOffset())
                .yOffset(spec.getYOffset())
                .zOffset(spec.getZOffset())
                .angle(spec.getAngle())
                .position(spec.getPosition())
                .guildName(spec.getGuildName())
                .build();
        return new Player(data);
    }

    @Override
    public IItem createItem(ItemSpec spec) {
        ItemData data = ItemData.builder()
                .uniqueId(spec.getUniqueId())
                .refId(spec.getRefId())
                .name(spec.getName())
                .xSector(spec.getXSector())
                .ySector(spec.getYSector())
                .xOffset(spec.getXOffset())
                .yOffset(spec.getYOffset())
                .zOffset(spec.getZOffset())
                .angle(spec.getAngle())
                .position(spec.getPosition())
                .amount(spec.getAmount())
                .ownerExist(false)
                .ownerJID(0)
                .plus((byte) 0)
                .rarity(null)
                .build();
        return new Item(data, spec.getSlot());
    }
}
