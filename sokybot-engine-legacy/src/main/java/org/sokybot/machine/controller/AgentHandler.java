package org.sokybot.machine.controller;

import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.service.IAuthenticationService;
import org.sokybot.machine.service.ICharacterService;
import org.sokybot.settings.BotType;
import org.sokybot.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Controller;

@WithStateMachine
public class AgentHandler {

	
	@Autowired
	private Logger log ; 
	
	
	
	@Autowired
	private ApplicationContext ctx ; 
	
	@Autowired
	private Settings config ;
	
	@Value("${" + AppConstants.MACHINE_NAME + "}")
	private String trainerName ; 
	
	
	@StateEntry(source =  MachineState.AUTHENTICATING , target =MachineState.LISTING)
	public void listingCharacters() {  
		if(this.config.getBotType() == BotType.CLIENTLESS) { 
			this.ctx.getBean(ICharacterService.class).listCharacters(); 
		}
	}
	
	
	
	@StateEntry(source =  MachineState.LISTING , target =MachineState.JOINING)
	public void joining() { 
		this.ctx.getBean(ICharacterService.class).joinCharacter(this.trainerName);
	}
	
	@StateEntry(source = MachineState.JOINING , target = MachineState.PLAYING)
	public void playing() { 
		if(this.config.getBotType() == BotType.CLIENTLESS) { 
			
			//TODO send game ready packet to server
		}
	}
}
