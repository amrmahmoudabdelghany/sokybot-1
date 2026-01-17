package org.sokybot.game.dto;

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
