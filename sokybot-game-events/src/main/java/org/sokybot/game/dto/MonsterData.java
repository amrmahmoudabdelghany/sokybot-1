package org.sokybot.game.dto;

import org.sokybot.settings.MonsterType;
import org.sokybot.persistence.entities.navmesh.Position;
import lombok.Getter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class MonsterData extends SpawnData {
    
    private final int maxHp;
    private final int currentHp;
    private final int level;
    private final MonsterType monsterType;
}
