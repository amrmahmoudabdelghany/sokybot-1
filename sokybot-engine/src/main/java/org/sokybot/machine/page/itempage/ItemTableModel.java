package org.sokybot.machine.page.itempage;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.sokybot.machinegroup.gamemodel.Gender;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.machinegroup.gamemodel.setting.ItemAction;
// import org.springframework.stereotype.Component;

// @Component
public class ItemTableModel extends AbstractTableModel {

	private String[] cols = { "Name", "Race", "Gender", "Level", "Action" };
	private List<TableEntry> rows = new ArrayList<>();

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {

		TableEntry entry = rows.get(rowIndex);
		ItemEntity item =  entry.getItemEntity() ; 
		
 		switch (columnIndex) {

		case 0: return item.getName() ; 
		case 1 : return item.getRace().name() ; 
		case 2 : return item.getGender().name() ; 
		case 3 : return String.valueOf(item.getLevel()) ; 
		case 4 : return entry.getAction().name() ; 
		default:
			return null;
		}

	}
	
	public void add(ItemEntity item) { 
		rows.add(new TableEntry(item, ItemAction.IGNORE)) ;
		fireTableStructureChanged();
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {	
		return (columnIndex == cols.length - 1) ; 
	}
	

	public ItemEntity getRepresentedItem(int rowIndex) { 
		return this.rows.get(rowIndex).getItemEntity() ;
		
	}
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if(columnIndex == cols.length - 1) return ItemAction.class ; 
		if(columnIndex == 2 ) return Gender.class ; 
		return String.class; 
	}
	@Override
	public String getColumnName(int column) {
	
		return cols[column] ; 
	}
	@Override
	public int getColumnCount() {
		return cols.length;
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

}
