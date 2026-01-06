package org.sokybot.machine.controller;

import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateChanged;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.model.ServerFeed;
import org.sokybot.machine.network.PacketListener;
import org.sokybot.machine.network.PacketListener.PacketSource;
import org.sokybot.machine.service.IConnectionManager;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.IPacketReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Controller;

import static org.sokybot.network.packet.GlobalOpcode.*;

@Controller
@WithStateMachine
public class GlobalHandler {

	@Autowired
	Logger log;

	@Autowired
	private IConnectionManager connectionManager;

	@Autowired
	private StateMachine<MachineState, IMachineEvent> machine;

	@Autowired
	private Settings config; 
	

	@StateEntry(source = { MachineState.CONNECTING, MachineState.REDIRECTING }, target = MachineState.HANDSHAKING)
	public void setupSecurityProtocol() {
		log.info("Setup security protocol (Handled by Proxy)");
	}

	@StateChanged(source = MachineState.HANDSHAKING, target = MachineState.CHALENGING)
	public void challengeServer() {
		log.info("Server challenge (Handled by Proxy)");
		// Proxy already validated challenge and fired event to proceed
	}

	/**
	 * This method will triggered only if client connected and accept security
	 * configuration
	 * 
	 * @return
	 * 
	 * @PacketListener(opcode = HANDSHAKE_ACCEPTANCE) public Message<IMachineEvent>
	 *                        onClientAccept() { IMachineEvent event =
	 *                        ClientFeed.CONNECTION_ACCEPTED; log.info("Client
	 *                        accept security confi"); return
	 *                        MessageBuilder.withPayload(event).build(); }
	 */

	/*
	 * Identification is now handled by sokybot-proxy.
	 * The state machine will receive ServerFeed events via ConnectionHandler.
	 */

}
