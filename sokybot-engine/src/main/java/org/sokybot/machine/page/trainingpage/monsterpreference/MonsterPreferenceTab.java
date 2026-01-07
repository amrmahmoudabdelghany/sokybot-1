package org.sokybot.machine.page.trainingpage.monsterpreference;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import org.sokybot.persistence.entities.MonsterType;
import org.sokybot.machinegroup.gamemodel.setting.MonsterPreference;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;

import info.clearthought.layout.TableLayout;

// @Component
public class MonsterPreferenceTab extends JPanel {
 

	private MonsterTypePreference monsterTypePreference ; 
	private MonsterNamePreference monsterNamePreference ; 
	
	
	// @Autowired
	public MonsterPreferenceTab(MonsterTypePreference monsterTypePreference , MonsterNamePreference monsterNamePreference) {
		this.monsterTypePreference = monsterTypePreference ; 
		this.monsterNamePreference = monsterNamePreference ; 
		init() ; 
	}
	
	private void init() { 
	
double border = 5 ; 
double padding = 10 ; 
		double size[][] = {
				{border , 0.40 , border} , //cols
				{border , 0.60 ,padding ,  0.40 , border } // rows
				};
		
		setLayout(new TableLayout(size));

		this.monsterTypePreference.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.monsterNamePreference.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JPanel b = new JPanel(new BorderLayout()) ;
		b.add(this.monsterTypePreference , BorderLayout.CENTER) ; 
		b.setBorder(BorderFactory.createTitledBorder("Type Filter"));
		add(b , "1 ,1 ") ; 
		b = new JPanel(new BorderLayout()) ;
		b.add(this.monsterNamePreference , BorderLayout.CENTER) ; 
		b.setBorder(BorderFactory.createTitledBorder("Name Filter"));
		add(b , "1 , 3") ; 
		
	}
	
	
	
	public static void main(String args[]) { 
		
		FlatDarkLaf.setup();
		
		JFrame frame = new JFrame() ; 
		
		MonsterPreferenceTab box = new MonsterPreferenceTab(new MonsterTypePreference(new Settings()) , new MonsterNamePreference(new Settings(), null)) ; 
		box.setBorder(BorderFactory.createTitledBorder("Monster Preferences"));
		frame.setLayout(new BorderLayout()); 
		
		frame.add(box , BorderLayout.CENTER) ; 
		frame.setPreferredSize(new Dimension(800 , 800));
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();  
		frame.setVisible(true);
	}
	
	
	
	
	
	 
	
	
}
