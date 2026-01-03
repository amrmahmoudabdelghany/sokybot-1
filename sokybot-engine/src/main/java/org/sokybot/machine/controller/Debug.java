package org.sokybot.machine.controller;

import org.slf4j.Logger;
import org.sokybot.machine.network.PacketListener;
import org.sokybot.network.packet.ImmutablePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class Debug {

	
	@Autowired
	private Logger log ;
	
	
	
	//@PacketListener(opcode = {0x300C , 0x3012 ,0x3080 , 0x35B5 , 0x35B6 , 0x34BE})
	public void observe(ImmutablePacket packet) { 
		log.info("Reciving {} " , packet) ; 
	}
	
	
	
	
}
