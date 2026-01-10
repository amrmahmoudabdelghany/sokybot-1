package org.sokybot.machineui.page.item;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.settings.ItemAction;
import org.sokybot.machinegroup.gamemodel.Gender;

public class ItemTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private String[] cols = { "Name", "Race", "Gender", "Level", "Action" };
    private List<TableEntry> rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return cols.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TableEntry entry = rows.get(rowIndex);
        ItemEntity item = entry.getItemEntity();

        switch (columnIndex) {
            case 0: return item.getName();
            case 1: return item.getRace().name();
            case 2: return item.getGender().name();
            case 3: return String.valueOf(item.getLevel());
            case 4: return entry.getAction();
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 4 && aValue instanceof ItemAction) {
            rows.get(rowIndex).setAction((ItemAction) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 4;
    }

    public void add(ItemEntity item) {
        rows.add(new TableEntry(item));
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }
    
    public void add(TableEntry entry) {
        rows.add(entry);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public ItemEntity getRepresentedItem(int rowIndex) {
        return rows.get(rowIndex).getItemEntity();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 4) return ItemAction.class;
        return String.class;
    }

    @Override
    public String getColumnName(int column) {
        return cols[column];
    }
    
    public void clear() {
        int size = rows.size();
        rows.clear();
        if (size > 0) fireTableRowsDeleted(0, size - 1);
    }
}
