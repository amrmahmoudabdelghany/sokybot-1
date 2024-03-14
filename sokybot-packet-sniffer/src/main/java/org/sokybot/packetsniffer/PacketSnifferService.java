package org.sokybot.packetsniffer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.packetsniffer.packetanalyzer.PacketAnalyzer;
import org.sokybot.packetsniffer.packetanalyzer.PacketVar;
import org.sokybot.packetsniffer.packettracer.PacketTracerModel;
import org.sokybot.packetsniffer.packettracer.PacketTracerTableModel;
import org.sokybot.packetsniffer.trafficmonitor.PacketMonitorTableModel;
import org.sokybot.packetsniffer.trafficmonitor.TablePacket;
import org.sokybot.packetsniffer.trafficmonitor.TrafficMonitor;

public class PacketSnifferService implements IPacketSnifferService {

	private PacketMonitorTableModel monitor;

	private PacketTracerTableModel tracers;

	private List<PacketVar> packetVars;

	private ObjectRepository<PacketTracerModel> tracersRepo;

	private boolean monitorEnabled = true;

	private ReentrantLock lock = new ReentrantLock();

	public PacketSnifferService(PacketMonitorTableModel monitorModel, PacketTracerTableModel tracerModel,
			ObjectRepository<PacketTracerModel> tracerRepo, List<PacketVar> packetVars) {

		this.monitor = monitorModel;
		this.tracers = tracerModel;
		this.tracersRepo = tracerRepo;
		this.packetVars = packetVars;
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

				// ObjectFilter filter = ObjectFilters.and(ObjectFilters.eq("opcode", opcode) ,
				// ObjectFilters.eq("source", packet.getPacketSource())) ;

				String id = packet.getPacketSource().name() + "." + Integer.toHexString(opcode);

				tracer = this.tracersRepo.find(ObjectFilters.eq("id", id)).firstOrDefault();

				if (tracer == null) {
					tracer = new PacketTracerModel(packet.getPacketSource(), opcode);
					tracer.setIgnored(false);
					tracer.setName("UNKNOWN");

				}

				tracer.setCount(1);
				this.tracers.addTracer(tracer);

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
			this.tracersRepo.update(tracer, true);
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
