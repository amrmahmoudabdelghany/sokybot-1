package org.sokybot.skilllistcomps;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.sing_group.gc4s.input.list.ExtendedDefaultListModel;

import com.formdev.flatlaf.FlatDarculaLaf;

public class SkillTable extends JPanel implements ActionListener{

	
	private JComboBox<String> optList ; 
	private JButton btnMoveUp ; 
	private JButton btnMoveDown ; 
	private JList<String> skillList ; 
	private ExtendedDefaultListModel<String> listModel ; 
	
	public SkillTable() { 
		
		this.optList = new JComboBox<>() ; 
		this.listModel = new ExtendedDefaultListModel<>() ;
		this.listModel.addAll(List.of("Nadien" , "Amr" , "Z3`lilo" , "7abibi"));
		this.skillList = new JList<>(this.listModel) ; 
		this.skillList.setPreferredSize(new Dimension(150 , 250));
		this.btnMoveUp = new JButton("▲"); 
		this.btnMoveDown = new JButton("▼");
		this.btnMoveUp.addActionListener(this); 
		this.btnMoveDown.addActionListener(this);
		init() ; 
	}

	
	@PostConstruct
	private void init() { 
	
		this.setLayout(new BorderLayout());
		Box mainBox = Box.createVerticalBox() ; 
		
		mainBox.add(this.optList) ; 
		mainBox.add(new JScrollPane(this.skillList)) ; 
 
	//	this.skillList.setPreferredSize(new Dimension(400 , 400));
	
		
		
		Box btnBox = Box.createVerticalBox() ; 
		
		btnBox.add(Box.createVerticalGlue()) ; 
		btnBox.add(this.btnMoveUp) ; 
		btnBox.add(Box.createVerticalStrut(5)) ; 
		btnBox.add(this.btnMoveDown) ; 
		btnBox.add(Box.createVerticalGlue()) ; 
		
		

		Box vBox = Box.createHorizontalBox() ; 
		
		vBox.add(mainBox) ; 
		vBox.add(Box.createHorizontalStrut(5));
		vBox.add(btnBox) ; 
		
		this.add(vBox , BorderLayout.CENTER) ;
		
		
		
		
		
		
	}
	
	
	
	
	
	public static void main(String args[]) { 
	 
		
		FlatDarculaLaf.setup() ; 
		 JFrame frame = new JFrame() ; 
		 
		 frame.setLayout(new GridBagLayout());
		 frame.setPreferredSize(new Dimension(600 , 600));
		 SkillTable com = new SkillTable() ; 
		 
		 frame.add(com) ;  
		 
		 
		 frame.setLocationRelativeTo(null);
		 frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		 frame.pack(); 
		 frame.setVisible(true);
		
		
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		 
		if(e.getSource() == this.btnMoveUp) { 
			 
			int selectedIndex = this.skillList.getSelectedIndex() ; 
			if(this.listModel.moveUp(selectedIndex)) { 
				this.skillList.setSelectedIndex((selectedIndex - 1));
			}
		}else if(e.getSource() == this.btnMoveDown) { 
		   int selectedIndex = 	this.skillList.getSelectedIndex() ;
			if(this.listModel.moveDown(selectedIndex)) { 
				this.skillList.setSelectedIndex(selectedIndex + 1);
			}
			
		}
		
	}
	
	
	
}
