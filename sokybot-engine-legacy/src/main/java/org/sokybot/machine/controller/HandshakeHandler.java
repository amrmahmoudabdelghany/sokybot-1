package org.sokybot.machine.controller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.machine.model.UserAction;
import org.sokybot.machine.network.NetworkAttributes;
import org.sokybot.settings.BotType;
import org.sokybot.settings.Settings;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.IPacketReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.security.IBlowfish;
import org.sokybot.security.ICRCSecurity;
import org.sokybot.security.ICountSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.EventHeader;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Controller;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.group.ChannelGroup;

//@Controller
//@WithStateMachine
public class HandshakeHandler {

	@Autowired
	private IBlowfish blowfish;

	@Autowired
	private ICRCSecurity crcSecurity;

	@Autowired
	private ICountSecurity countSecurity;

	@Autowired
	private Logger log;

	private int clientSecret;
	private int serverSecret;
	private int sharedSecret;
	private final int RND = 0x33;

	private byte[] finalKey = null;

	private ByteBuffer privateKey;

	@Autowired
	private StateMachine<MachineState, IMachineEvent> dispatcher;

	@Autowired
	ChannelGroup channelGroup;

	@Autowired
	private Settings config;

	//@StateEntry(source = {MachineState.CONNECTING , MachineState.REDIRECTING}, target = MachineState.HANDSHAKING)
	public void setupSecurityProtocol(@EventHeader(name = "packet", required = true) ImmutablePacket setupPacket) {
		log.info("setup security protocol");
		// Setup Security Count and CRC
		IPacketReader reader = setupPacket.getPacketReader();
		int countSeed = reader.readInt(9);
		int crcSeed = reader.readInt(13);
		int finalKeySeed1 = reader.readInt(17);
		int finalKeySeed2 = reader.readInt(21);
		int primitiveRoot = reader.readInt(25);
		int primaryNumber = reader.readInt(29);
		this.serverSecret = reader.readInt(33);

		this.crcSecurity.configur(crcSeed);
		this.countSecurity.configur(countSeed);

		this.clientSecret = (int) generateSecrets(primitiveRoot, RND, primaryNumber);

		this.sharedSecret = (int) generateSecrets(serverSecret, RND, primaryNumber);

		// set Final Key

		ByteBuffer finalKeySeeds = ByteBuffer.allocate(8);
		finalKeySeeds.order(ByteOrder.LITTLE_ENDIAN);
		finalKeySeeds.putInt(finalKeySeed1);
		finalKeySeeds.putInt(finalKeySeed2);

		ByteBuffer sharredSecretBuffer = ByteBuffer.allocate(4);
		sharredSecretBuffer.order(ByteOrder.LITTLE_ENDIAN);
		sharredSecretBuffer.putInt(sharedSecret);

		transformValue(finalKeySeeds.array(), sharredSecretBuffer.array(), (byte) 0x3);

		this.finalKey = finalKeySeeds.array();
		if(this.config.getBotType()  == BotType.CLIENTLESS) { 
			log.info("Authenticating connection");
		// generate Private Key
		byte PK_KeyByte = (byte) (this.sharedSecret & 0x03);
		this.privateKey = ByteBuffer.allocate(8);
		this.privateKey.order(ByteOrder.LITTLE_ENDIAN);
		this.privateKey.putInt(this.serverSecret);
		this.privateKey.putInt(this.clientSecret);

		transformValue(privateKey.array(), sharredSecretBuffer.array(), PK_KeyByte);

		blowfish.configur(privateKey.array());

		// generate Private Key Data
		byte PD_KeyByte = (byte) (this.clientSecret & 0x07);

		ByteBuffer privateData = ByteBuffer.allocate(8);
		privateData.order(ByteOrder.LITTLE_ENDIAN);
		privateData.putInt(this.clientSecret);
		privateData.putInt(this.serverSecret);

		transformValue(privateData.array(), sharredSecretBuffer.array(), PD_KeyByte);

		blowfish.encode(0, privateData.array());


		// reset Secrets
		// this.serverSecret = packet.getServerSecret();
		this.clientSecret = (int) generateSecrets(primitiveRoot, RND, primaryNumber);
		this.sharedSecret = (int) generateSecrets(this.serverSecret, RND, primaryNumber);

		ByteBuffer authPacket = ByteBuffer.allocate(12);
		authPacket.order(ByteOrder.LITTLE_ENDIAN);
		authPacket.putInt(clientSecret);
		authPacket.put(privateData.array());

		MutablePacket packet = MutablePacket.getBuilder(12 , 0x5000).putBytes(authPacket.array()).build();
		//packet.setPacketEncoding(Encoding.PLAIN);
		//packet.setDataEncoding(Encoding.PLAIN);
		//packet.setPacketSource(NetworkPeer.BOT);
	//	packet.setOpcode((short) 0x5000);
	//	packet.setPacketSize((short) 0x0c);
		log.info("Bot Authentication packet {} " , packet);
		this.channelGroup.writeAndFlush(packet,
				(ctx) -> ctx.attr(NetworkAttributes.TRANSPORT).get() == NetworkPeer.SERVER);
	}
		this.blowfish.configur(this.finalKey);

	}

