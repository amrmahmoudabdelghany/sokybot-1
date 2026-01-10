package org.sokybot.machineui.page.skill;

import java.util.Vector;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class SkillTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final Vector<SkillTableEntry> data = new Vector<>();

    public boolean contains(String skillName) {
        return data.stream()
                .anyMatch((s) -> s.getSkillName() != null && s.getSkillName().equals(skillName));
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void move(int index1, int index2) {
        if (index1 < 0 || index1 >= data.size() || index2 < 0 || index2 >= data.size())
            return;

        SkillTableEntry tmp = data.get(index1);
        data.set(index1, data.get(index2));
        data.set(index2, tmp);
        fireTableRowsUpdated(Math.min(index1, index2), Math.max(index1, index2));
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SkillTableEntry obj = data.get(rowIndex);
        if (columnIndex == 0) {
            return obj.getSkillName();
        } else if (columnIndex == 1) {
            return obj.getSkillLvl();
        }
        throw new IndexOutOfBoundsException(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0: return String.class;
        case 1: return Integer.class;
        }
        throw new IndexOutOfBoundsException(columnIndex);
    }

    public void removeRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < data.size()) {
            data.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    public void removeAll() {
        data.removeAllElements();
        fireTableDataChanged();
    }

    public void addSkill(SkillTableEntry skillEntry) {
        data.add(skillEntry);
        int index = data.indexOf(skillEntry);
        fireTableRowsInserted(index, index);
    }

    public void addAll(List<SkillTableEntry> skills) {
        data.addAll(skills);
        fireTableDataChanged();
    }

    public List<String> getAllSkillNames() {
        return data.stream().map(SkillTableEntry::getSkillName).collect(java.util.stream.Collectors.toList());
    }

    public SkillTableEntry getRowObject(int index) {
        if (index >= 0 && index < data.size()) {
            return data.get(index);
        }
        throw new IndexOutOfBoundsException(index);
    }
}
