package org.sokybot.machine.page.trainingpage.monsterpreference;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.sokybot.machinegroup.gamemodel.setting.MonsterPreference;
import org.sokybot.machinegroup.gamemodel.setting.Settings;

public class NamePreferencesTableModel extends AbstractTableModel {

	private final String[] cols = { "Name", "Lvl", "Action" };
	private final List<TableEntry> data = new ArrayList<>();
	private final Settings settings;

	public NamePreferencesTableModel(Settings settings) {
		this.settings = settings;
	}

	@Override
	public int getRowCount() {
		return this.data.size();
	}

	@Override
	public int getColumnCount() {

		return this.cols.length;
	}

	public void addEntry(String name, String lvl, MonsterPreference preference) {
		TableEntry entry = new TableEntry();
		entry.name = name;
		entry.lvl = lvl;
		entry.preference = preference;
		this.data.add(entry);
		int index = this.data.indexOf(entry);
		fireTableRowsInserted(index, index);

	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {

		TableEntry entry = this.data.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return entry.name;
		case 1:
			return entry.lvl;
		case 2:
			return entry.preference;
		default:
			return "UNKNOWN";
		}

	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {

		return (columnIndex == 2);
	}

	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	 
		if(columnIndex == 2 && aValue instanceof MonsterPreference) { 
			
			TableEntry entry = this.data.get(rowIndex) ; 
			entry.preference = (MonsterPreference) aValue ; 
			this.settings.setMonsterPreference(entry.name, entry.preference);
		}
		
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {

		switch (columnIndex) {
		case 0:
			return String.class;
		case 1:
			return String.class;
		case 2:
			return MonsterPreference.class;

		default:
			return Object.class;
		}
	}

	@Override
	public String getColumnName(int column) {

		return this.cols[column];
	}

	public void clear() {
		this.data.clear();
		fireTableDataChanged();
	}

	private static class TableEntry {
		String name;
		String lvl;
		MonsterPreference preference;

	}

}
