package org.sokybot.machine.page.environmentpage;

import java.awt.Dimension;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.sokybot.machine.gamemodel.IGameModel;
import org.sokybot.machine.gamemodel.ISpawnListener;
import org.sokybot.machinegroup.gamemodel.ISpawnable;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.npc.MonsterType;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

import info.clearthought.layout.TableLayout;

// @Component
public class MonsterTable extends JPanel {

	
	private JTable monsterTable ; 
	
	
	// @Autowired
	private IGameModel gameModel ; 
	
	
	
	
	
	
	// @PostConstruct
	public void init() { 
		

		MonsterTableModel tableModel = new MonsterTableModel() ; 
		this.gameModel.addSpawnListener(tableModel);	
		this.monsterTable = new JTable(tableModel ) ; 
	
		
		
		int border =  5 ; 
		
		double sizes[][] = {
				{border , TableLayout.FILL ,border } , // columns 
				{border , TableLayout.FILL , border}  // rows 
				} ; 
		TableLayout layout = new TableLayout(sizes) ; 
		setLayout(layout);
		
		this.add(new JScrollPane(monsterTable)  , "1 , 1") ; 
		
		
	}
	
	
	private class MonsterTableModel extends AbstractTableModel implements ISpawnListener {

		
		private Vector<Monster> monsters = new Vector<>() ; 

		private String [] cols = {"Name" ,"Type" ,  "Lvl" , "HP" , "Distance"} ; 
		
		
		
		@Override
		public int getColumnCount() {
		
			return cols.length;
		}

		@Override
		public int getRowCount() {

			return monsters.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
		
			if(rowIndex >= this.monsters.size())  {
				return null ; 
			}
			
			Monster monster = this.monsters.get(rowIndex) ; 
			
			switch(columnIndex) { 
			 
			case 0 : return monster.getName()  ; 
			
			case 1 : return  monster.getMonsterType().name() ; 
					
			case 2 : return monster.getLevel() ; 
			
			case 3 : return ((monster.getCurrentHP() / monster.getMaxHP()) * 100) + "%"   ; 
			
			case 4 : return (int) gameModel.getTrainer().distance(monster.getX(), monster.getY()) ;
			
			}
			
			return "UNKNOWN";
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {

			return String.class ; 
		}

		@Override
		public String getColumnName(int column) {

			if (column >= this.cols.length)
				return null;

			return this.cols[column];
		}

		@Override
		public void spawnAdded(ISpawnable spawnObj) {
		
			if(spawnObj instanceof Monster) { 
				this.monsters.add((Monster) spawnObj) ; 
			}
			 
			fireTableDataChanged();
		}

		@Override
		public void spawnRemoved(ISpawnable spawnObj) {
			if(spawnObj instanceof Monster) { 
				this.monsters.remove(spawnObj) ; 
			}
			
			fireTableDataChanged();
		}
		
		@Override
		public void spawnSelected(ISpawnable spawnObj) {
		 
			
		}
	}

	
	
	
	public static void main(String args[]) { 
		
		JFrame frame = new JFrame() ; 
		
		
		frame.add(new MonsterTable()) ; 
		
		
		
		frame.setPreferredSize(new Dimension(400 , 400)); 
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack(); 
		frame.setVisible(true);
		
		
	}
	
	
	
}
