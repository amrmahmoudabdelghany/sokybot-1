package org.sokybot.machine.page.skillpage;

import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.sokybot.machine.page.skillpage.SkillTab.SkillTableEntry;
import org.sokybot.machinegroup.gamemodel.skill.Skill;

public class SkillTableModel extends AbstractTableModel {

	private final Vector<SkillTableEntry> data = new Vector<>();

	protected SkillTableModel() {

	}

	public boolean contains(String  skillName) {
		return data.stream()
				.anyMatch((s) -> s.skillName != null && s.skillName.equals(skillName));
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
		// check validataion
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

			return obj.skillName ;
		} else if (columnIndex == 1) {
			return obj.skillLvl ; 
		}

		throw new IndexOutOfBoundsException(columnIndex);

	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {

		switch (columnIndex) {
		case 0:
			return String.class;
		case 1:
			return Integer.class;
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

	public SkillTableEntry getRowObject(int index) {
		if (index >= 0 && index < data.size()) {
			return data.get(index);
		}
		throw new IndexOutOfBoundsException(index);
	}

}