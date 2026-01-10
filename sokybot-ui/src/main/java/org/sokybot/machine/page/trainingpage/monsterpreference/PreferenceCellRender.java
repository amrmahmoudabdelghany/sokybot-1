package org.sokybot.machine.page.trainingpage.monsterpreference;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.sokybot.settings.MonsterPreference;


public class PreferenceCellRender extends DefaultTableCellRenderer {

	
	@Override
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected,
			boolean hasFocus,
			int row, 
			int column) {
	
		if(value instanceof MonsterPreference) { 
			setText(((MonsterPreference) value).name());
		}
		
		
		
		return this ; 
		
	}
}
