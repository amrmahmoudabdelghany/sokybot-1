package org.sokybot.packetsniffer.trafficmonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;


public class PacketMonitorTableModel extends AbstractTableModel {

	// private List<ImmutablePacket> packets = new ArrayList<>();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Vector<TablePacket> packets = new Vector<>();
	private String[] columns = { "Source", "Packet Encoding", "Size", "Name", "Opcode", "Count", "CRC" };

	public PacketMonitorTableModel() {

		// adding empty row so
		packets.add(null);
	}

	@Override
	public int getColumnCount() {
		return this.columns.length;
	}

	@Override
	public int getRowCount() {

		return packets.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		TablePacket packet = this.packets.get(rowIndex);
		if (packet == null) {
			return "";
		}
		switch (columnIndex) {
		case 0:
			return packet.getPacket().getPacketSource();
		case 1:
			return packet.getPacket().getPacketEncoding();
		case 2:
			return packet.getPacket().getPacketSize();
		case 3:

			return packet.getName();
		case 4:
			return "0x" + Integer.toHexString(packet.getPacket().getOpcode() & 0xffff);
		case 5:
			return "0x" + Integer.toHexString(packet.getPacket().getCount() & 0xff);

		case 6:
			return "0x" + Integer.toHexString(packet.getPacket().getCRC() & 0xff);

		}
		return null;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {

		switch (columnIndex) {
		case 0:
			return NetworkPeer.class;
		case 1:
			return Encoding.class;

		case 2:
		case 3:
		case 4:
		case 5:
			return String.class;

		}
		return Object.class;
	}

	@Override
	public String getColumnName(int column) {

		if (column >= this.columns.length)
			return null;

		return this.columns[column];
	}

	public void clear() {
		int len = this.packets.size();
		this.packets.clear();
		this.packets.add(null);
		super.fireTableRowsDeleted(0, len - 1);

	}

	public TablePacket getRow(int rowIndex) {

		if (rowIndex >= this.packets.size() || rowIndex < 0) {
			return null;
		}

		return this.packets.get(rowIndex);
	}

	public int getPacketCount() { 
		return getRowCount() - 1 ; 
	}
	public TablePacket[] toArray() { 
		TablePacket[] arr= new TablePacket[this.packets.size() - 1]  ; 
		
		for(int i = 0 ; i < arr.length ; i++) { 
			arr[i] = this.packets.get(i) ; 
		}
		
		return arr ; 
	}

	public void addRow(TablePacket packet) {
		//System.out.println("PacketMonitorTablemodel::addRow" + packet) ; 
		this.packets.insertElementAt(packet, this.packets.size() - 1); // insert at last
		// super.fireTableRowsUpdated(packets.size() - 1, packets.size() - 1);
		super.fireTableRowsInserted(this.packets.size() - 2, this.packets.size() - 2);

	}

}
