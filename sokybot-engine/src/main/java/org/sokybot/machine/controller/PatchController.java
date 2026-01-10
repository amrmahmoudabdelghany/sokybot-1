package org.sokybot.machine.controller;

import org.slf4j.Logger;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.model.ServerFeed;
import org.sokybot.machine.network.PacketListener;
import org.sokybot.machine.service.IPatchService;
import org.sokybot.settings.BotType;
import org.sokybot.settings.Settings;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Controller;

@Controller
@WithStateMachine
public class PatchController {

	@Autowired
	private IGameDataLookup gameDAO;

	@Autowired
	private IPatchService patchService;

	@Autowired
	private Settings config;

	@Autowired
	private Logger log;

	@StateEntry(source = MachineState.IDENTIFYING, target = MachineState.VERIFYING)
	public void verifying() {
		if (this.config.getBotType() == BotType.CLIENTLESS) {

			this.patchService.verify(this.gameDAO.getLocal().orElse((byte)22), this.gameDAO.getVersion());
			log.info("Patch requested");

		}
	}

	@PacketListener(opcode = ServerOpcode.PATCH_RESPONSE)
	public Message<IMachineEvent> onPatchResponse(ImmutablePacket packet) {

		IMachineEvent event  ; 
		
		if (packet.getPacketReader().readByte(0) == 1) {
			event = ServerFeed.COMPATIBLE ; 
			
		} else {
			event = ServerFeed.INCOMPATIBLE ; 
			
		}

		return MessageBuilder.withPayload(event).build() ;
	}

}
