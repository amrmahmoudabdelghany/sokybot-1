package org.sokybot.machine.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.commons.SilkroadUtils;

import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class SecurityConfig {

	private final int clientSecret;
	private final int serverSecret;
	private final int sharedSecret;

	private byte[] sharedSecretBuffer() {
		ByteBuffer sharredSecretBuffer = ByteBuffer.allocate(4);
		sharredSecretBuffer.order(ByteOrder.LITTLE_ENDIAN);
		sharredSecretBuffer.putInt(sharedSecret);

		return sharredSecretBuffer.array();
	}

	public byte[] getPrivateKey() {

		byte PK_KeyByte = (byte) (this.sharedSecret & 0x03);
		ByteBuffer privateKey = ByteBuffer.allocate(8);
		privateKey.order(ByteOrder.LITTLE_ENDIAN);
		privateKey.putInt(this.serverSecret);
		privateKey.putInt(this.clientSecret);

		SilkroadUtils.transformValue(privateKey.array(), sharedSecretBuffer(), PK_KeyByte);

		return privateKey.array();
	}

	public byte[] getClientPrivateData() {
		byte PD_KeyByte = (byte) (this.clientSecret & 0x07);

		ByteBuffer privateData = ByteBuffer.allocate(8);
		privateData.order(ByteOrder.LITTLE_ENDIAN);
		privateData.putInt(this.clientSecret);
		privateData.putInt(this.serverSecret);

		SilkroadUtils.transformValue(privateData.array(), sharedSecretBuffer(), PD_KeyByte);

		return privateData.array();
	}

	public byte[] getServerPrivateData() {

		byte PD_KeyByte = (byte) (this.serverSecret & 0x07);

		ByteBuffer privateData = ByteBuffer.allocate(8);
		privateData.order(ByteOrder.LITTLE_ENDIAN);
		privateData.putInt(this.serverSecret);
		privateData.putInt(this.clientSecret);

		SilkroadUtils.transformValue(privateData.array(), sharedSecretBuffer(), PD_KeyByte);
		return privateData.array();
	}

}
