/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sokybot.machinegroup.gamemodel.login ; 

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 *
 * @author AMROO
 */
@Builder
@Getter
@ToString
public class AgentServer {
	private short serverId;
	private String serverName;
	private int onlineUsers;
	private int maxUsers;
	private int operating;


	
	
}
