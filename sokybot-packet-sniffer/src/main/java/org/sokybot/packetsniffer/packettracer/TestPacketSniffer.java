package org.sokybot.packetsniffer.packettracer;

import java.awt.BorderLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.packetsniffer.PacketSniffer;

public class TestPacketSniffer {

	
	
	private static  List<Integer> opcodes = new ArrayList<>() ; 
	
	private static Random rnd = new Random() ; 

	static { 
		
		opcodes.add(0x5000); 
		opcodes.add(0x2000) ; 
		opcodes.add(0x9000) ; 
		opcodes.add(0xA100);
		opcodes.add(0x3011) ; 
		
		
	}
	 
	
	public static void main(String args[]) {

		JFrame frame = new JFrame();

		JPanel content = (JPanel) frame.getContentPane();

		content.setLayout(new BorderLayout());

		PacketSniffer ps = new PacketSniffer();
		content.add(ps, BorderLayout.CENTER);

		Executors.newSingleThreadScheduledExecutor()
				.scheduleAtFixedRate(() -> ps.onNext((short) rnd.nextInt(), rndPacket()), 0, 1, TimeUnit.SECONDS);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}

	private static ImmutablePacket rndPacket() {
		
		byte[] buff = new byte[6 + rnd.nextInt(40)];
		ByteBuffer buffer = ByteBuffer.wrap(buff);
		rnd.nextBytes(buff);
		buffer.order(ByteOrder.LITTLE_ENDIAN) ; 
		buffer.putShort(2, (short)rndOpcode()) ; 
		return ImmutablePacket.wrap(buff);
	}
	private static int rndOpcode() { 
		int index = rnd.nextInt(opcodes.size()) ; 
		return opcodes.get(index) ; 
	}

}
