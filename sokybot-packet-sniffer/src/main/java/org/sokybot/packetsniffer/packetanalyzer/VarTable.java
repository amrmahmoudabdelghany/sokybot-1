package org.sokybot.packetsniffer.packetanalyzer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class VarTable extends AbstractTableModel {

	private List<PacketVar> packetVars;
	String columns[] = { "Packet", "Var", "Value", "Comment" };

	public VarTable(List<PacketVar> vars) {
		this.packetVars = vars;
	}

	@Override
	public int getColumnCount() {
		return columns.length;

	}

	@Override
	public int getRowCount() {
		return this.packetVars.size();
	}

	@Override
	public String getColumnName(int column) {
		return columns[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		PacketVar packet = this.packetVars.get(rowIndex);

		if (packet != null) {
			switch (columnIndex) {
			case 0:
				return packet.getPacket();
			case 1:
				return packet.getVarName();
			case 2:
				return packet.getFormatedValue();
			case 3:
				return packet.getComment();
			}
		}
		return null;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if(columns[columnIndex].equals("Var") || columns[columnIndex].equals("Comment")) { 
			return true ; 
		}
		return false ; 
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	     PacketVar packetVar = this.packetVars.get(rowIndex) ; 
	     
	     if(packetVar != null && aValue != null && aValue instanceof String) { 
	    	 switch(columnIndex) { 
	    	 case 1 : packetVar.setVarName((String)aValue);break ; 
	    	 case 3 : packetVar.setComment((String)aValue);break ; 
	    	 }
	     }
	}

	public void addPacketVar(PacketVar packetVar) {
		if (packetVar != null) {
			int index = this.packetVars.size();
			this.packetVars.add(packetVar);

			this.fireTableRowsInserted(index, index);
		}
	}
	
	public PacketVar getPacketVar(int index) { 
		return this.packetVars.get(index) ; 
	}

	
}
