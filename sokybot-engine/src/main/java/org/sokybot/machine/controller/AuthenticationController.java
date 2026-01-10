package org.sokybot.machine.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.model.ServerFeed;
import org.sokybot.machine.model.UserAction;
import org.sokybot.machine.network.PacketListener;
import org.sokybot.machine.network.PacketListener.PacketSource;
import org.sokybot.machine.service.IAuthenticationService;
import org.sokybot.machinegroup.gamemodel.login.AgentServer;
import org.sokybot.machinegroup.gamemodel.login.AuthenticationErrorCode;
import org.sokybot.machinegroup.gamemodel.login.LoginBlockType;
import org.sokybot.machinegroup.gamemodel.login.LoginErrorCode;
import org.sokybot.settings.BotType;
import org.sokybot.settings.Settings;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.annotation.EventHeader;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Controller;

@Controller
@WithStateMachine
public class AuthenticationController {

	@Autowired
	private IAuthenticationService authenticationService;

	@Autowired
	private Settings config;

	@Autowired
	private Logger log;

	private String userName;
	private String password;
	private int loginId;

	@StateEntry(source = MachineState.VERIFYING, target = MachineState.DISCOVERING)
	public void discoveringAgents() {
		if (this.config.getBotType() == BotType.CLIENTLESS) {

			log.info("Discovering agent servers ");
			this.authenticationService.discoverAgents();

		}
	}

	@PacketListener(opcode = ClientOpcode.LOGIN_REQUEST)
	public void manualLogin(ImmutablePacket packet) {

		IStreamReader reader = packet.getStreamReader();

		reader.skip(1); // local
		this.userName = new String(reader.getBytes(reader.getShort()));
		this.password = new String(reader.getBytes(reader.getShort()));
		// keep this info for authenticating
	}

	@StateEntry(source = MachineState.DISCOVERING, target = MachineState.LOGINING)
	public void autologin(@EventHeader("model") List<AgentServer> agents) {
		if (config.isAutoLogin()) {
			log.info("Perform logining");
			agents.stream()
					.filter((agent) -> agent.getServerName().equalsIgnoreCase(config.getTargetAgent()))
					.findFirst()
					.ifPresentOrElse((agent) -> {
						this.authenticationService.login(config.getUsername(), config.getPassword(),
								agent.getServerId());
						this.userName = config.getUsername();
						this.password = config.getPassword();

					}, () -> {

						log.info("Could not perform auto login ");
					});
		}
	}

	@StateEntry(source = MachineState.IDENTIFYING, target = MachineState.AUTHENTICATING)
	public void authenticating() {
		log.info("Authenticating login ");
		this.authenticationService.authenticate(this.userName, this.password, this.loginId);

	}

	@PacketListener(opcode = ServerOpcode.AGENT_LIST)
	public List<AgentServer> onAgentListed(ImmutablePacket packet) {

		List<AgentServer> resList = new ArrayList<>();

		byte hasEntity;

		IStreamReader reader = packet.getPacketReader().asStreamReader();
		hasEntity = reader.getByte();

		if (hasEntity == 0x01) {
			reader.getByte();
			short farmSize = reader.getShort();
			String farmName = new String(reader.getBytes(farmSize));
			reader.getByte(); // spirator..
			hasEntity = reader.getByte();

			while (hasEntity == 0x01) {
				AgentServer agent = AgentServer.builder()
						.serverId(reader.getShort())
						.serverName(new String(reader.getBytes(reader.getShort())))
						.onlineUsers(reader.getShort())
						.maxUsers(reader.getShort())
						.operating(reader.getByte())
						.build();

				reader.getByte(); // fram id
				hasEntity = reader.getByte();
				resList.add(agent);
			}

		}

		return resList;
	}

	@PacketListener(opcode = ServerOpcode.LOGIN_RESPONSE)
	public Message<IMachineEvent> onLogined(ImmutablePacket packet) {

		IStreamReader reader = packet.getStreamReader();
		IMachineEvent event;

		byte loginResult = reader.getByte();

		if (loginResult == 0x01) {

			this.loginId = reader.getInt(); // keep login id for authentication
			String agentHost = reader.getString();
			short agentPort = reader.getShort();

			// parsing this packet and send login_SUCCESS event
			event = ServerFeed.LOGIN_SUCCESS;
			return MessageBuilder.withPayload(event)
					.setHeader(AppConstants.MACHINE_AGENT_HOST, agentHost)
					.setHeader(AppConstants.MACHINE_AGENT_PORT, agentPort)
					.setHeader(AppConstants.MACHINE_LOGIN_ID, this.loginId)
					.build();

		} else if (loginResult == 0x02) {

			LoginErrorCode error = LoginErrorCode.getError(reader.getByte());
			if (error == LoginErrorCode.Blocked) {
				LoginBlockType blockType = LoginBlockType.getBlockType(reader.getByte());
				event = blockType;
				// if (blockType == LoginBlockType.Punishment) {

				// IMachineEvent event = blockType;
				// we can parse packet here to get more information about punishment
				// but currently i will ignore it
				// this.machine.sendEvent(MessageBuilder.withPayload(event).setHeader("packet",
				// packet).build());

				// } else {
				// this.machine.sendEvent(blockType);
				// }
			} else {
				event = error;
			}

		} else {
			log.info("Unknown login result {} ", Integer.toHexString(loginResult));
			event = UserAction.DISCONNECT;
		}

		return MessageBuilder.withPayload(event).build();

	}

	@PacketListener(opcode = ServerOpcode.AUTH_RESPONSE)
	public Message<IMachineEvent> onAuthentication(ImmutablePacket packet) {

		IStreamReader reader = packet.getStreamReader();
		IMachineEvent event;
		byte result = reader.getByte();
		if (result == 0x02) {
			log.info("Authentication faild");
			event = AuthenticationErrorCode.getError(reader.getByte());
		} else {
			log.info("Login Authenticated successuffly");
			event = ServerFeed.AUTHENTICATED;
		}

		return MessageBuilder.withPayload(event).build();

	}

}
