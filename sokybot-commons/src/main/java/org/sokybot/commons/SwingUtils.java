package org.sokybot.commons;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class SwingUtils {

	
	
	
	
	
	public static void display(JComponent comp ) { 
	 JFrame frame = new JFrame() ; 
	 
	 frame.setLayout(new GridBagLayout());
	 GridBagConstraints gbc = new GridBagConstraints() ; 
	 gbc.gridx = 0 ; 
	 gbc.gridy = 0 ; 
	 gbc.insets = new Insets(5, 5, 5, 5) ;
	 frame.add(comp , gbc) ;
	 frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	 frame.pack(); 
	 frame.setLocationRelativeTo(null);
	 frame.setVisible(true);
		
	}
	
	
}
