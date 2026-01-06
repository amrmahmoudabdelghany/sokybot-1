package org.sokybot.packetsniffer;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.sokybot.packetsniffer.storage.JsonPacketStorage;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;
import org.sokybot.IGroupContext;
import org.sokybot.IGroupListener;
import org.sokybot.ISokybotContext;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.packetsniffer.packettracer.IPacketTracerHandler;
import org.sokybot.packetsniffer.packettracer.PacketTracer;
import org.sokybot.packetsniffer.packettracer.PacketTracerHandler;
import org.sokybot.packetsniffer.packettracer.PacketTracerModel;
import org.sokybot.packetsniffer.packettracer.PacketTracerTableModel;
import org.sokybot.packetsniffer.trafficmonitor.PacketMonitorHandler;
import org.sokybot.packetsniffer.trafficmonitor.PacketMonitorTableModel;
import org.sokybot.packetsniffer.trafficmonitor.TrafficMonitor;

public class PacketSniffer extends JTabbedPane implements IPacketObserver {

	private IPacketSnifferService service;
	private final PacketMonitorTableModel monitorModel = new PacketMonitorTableModel();
	private final PacketTracerTableModel tracerModel = new PacketTracerTableModel();
	private final TrafficMonitor monitor = new TrafficMonitor(monitorModel);
	private final PacketTracer packetTracer = new PacketTracer(tracerModel);



	private static final JsonPacketStorage storage;
 
	static { 
		storage = new JsonPacketStorage("./packet-data.json");
	}

	public PacketSniffer() {

		add("Traffic Monitor ", monitor);
		add("Packet Tracer ", packetTracer);

		this.service = new PacketSnifferService(monitorModel, tracerModel, storage,
				new ArrayList<>());
		this.monitor.setHandler(new PacketMonitorHandler(service));
		this.packetTracer.setHandler(new PacketTracerHandler(service));

	}

	@Override
	public void onNext(int opcode, ImmutablePacket packet) {
	
		this.service.display(packet);

	}

	@Override
	public void onComplete() {

	}

	@Override
	public void onError(Throwable ex) {

	}

	
	
	
}
