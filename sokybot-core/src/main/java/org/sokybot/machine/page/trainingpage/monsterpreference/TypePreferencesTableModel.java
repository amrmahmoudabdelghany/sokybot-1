package org.sokybot.machine.page.trainingpage.monsterpreference;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.sokybot.machinegroup.gamemodel.npc.MonsterType;
import org.sokybot.machinegroup.gamemodel.setting.MonsterPreference;
import org.sokybot.machinegroup.gamemodel.setting.Settings;

public class TypePreferencesTableModel extends AbstractTableModel {

	private String cols[] = { "Monster Type ", "Preference" };

	private final List<TableEntry> data = new ArrayList<>();

	private final Settings settings ; 
	
	
	protected TypePreferencesTableModel(Settings s) {
		this.settings = s ; 
		
	}

	@Override
	public int getColumnCount() {

		return cols.length;
	}

	@Override
	public int getRowCount() {

		return data.size();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {

		return (columnIndex == 1);
	}

	@Override
	public String getColumnName(int column) {
		return this.cols[column];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return MonsterType.class;
		case 1:
			return MonsterPreference.class;
		default:
			return Object.class;
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {

		TableEntry entry = this.data.get(rowIndex);
		if (columnIndex == 0) {
			return entry.name;
		} else if (columnIndex == 1) {
			return entry.monsterPreference;
		}
		return null;
	}
	
	public void addEntry(String name , MonsterType monsterType , MonsterPreference monsterPreference) { 
		TableEntry entry = new TableEntry() ; 
		entry.name = name ; 
		entry.type = monsterType ; 
		entry.monsterPreference = monsterPreference ; 
		this.data.add(entry) ; 
	}
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	  
		if(columnIndex == 1) { 
			if(aValue instanceof MonsterPreference) {
			 MonsterPreference newVal = (MonsterPreference) aValue ; 
			 TableEntry entry =	this.data.get(rowIndex) ; 
				entry.monsterPreference = newVal; 
				 this.settings.setMonsterPreference(entry.type, newVal); 
			}
		//	fireTableCellUpdated(rowIndex, columnIndex);
		}
		
	}

	private static  class TableEntry  { 
		String name ; 
		MonsterType type ; 
	
		MonsterPreference monsterPreference ; 

		protected TableEntry() {}
		
}
}