	//@StateEntry(source = MachineState.HANDSHAKING, target = MachineState.CHALENGING)
	public void challengeServer(@EventHeader(name = "packet", required = true) ImmutablePacket challengePacket) {

		if (config.getBotType() == BotType.CLIENTLESS) {
			log.info("Challenging server...");
			IPacketReader reader = challengePacket.getPacketReader();

			ByteBuffer actualPrivateData = ByteBuffer.wrap(reader.readBytes(1, 8));

			ByteBuffer expectedPrivateData = ByteBuffer.allocate(8);
			expectedPrivateData.order(ByteOrder.LITTLE_ENDIAN);
			expectedPrivateData.putInt(this.serverSecret);
			expectedPrivateData.putInt(this.clientSecret);

			byte PD_KeyByte = (byte) (this.serverSecret & 0x07);

			ByteBuffer sharredSecretBuffer = ByteBuffer.allocate(4);
			sharredSecretBuffer.order(ByteOrder.LITTLE_ENDIAN);
			sharredSecretBuffer.putInt(sharedSecret);

			transformValue(expectedPrivateData.array(), sharredSecretBuffer.array(), PD_KeyByte);

			this.blowfish.configur(this.privateKey.array());
			byte[] encoded = this.blowfish.encode(0, expectedPrivateData.array());

			this.blowfish.configur(this.finalKey);

			expectedPrivateData = ByteBuffer.wrap(encoded);

			if (expectedPrivateData.equals(actualPrivateData)) {

				MutablePacket acceptancePacket = MutablePacket.wrap(new byte[6]);
				acceptancePacket.setPacketSize((short) 0x00);
				acceptancePacket.setOpcode((short) 0x9000);
				acceptancePacket.setPacketEncoding(Encoding.PLAIN);
				acceptancePacket.setDataEncoding(Encoding.PLAIN);
				acceptancePacket.setPacketSource(NetworkPeer.BOT);
				this.channelGroup.writeAndFlush(acceptancePacket,
						(ctx) -> ctx.attr(NetworkAttributes.TRANSPORT).get() == NetworkPeer.SERVER);

				log.debug("Final Blowfish Key : " + ByteBufUtil.hexDump(this.finalKey));

				log.info("Authentication Accepted");

				this.dispatcher.sendEvent(ClientFeed.CONNECTION_ACCEPTED);

			} else {

				log.info("Authentication  refused");
				this.dispatcher.sendEvent(UserAction.DISCONNECT);

				//this.dispatcher.sendEvent(ClientFeed.CONNECTION_REFUSED);

			}
		}
	}

	private long generateSecrets(long g, int x, long p) {

		long result = 1;
		long mult = g;
		if (x == 0)
			return 0;

		while (x > 0) {

			int y = x & 1;
			if (1 == y) {

				result = (mult * result) % p;

			}
			x = x >> 1;
			mult = (mult * mult) % p;

		}

		return result;
	}

	private void transformValue(byte[] stream, byte[] Dkey, byte keyByte) {

		stream[0] ^= (byte) (stream[0] + Dkey[0] + keyByte);
		stream[1] ^= (byte) (stream[1] + Dkey[1] + keyByte);
		stream[2] ^= (byte) (stream[2] + Dkey[2] + keyByte);
		stream[3] ^= (byte) (stream[3] + Dkey[3] + keyByte);

		stream[4] ^= (byte) (stream[4] + Dkey[0] + keyByte);
		stream[5] ^= (byte) (stream[5] + Dkey[1] + keyByte);
		stream[6] ^= (byte) (stream[6] + Dkey[2] + keyByte);
		stream[7] ^= (byte) (stream[7] + Dkey[3] + keyByte);
	}

}
