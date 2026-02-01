package org.sokybot.gamemodel.internal;

import org.sokybot.gameevents.dto.MonsterData;
import org.sokybot.gameevents.dto.SpawnData;
import org.sokybot.gameevents.enums.CharacterStatus;
import org.sokybot.gameevents.enums.DebuffStatus;
import org.sokybot.gameevents.enums.LifeState;
import org.sokybot.gameevents.enums.MotionState;
import org.sokybot.gameevents.enums.MovementType;
import org.sokybot.gamemodel.model.IFighter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class Fighter extends Spawn implements IFighter {

    private boolean hasDestination;
    private MovementType movementType = MovementType.Walking;
    private LifeState lifeState = LifeState.Alive;
    private DebuffStatus debuffStatus = DebuffStatus.Normal;
    private MotionState motionState = MotionState.None;
    private CharacterStatus characterStatus = CharacterStatus.None;

    private float walkSpeed;
    private float runSpeed;
    private float hwanSpeed;

    private int currentHP;
    private int currentMP;

    private int maxHP = 1;
    private int maxMP = 1;

    // Destination fields
    private int destX;
    private int destY;
    private byte destXSector;
    private byte destYSector;
    private short destXOffset;
    private short destYOffset;
    private short destZOffset;

    private int targetId;

    public Fighter(SpawnData data) {
        super(data);
        if (data instanceof MonsterData) {
            MonsterData md = (MonsterData) data;
            this.currentHP = md.getCurrentHp();
            this.maxHP = md.getMaxHp();
        }
    }

    @Override
    public int getHPPercentage() {
        if (maxHP == 0)
            return 0;
        return (int) ((currentHP * 100L) / maxHP);
    }

    @Override
    public int getMPPercentage() {
        if (maxMP == 0)
            return 0;
        return (int) ((currentMP * 100L) / maxMP);
    }

    public boolean isAlive() {
        return lifeState == LifeState.Alive;
    }

    public void translate(int x, int y) {
        // TODO implementation
    }

    public void setSkyClickFlag(byte flag) {
    }
}
