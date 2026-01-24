package org.sokybot.gamemodel.model;

import org.sokybot.game.enums.CharacterStatus;
import org.sokybot.game.enums.DebuffStatus;
import org.sokybot.game.enums.LifeState;
import org.sokybot.game.enums.MotionState;
import org.sokybot.game.enums.MovementType;

public interface IFighter extends ISpawn {

    boolean isHasDestination();
    MovementType getMovementType();
    
    LifeState getLifeState();
    boolean isAlive();
    
    DebuffStatus getDebuffStatus();
    MotionState getMotionState();
    CharacterStatus getCharacterStatus();
    
    float getWalkSpeed();
    float getRunSpeed();
    float getHwanSpeed();
    
    int getCurrentHP();
    int getCurrentMP();
    
    int getMaxHP();
    int getMaxMP();
    
    int getHPPercentage();
    int getMPPercentage();
    
    int getDestX();
    int getDestY();
    byte getDestXSector();
    byte getDestYSector();
    short getDestXOffset();
    short getDestYOffset();
    short getDestZOffset();
}
