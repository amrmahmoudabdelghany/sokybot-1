package org.sokybot.gamemodel.model;

import org.sokybot.gameevents.enums.MonsterType;

public interface IMonster extends IFighter {

    MonsterType getMonsterType();

    int getLevel();

    int getMaxHP();
}
