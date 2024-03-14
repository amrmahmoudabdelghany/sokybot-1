package org.sokybot.machine.service;

import org.slf4j.Logger;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrainerManager implements ITrainerManager {

	private final byte AUTO_ATTACK_ACTION = 0x01;
	private final byte USE_SKILL_ACTION = 0x04;

	private final IConnectionManager conn;

	private Trainer trainer;

	@Autowired
	Logger log;

	public TrainerManager(IConnectionManager connectionManager, Trainer trainer) {
		this.conn = connectionManager;
		this.trainer = trainer;

	}

	@Override
	public void walk(int tragetX, int targetY) {

		short xSector = SilkroadUtils.getSectorX(tragetX);
		short ySector = SilkroadUtils.getSectorY(targetY);
		short xOffset = (short) SilkroadUtils.getXOffset(tragetX, xSector);
		short yOffset = (short) SilkroadUtils.getYOffset(targetY, ySector);

		this.conn.writeToServer(MutablePacket.getBuilder(9, ClientOpcode.CHAR_MOVEMENT)
				.put((byte) 0x01)
				.put((byte)xSector)
				.put((byte)ySector)
				.putShort(xOffset)
				.putShort((short) 0)
				.putShort(yOffset)
				.build());

	}

	@Override
	public void walkInCave(int targetX, int targetY) {

	}

	@Override
	public void enterBerserkMode() {
 
		

		this.conn
		.writeToServer(MutablePacket.getBuilder(1, 0x70A7)
				.put((byte)0x01).build());

	}
	
	@Override
	public void select(int npcId) {

		this.conn.writeToServer(MutablePacket.getBuilder(4, ClientOpcode.CHAR_SELECT).putInt(npcId).build());

	}

	@Override
	public void attack(int monsterId) {

		this.conn.writeToServer(MutablePacket.getBuilder(7, ClientOpcode.CHAR_ACTION)
				.put(AUTO_ATTACK_ACTION)
				.put((byte) 0x01)
				.put((byte) 0x01)
				.putInt(monsterId)
				.build());
	}

	@Override
	public void useSkill(int skillId) {

		if (!this.trainer.hasSkill(skillId)) {
			log.info("Traienr does not have skill  " + skillId);
			return;
		}

		this.conn.writeToServer(MutablePacket.getBuilder(6, ClientOpcode.CHAR_ACTION)
				.put(USE_SKILL_ACTION)
				.putInt(skillId)
				.put((byte) 0x00)
				.build());

	}

	@Override
	public void useSkill(int skillId, int targetId) {
		if (!this.trainer.hasSkill(skillId)) {
			log.info("Traienr does not have skill  " + skillId);
			return;
		}

		this.conn.writeToServer(MutablePacket.getBuilder(11, ClientOpcode.CHAR_ACTION)
				.put((byte) 0x01)
				.put(USE_SKILL_ACTION)
				.putInt(skillId)
				.put((byte) 0x01)
				.putInt(targetId)
				.build());

	}

	@Override
	public void levelUpMastery(int masteryID) {

		this.conn.writeToServer(MutablePacket.getBuilder(5, ClientOpcode.CHAR_MASTERY_LVL_UP)
				.putInt(masteryID)
				.put((byte) 0x01)
				.build());
	}

	@Override
	public void levelUpSkill(int skillId) {
		this.conn.writeToServer(MutablePacket.getBuilder(5, ClientOpcode.CHAR_SKILL_LVL_UP).putInt(skillId).build());

	}

}
