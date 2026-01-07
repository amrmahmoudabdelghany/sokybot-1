package org.sokybot.common;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.InvalidClassException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;

import org.sing_group.gc4s.input.list.ExtendedDefaultListModel;
import org.sing_group.gc4s.input.list.JListPanel;
import org.sing_group.gc4s.input.list.JParallelListsPanel;

public class TestJParallelListPane {

	
	
	public static void main(String args[]) throws InvalidClassException { 
		 
		ExtendedDefaultListModel<String> lstModel1 = new ExtendedDefaultListModel<>() ; 
		lstModel1.addAll(List.of("Amr" , "Mahmoud" , "abdelghany"));
		
		JList<String> list1 = new JList<>(lstModel1) ; 
		lstModel1 =  new ExtendedDefaultListModel<>() ; 
		lstModel1.addAll(List.of("Amr" , "Mahamoud" , "abdelghany" , "Mahmoud"));
		
		JList<String> list2 = new JList<>(lstModel1) ; 
		
		lstModel1 = new ExtendedDefaultListModel<>() ; 
		lstModel1.addAll(List.of("Amr" , "Mahamoud" , "AbdElghany2")); 
		
		JList<String> list3 = new JList<String>(lstModel1) ; 
		
		
		
		JParallelListsPanel<String > listPane = new JParallelListsPanel<>(list1, list2) ;  
		JParallelListsPanel<String> listPane2 = new JParallelListsPanel<>(list2, list3) ; 
		
		listPane.getRightListPanel().getBtnClearSelection().setVisible(false) ;
		listPane.getRightListPanel().getBtnRemoveElements().setVisible(false);
		listPane.getRightListPanel().getBtnSelectAll().setVisible(false);
		listPane.getLeftListPanel().getBtnClearSelection().setVisible(false) ;
		listPane.getLeftListPanel().getBtnRemoveElements().setVisible(false);
		listPane.getLeftListPanel().getBtnSelectAll().setVisible(false);
		
		
		JFrame frame = new JFrame() ; 
		JPanel cont = (JPanel) frame.getContentPane() ; 
		cont.setLayout(new GridBagLayout());
		cont.add(listPane) ; 
		cont.add(listPane2) ; 
		
		frame.setPreferredSize(new Dimension(400 , 400));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null); 
		frame.pack(); 
		frame.setVisible(true);
		
		
		
		
		
	}
}
