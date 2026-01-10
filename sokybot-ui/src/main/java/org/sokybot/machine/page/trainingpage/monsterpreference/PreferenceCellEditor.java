package org.sokybot.machine.page.trainingpage.monsterpreference;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.sokybot.settings.MonsterPreference;



public class PreferenceCellEditor extends AbstractCellEditor implements TableCellEditor, ItemListener {

	private Map<Integer, JComboBox<MonsterPreference>> m = new HashMap<>();

	private MonsterPreference monsterPreference;

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

		if (value instanceof MonsterPreference) {
			this.monsterPreference = ((MonsterPreference)value) ;
		}

		JComboBox<MonsterPreference> opts;

		if (m.containsKey(row)) {
			opts = this.m.get(row);
		} else {
			opts = new JComboBox<MonsterPreference>();
			
			MonsterPreference[] values = MonsterPreference.values();

			for (MonsterPreference p : values) {
				opts.addItem(p);
			}
			opts.addItemListener(this);

			this.m.put(row, opts) ; 
		}


		//opts.setSelectedItem(this.monsterPreference);

		return opts;

	}

	@Override
	public Object getCellEditorValue() {
		return this.monsterPreference;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {

		if (e.getStateChange() == ItemEvent.SELECTED) {
			JComboBox<MonsterPreference> opts = (JComboBox<MonsterPreference>) e.getSource();
			MonsterPreference selectedOpt = (MonsterPreference) opts.getSelectedItem();
			this.monsterPreference = selectedOpt;
			fireEditingStopped();
			System.out.println("MonsterPreferences Action Selected") ; 
			
		}

	}
  
}
