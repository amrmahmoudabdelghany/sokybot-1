package org.sokybot.packetsniffer.packettracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javax.swing.table.AbstractTableModel;

import org.sokybot.network.NetworkPeer;

public class PacketTracerTableModel extends AbstractTableModel {

	private String[] columns = { "Ignored", "Source", "Name", "Opcode", "Packet Count" };

	private final Vector<PacketTracerModel> data = new Vector<>();
	private final Map<String, PacketTracerModel> models = new ConcurrentHashMap<String, PacketTracerModel>();

	// private Map<Integer, PacketTracerModel> rows = new ConcurrentHashMap<>();

	// private Map<Integer, Integer> opcodeIndex = new ConcurrentHashMap<>();
	// private Map<Integer, Integer> indexOpcode = new ConcurrentHashMap<>();


	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return columns.length;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {

		if (rowIndex >= this.data.size())
			throw new IndexOutOfBoundsException(rowIndex);

		PacketTracerModel model = this.data.get(rowIndex);

		if (model != null) {

			switch (columnIndex) {

			case 0:
				return model.isIgnored();
			case 1:
				return model.getSource();
			case 2:
				return model.getName();
			case 3:
				return "0x" + Integer.toHexString(model.getOpcode() & 0xffff);
			case 4:
				return model.getCount();

			}

		}

		return null;
	}

	@Override
	public String getColumnName(int column) {
		return this.columns[column];
	}

	public void addTracer(PacketTracerModel tracer) {

		String key = tracer.getSource().name() + "." + Integer.toHexString(tracer.getOpcode());

		if (this.models.containsKey(key))
			throw new IllegalArgumentException("Trying to add Packet Tracer that already exists");

		
		this.data.add(tracer);

		this.models.put(key, tracer) ; 
		
		int index = this.data.indexOf(tracer) ; 
		
		
		this.fireTableRowsInserted(index , index);
		
		System.out.println("Tracer : " + tracer  + " added successfully") ; 
	}

	public boolean containsTracer(NetworkPeer source, int opcode) {

		return this.models.containsKey(source.name() + "." + Integer.toHexString(opcode));
	}

	public PacketTracerModel getTracer(NetworkPeer source, int opcode) {

		String key = source.name() + "." + Integer.toHexString(opcode);

		return this.models.get(key);
	}

	public void incPacketCount(NetworkPeer source, int opcode) {

		if (!containsTracer(source, opcode)) {

			throw new IllegalArgumentException("Trying to increment packet count for tracer that does`nt exists.");
		}

		PacketTracerModel model = getTracer(source, opcode);

		int count = model.getCount();
		model.setCount((count + 1));

		int index = this.data.indexOf(model) ; 
		
 
		this.fireTableCellUpdated(index, 4);
	}

	public String getName(NetworkPeer source, int opcode) {

		if (!containsTracer(source, opcode))
			throw new IllegalArgumentException(
					"No such Packet Tracer for " + source.name() + "." + Integer.toHexString(opcode));

		PacketTracerModel model = getTracer(source, opcode);

		return model.getName();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {

		switch (columnIndex) {
		case 0:
			return Boolean.class;
		case 1:
			return NetworkPeer.class;
		case 2:

		case 3:
		case 4:
			return String.class;
		default:
			break;
		}

		return Object.class;
	}

	public PacketTracerModel getByIndex(int index) {

		if (index >= this.data.size())
			throw new IndexOutOfBoundsException(index);

		return this.data.get(index);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {

		return (columnIndex == 0 || columnIndex == 2);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

		if (rowIndex >= this.data.size())
			throw new IndexOutOfBoundsException(rowIndex);

		if (columnIndex >= this.columns.length)
			throw new IndexOutOfBoundsException(columnIndex);

		PacketTracerModel model = this.data.get(rowIndex);
		if (model != null) {
			if (columnIndex == 0 && aValue instanceof Boolean) {
				model.setIgnored((Boolean) aValue);
			} else if (columnIndex == 2 && aValue instanceof String) {
				model.setName((String) aValue);
			}

		}
	}

	public void setIgnored(NetworkPeer source, int opcode) {

		if (!containsTracer(source, opcode))
			throw new IllegalArgumentException(
					"No such Packet Tracer for " + source.name() + "." + Integer.toHexString(opcode));

		PacketTracerModel model = getTracer(source, opcode);

		model.setIgnored(true);

		 this.fireTableDataChanged();

	}

}
