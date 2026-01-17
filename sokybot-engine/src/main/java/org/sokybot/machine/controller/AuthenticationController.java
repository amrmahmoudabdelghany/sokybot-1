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
import org.sokybot.machine.service.IAuthenticationService;
import org.sokybot.game.dto.AgentServer;
import org.sokybot.settings.BotType;
import org.sokybot.settings.Settings;
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


	@org.springframework.context.event.EventListener
	public void onLoginRequest(org.sokybot.gameevents.events.session.LoginRequestEvent event) {
		this.userName = event.getUsername();
		this.password = event.getPassword();
		log.info("Manual login credentials updated: {}", this.userName);
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

}

