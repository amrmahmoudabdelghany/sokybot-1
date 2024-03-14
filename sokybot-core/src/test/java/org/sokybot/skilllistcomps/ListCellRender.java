package org.sokybot.skilllistcomps;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.formdev.flatlaf.icons.FlatSearchIcon;

public class ListCellRender extends JLabel implements ListCellRenderer<String>{

	
	
	public ListCellRender() {
	
		setOpaque(true);
	}
	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
			boolean isSelected, boolean cellHasFocus) {
		setIcon(new FlatSearchIcon()); 
		setText(value.concat("    ( Lvl." + (int)(Math.random() * 10) + " )"  ));
		
		
		
		if(isSelected) { 
			
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}else { 
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		return this;
	}

	
	
}
