package org.sokybot.packetsniffer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.packetsniffer.packetanalyzer.PacketAnalyzer;
import org.sokybot.packetsniffer.packetanalyzer.PacketVar;
import org.sokybot.packetsniffer.packettracer.PacketTracerModel;
import org.sokybot.packetsniffer.storage.JsonPacketStorage;
import org.sokybot.packetsniffer.packettracer.PacketTracerTableModel;
import org.sokybot.packetsniffer.trafficmonitor.PacketMonitorTableModel;
import org.sokybot.packetsniffer.trafficmonitor.TablePacket;
import org.sokybot.packetsniffer.trafficmonitor.TrafficMonitor;

public class PacketSnifferService implements IPacketSnifferService {

	private PacketMonitorTableModel monitor;

	private PacketTracerTableModel tracers;

	private List<PacketVar> packetVars;

	private JsonPacketStorage packetStorage;

	private boolean monitorEnabled = true;

	private ReentrantLock lock = new ReentrantLock();

	public PacketSnifferService(PacketMonitorTableModel monitorModel, PacketTracerTableModel tracerModel,
			JsonPacketStorage packetStorage, List<PacketVar> packetVars) {

		this.monitor = monitorModel;
		this.tracers = tracerModel;
		this.packetStorage = packetStorage;
		this.packetVars = packetVars;
		
		// Load existing tracers
		List<PacketTracerModel> savedTracers = packetStorage.load();
		for(PacketTracerModel tracer : savedTracers) {
			this.tracers.addTracer(tracer);
		}
	}

	@Override
	public void analyze() {

		TablePacket[] packets = this.monitor.toArray();
		if (packets.length > 0)
			PacketAnalyzer.showPacketAnalyzer(packets, this.packetVars);

	}

	@Override
	public void ignore(NetworkPeer source, int opcode) {

		this.tracers.setIgnored(source, opcode);
		PacketTracerModel tracer = this.tracers.getTracer(source, opcode);
		if(tracer != null) {
			saveOrUpdate(tracer);
		}
	}

	@Override
	public void clearMonitor() {

		this.monitor.clear();

	}

	@Override
	public void display(ImmutablePacket packet) {

		try {
			lock.lock();
			int opcode = packet.getOpcode();

			PacketTracerModel tracer = null;

			if (!this.tracers.containsTracer(packet.getPacketSource(), opcode)) {

				tracer = new PacketTracerModel(packet.getPacketSource(), opcode);
				tracer.setIgnored(false);
				tracer.setName("UNKNOWN");

				tracer.setCount(1);
				this.tracers.addTracer(tracer);
				saveOrUpdate(tracer);

			} else {

				// we need to update packet count field
				tracer = this.tracers.getTracer(packet.getPacketSource(), opcode);
				this.tracers.incPacketCount(packet.getPacketSource(), opcode);

			}

			if (!tracer.isIgnored() && monitorEnabled)
				this.monitor.addRow(TablePacket.createTablePacket(tracer.getName(), packet));

		} finally {
			lock.unlock();
		}
	}

	@Override
	public void pasueMonitoring() {
		this.monitorEnabled = false;
	}

	@Override
	public void resumeMonitoring() {

		this.monitorEnabled = true;
	}

	@Override
	public void saveOrUpdate(PacketTracerModel tracer) {
		if (tracer != null) {
			this.packetStorage.save(this.tracers.getTracers());
		}
	}

	public static void main(String args[]) {

		JFrame frame = new JFrame();

		JPanel content = (JPanel) frame.getContentPane();

		content.setLayout(new BorderLayout());

		PacketMonitorTableModel tableModel = new PacketMonitorTableModel();
		content.add(new TrafficMonitor(tableModel), BorderLayout.CENTER);

		frame.setPreferredSize(new Dimension(400, 400));
		frame.pack();
		frame.setVisible(true);

	}

}
