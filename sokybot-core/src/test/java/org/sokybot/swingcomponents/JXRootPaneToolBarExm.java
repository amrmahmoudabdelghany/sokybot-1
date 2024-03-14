package org.sokybot.swingcomponents;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXRootPane;

public class JXRootPaneToolBarExm {

	
	
	public static void main(String args[]) { 
		
		
		
		JXFrame frame = new JXFrame("ToolBar Exmple " , true) ; 
		
		JXRootPane rootPane = frame.getRootPaneExt() ; 
		
		rootPane.setToolBar(createToolBar());
		rootPane.getContentPane().add(createContentPanel()) ;
		
		//frame.setPreferredSize(new Dimension(400 , 400));
		frame.pack(); 
		frame.setVisible(true);
		
	}
	
	
	
	private static JToolBar createToolBar() { 
	
		JToolBar toolbar = new JToolBar() ; 
		toolbar.setFloatable(false);
		toolbar.add(new JButton("Action 1")) ;
		toolbar.add(new JButton("Action 2")) ;
		toolbar.add(new JButton("Action 3")) ;
		
		return toolbar ; 
	}
	
	
	private static JPanel createContentPanel() { 
		
		
		JPanel panel = new JPanel(new BorderLayout()) ; 
		
		Box page = Box.createVerticalBox() ; 
		
	    Box row = Box.createHorizontalBox() ; 
	    
	    row.add(new JLabel("First Name : ")) ;
		row.add(Box.createHorizontalStrut(5)) ; 
		row.add(new JTextField()) ; 
		page.add(row) ; 
		
		page.add(Box.createVerticalStrut(5)) ; 
		
		row= Box.createHorizontalBox() ; 
		row.add(new JLabel("Last Name : ")) ; 
		row.add(Box.createHorizontalStrut(5)) ; 
		row.add(new JTextField()) ; 
		
		page.add(row) ; 
		
		
		panel.add(page , BorderLayout.CENTER) ;
		return panel ; 
	}
}
