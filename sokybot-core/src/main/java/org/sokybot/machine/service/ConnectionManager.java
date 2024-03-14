package org.sokybot.machine.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.app.AppConstants;
import org.sokybot.machine.model.SecurityConfig;
import org.sokybot.machine.network.NetworkAttributes;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.IPacketReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.security.Blowfish;
import org.sokybot.security.IBlowfish;
import org.sokybot.security.ICRCSecurity;
import org.sokybot.security.ICountSecurity;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;

@Service
public class ConnectionManager implements IConnectionManager {

	private final ChannelMatcher clientMatcher = (
			channel) -> channel.attr(NetworkAttributes.TRANSPORT).get() == NetworkPeer.CLIENT;
	private final ChannelMatcher serverMatcher = (
			channel) -> channel.attr(NetworkAttributes.TRANSPORT).get() == NetworkPeer.SERVER;

	@Autowired
	private ChannelGroup channelGroup;

	@Autowired
	private ApplicationContext ctx;

	@Override
	public void writeToServer(MutablePacket packet) {
		this.channelGroup.writeAndFlush(packet, this.serverMatcher);

	}

	@Override
	public void writeToClient(MutablePacket packet) {
		this.channelGroup.writeAndFlush(packet, this.clientMatcher);

	}

	@Override
	public void authenticateConnection(SecurityConfig secConfig) {

		IBlowfish blowfish = new Blowfish();
		blowfish.configur(secConfig.getPrivateKey());
		byte[] privateData = blowfish.encode(0, secConfig.getClientPrivateData());

		MutablePacket packet = MutablePacket.getBuilder(12, 0x5000).putInt(secConfig.getClientSecret()).putBytes(privateData).build() ; 
		
		writeToServer(packet);

	}

	@Override
	public SecurityConfig setupSecurity(ImmutablePacket setupPacket) {
		// Setup Security Count and CRC
		IPacketReader reader = setupPacket.getPacketReader();
		int countSeed = reader.readInt(9);
		int crcSeed = reader.readInt(13);
		int finalKeySeed1 = reader.readInt(17);
		int finalKeySeed2 = reader.readInt(21);
		int primitiveRoot = reader.readInt(25);
		int primaryNumber = reader.readInt(29);
		int serverSecret = reader.readInt(33);

		this.ctx.getBean(ICRCSecurity.class).configur(crcSeed);
		this.ctx.getBean(ICountSecurity.class).configur(countSeed);

		int clientSecret = (int) generateSecrets(primitiveRoot, AppConstants.CLIENT_RND_SEED, primaryNumber);

		int sharedSecret = (int) generateSecrets(serverSecret, AppConstants.CLIENT_RND_SEED, primaryNumber);

		// set Final Key

		ByteBuffer finalKeySeeds = ByteBuffer.allocate(8);
		finalKeySeeds.order(ByteOrder.LITTLE_ENDIAN);
		finalKeySeeds.putInt(finalKeySeed1);
		finalKeySeeds.putInt(finalKeySeed2);

		ByteBuffer sharredSecretBuffer = ByteBuffer.allocate(4);
		sharredSecretBuffer.order(ByteOrder.LITTLE_ENDIAN);
		sharredSecretBuffer.putInt(sharedSecret);

		SilkroadUtils.transformValue(finalKeySeeds.array(), sharredSecretBuffer.array(), (byte) 0x3);

		byte[] finalKey = finalKeySeeds.array();
		this.ctx.getBean(IBlowfish.class).configur(finalKey);

		return new SecurityConfig(clientSecret, serverSecret, sharedSecret);

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

}
