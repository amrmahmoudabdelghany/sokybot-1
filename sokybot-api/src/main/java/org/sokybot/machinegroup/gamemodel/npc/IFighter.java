package org.sokybot.machinegroup.gamemodel.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.sokybot.machinegroup.gamemodel.skill.Buff;
import org.sokybot.persistence.entities.NPCEntity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class IFighter extends INPCObject {

	private static final long serialVersionUID = 1L;

	private  int currentHP ; 
	
	private boolean hasDestination;

	private MovementType movementType = MovementType.UNKNOWN;

	private short destXSector;

	private short destYSector;

	private short destAngle ; 
	
	private int destXOffset;
	private int destZOffset;
	private int destYOffset;

	private int destX ; 
	private int destY ; 
	
	private byte skyClickFlag;
	

	private LifeState lifeState = LifeState.UNKNOWN;

	private DebuffStatus debuffStatus = DebuffStatus.UNKNOWN;

	private MotionState motionState = MotionState.UNKNOWN;

	private CharacterStatus characterStatus = CharacterStatus.UNKNWON;

	private float walkSpeed;
	private float runSpeed;
	private float hwanSpeed;

	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private Map<Integer, Buff> buff = new HashMap<>();
	
	public int getCurrentHP() { return currentHP; }
	public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }
	public LifeState getLifeState() { return lifeState; }
	
	public void setLifeState(LifeState lifeState) { this.lifeState = lifeState; }

	
	public IFighter(NPCEntity entity) { 
		super(entity) ; 
	}
	
    // Explicit no-args constructor to ensure visibility
    public IFighter() {
        super();
    }

	public boolean isAlive() {
		return this.lifeState != LifeState.Dead  ; 
	}

	public void addBuff(Buff buff) {
		Objects.requireNonNull(buff, "Buff object must not null");
		if (this.buff == null) this.buff = new HashMap<>();
		if (buff != null) {
			this.buff.put(buff.getRefId(), buff);
		}

	}
}
