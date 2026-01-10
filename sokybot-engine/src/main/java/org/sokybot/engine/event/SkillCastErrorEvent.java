package org.sokybot.engine.event;

import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.machine.IMachineEvent;

import lombok.Getter;

@Getter
public class SkillCastErrorEvent implements IMachineEvent {

	public static final int SKILL_ON_COOLDOWN = 0x05;
	public static final int INVALID_TARGET = 0x06;
	public static final int OBSTACLE = 0x10;
	public static final int WRONG_WEAPON = 0x30;
	public static final int INSUFFICIENT_BOLTS = 0x0E;

	private int errorType;

	public SkillCastErrorEvent(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();

		if (!reader.getBoolean()) {
			byte errorType = reader.getByte();

			if (errorType == 0x0D) {

				byte subType = reader.getByte();
				this.errorType = subType ; 
			}else { 
				 this.errorType = errorType ; 
			}

		}

	}

}
