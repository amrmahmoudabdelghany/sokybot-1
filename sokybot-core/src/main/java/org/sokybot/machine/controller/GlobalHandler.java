package org.sokybot.machine.controller;

import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateChanged;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.machine.model.SecurityConfig;
import org.sokybot.machine.model.ServerFeed;
import org.sokybot.machine.model.UserAction;
import org.sokybot.machine.network.PacketListener;
import org.sokybot.machine.network.PacketListener.PacketSource;
import org.sokybot.machine.service.IConnectionManager;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.GlobalOpcode;
import org.sokybot.network.packet.IPacketReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.security.Blowfish;
import org.sokybot.security.IBlowfish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.EventHeader;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Controller;

import static org.sokybot.network.packet.GlobalOpcode.*;

import java.nio.ByteBuffer;

@Controller
@WithStateMachine
public class GlobalHandler {

	@Autowired
	Logger log;

	@Autowired
	private IConnectionManager connectionManager;

	@Autowired
	private StateMachine<MachineState, IMachineEvent> machine ;
	
	@Autowired
	private Settings config;
	
	private SecurityConfig secConfig ; 
	

	@PacketListener(opcode = GlobalOpcode.HANDSHAKE, packetSource = PacketSource.SERVER)
	public Message<IMachineEvent> onHandshake(ImmutablePacket packet) {

		log.info("Handshaking.....");
		IPacketReader reader = packet.getPacketReader();
		IMachineEvent event;

		byte flagByte = reader.readByte(0);
		if (flagByte == 0x0E) {

			event = ServerFeed.SETUP;

		} else if (flagByte == 0x10) {
			log.info("Server send challenge packet ");
			event = ServerFeed.CHALLENGE;
		} else {
			log.info("Unknwon handshake event");
			event = ServerFeed.UNKNOWN;
		}

		return MessageBuilder.withPayload(event).setHeader("packet", packet).build();
	}

	@StateEntry(source = { MachineState.CONNECTING, MachineState.REDIRECTING }, target = MachineState.HANDSHAKING)
	public void setupSecurityProtocol(@EventHeader("packet") ImmutablePacket packet) {
		log.info("Setup security protocol");
		this.secConfig  = this.connectionManager.setupSecurity(packet);
		// if (this.config.getBotType() == BotType.CLIENTLESS) {
		log.info("Authenticate connection");
		this.connectionManager.authenticateConnection(secConfig);
		// }
	}

	

	@StateChanged(source = MachineState.HANDSHAKING, target = MachineState.CHALENGING)
	public void challengeServer(
			@EventHeader(name = "packet", required = true) ImmutablePacket challengePacket) {

		log.info("On parsing server challeng packet");

		IMachineEvent event;
		IPacketReader reader = challengePacket.getPacketReader();

		ByteBuffer actualPrivateData = ByteBuffer.wrap(reader.readBytes(1, 8));
		byte [] expectedPrivateData = this.secConfig.getServerPrivateData() ; 
		IBlowfish blowfish = new Blowfish() ; 
		blowfish.configur(this.secConfig.getPrivateKey());
		blowfish.encode(0, expectedPrivateData) ; 
		
		if(actualPrivateData.equals(ByteBuffer.wrap(expectedPrivateData))) { 
			this.connectionManager.writeToServer(MutablePacket.getBuilder(0, GlobalOpcode.HANDSHAKE_ACCEPTANCE).build()) ; 
			event = ClientFeed.CONNECTION_ACCEPTED ; 
			log.info("Connection accepted ...");
		}else { 
			log.info("Connection refused ...");
			event = UserAction.DISCONNECT ; 
		}
		
		
		this.machine.sendEvent(event); 
		
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

	@StateEntry(source = MachineState.CHALENGING, target = MachineState.IDENTIFYING)
	public void identifying() {

		//if (config.getBotType() == BotType.CLIENTLESS) {
			log.info("Identifing service");
			int packetSize = AppConstants.CLIENT_MODULE_NAME.length() + 3;

			MutablePacket moduleIdentification = MutablePacket.getBuilder(packetSize, MODULE_IDENTIFICATION)
					.packetEncoding(Encoding.ENCRYPTED)
					.putShort((short) AppConstants.CLIENT_MODULE_NAME.length())
					.putBytes(AppConstants.CLIENT_MODULE_NAME.getBytes())
					.put((byte) 0x00)
					.build();

			connectionManager.writeToServer(moduleIdentification);
		//}

	}

	@PacketListener(opcode = MODULE_IDENTIFICATION, packetSource = PacketSource.SERVER)
	public Message<IMachineEvent> serverIdentification(ImmutablePacket packet) {

		IPacketReader reader = packet.getPacketReader();
		int serviceNameLen = reader.readShort(0);
		String name = new String(reader.readBytes(2, serviceNameLen));
		IMachineEvent event;
		if (name.equals("GatewayServer")) {

			event = ServerFeed.GATEWAY_CONNECTED;

		} else if (name.equals("AgentServer")) {
			event = ServerFeed.AGENT_CONNECTED;
		} else {
			event = ServerFeed.UNKNOWN;
		}

		return MessageBuilder.withPayload(event).build();

	}

}
