package org.sokybot.machine.service;

import org.sokybot.app.AppConstants;
import org.sokybot.machinegroup.gamemodel.chat.ChatType;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatManager implements IChatManager {

	@Autowired
	private IConnectionManager connectionManager; 
	
	
	@Override
	public void logMessage(String loggerName , String message) {
		byte chatType = ChatType.PM.getTypeValue() ;
		this.connectionManager.writeToClient(  MutablePacket
				.getBuilder(5 +loggerName.length() +  message.length()  , ServerOpcode.CHAT_UPDATE)
				.put(chatType)
				.putShort((short)loggerName.length())
				.putBytes(loggerName.getBytes())
				.putShort((short)message.length())
				.putBytes(message.getBytes())
				.build()) ; 
	}
}
