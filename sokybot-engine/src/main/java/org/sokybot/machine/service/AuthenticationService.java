package org.sokybot.machine.service;


import org.slf4j.Logger;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;


@Service
public class AuthenticationService implements IAuthenticationService {

	@Autowired
	private IGameDataLookup gameDAO;


	@Autowired
	private IConnectionManager connectionManager ; 
	
	@Autowired
	private StateMachine<MachineState, IMachineEvent> machine;

	@Autowired
	private Logger log;



	@Override
	public void discoverAgents() {
		MutablePacket sharedRequest = MutablePacket.getBuilder(0, ClientOpcode.AGENT_REQUEST)
				.packetEncoding(Encoding.ENCRYPTED)
				.dataEncoding(Encoding.PLAIN)
				.packetSource(NetworkPeer.BOT)
				.build();
		this.connectionManager.writeToServer(sharedRequest);
		
		
	}
	
	@Override
	public void authenticate(String userName , String password , int loginId) {
		this.log.info("Authenticating login...");

		int packetLen = 15 + userName.length() + password.length();

		MutablePacket authLogin = MutablePacket
				.getBuilder(packetLen , ClientOpcode.AUTH_REQUEST)
				.packetEncoding(Encoding.ENCRYPTED)
				.dataEncoding(Encoding.PLAIN)
				.packetSource(NetworkPeer.BOT)
				.putInt(loginId)
				.putShort((short) userName.length())
				.putBytes(userName.toLowerCase().getBytes())
				.putShort((short) password.length())
				.putBytes(password.getBytes())
				.put(this.gameDAO.getLocal().orElse((byte)22))
				.putBytes(new byte[6]) // we can ignore this																								// statement but for																				// readability
				.build();

		
		
		this.connectionManager.writeToServer(authLogin);

	}

	@Override
	public void login(String userName, String password, short agentId) {

		
			// int agentId = agentServer.getServerId();

			int packetLen = 7 + userName.length() + password.length();

			MutablePacket loginPacket = MutablePacket.getBuilder(packetLen, ClientOpcode.LOGIN_REQUEST)
					.packetEncoding(Encoding.ENCRYPTED)
					.dataEncoding(Encoding.PLAIN)
					.packetSource(NetworkPeer.BOT)
					.put(this.gameDAO.getLocal().orElse((byte)22))
					.putShort((short) userName.length())
					.putBytes(userName.getBytes())
					.putShort((short) password.length())
					.putBytes(password.getBytes())
					.putShort(agentId )
					.build();

			this.connectionManager.writeToServer(loginPacket) ; 
					

	}



	@Override
	public void logout() {
		// TODO Auto-generated method stub

	}


}
