package org.sokybot.machine.service;

import static org.sokybot.app.AppConstants.CLIENT_MODULE_NAME;

import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatchService  implements IPatchService{

	@Autowired
	private IConnectionManager connectionManager ; 
	
	@Override
	public void verify(byte local, int version) {
		int packetSize = 7 + CLIENT_MODULE_NAME.length();

		MutablePacket patchRequestPacket = MutablePacket.getBuilder(packetSize, 0x6100)
				.packetEncoding(Encoding.ENCRYPTED)
				.dataEncoding(Encoding.PLAIN)
				.put(local)
				.putShort((short) CLIENT_MODULE_NAME.length())
				.putBytes(CLIENT_MODULE_NAME.getBytes())
				.putInt(version)
				.build();
		this.connectionManager.writeToServer(patchRequestPacket);

			
	}
}
