package org.sokybot.swingcomponents;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXTextField;
import org.sing_group.gc4s.input.InputParameter;
import org.sing_group.gc4s.input.InputParametersPanel;
import org.sing_group.gc4s.input.InputParametersPanel.DescriptionAlignment;

public class InputParamaterPanelExample {

	
	
	public static void main(String args[]) { 
		
		JFrame frame = new JFrame() ; 
		
		frame.add(createInputParameter()) ; 
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();  
		frame.setVisible(true);
		
		
	}
	
	
	private static JPanel createInputParameter() { 
		
		InputParameter in = new InputParameter("First Name : ", new JXTextField("enter first name"), "help ") ; 
		InputParameter in2 = new InputParameter("First Name : ", new JXTextField("enter first name"), "help ") ; 
		InputParameter in3 = new InputParameter("First Name : ", new JXTextField("enter first name"), "help ") ; 
		
		InputParametersPanel panel = new InputParametersPanel(DescriptionAlignment.LEFT,in , in2 , in3 ) ;
		
		
		
		return panel ; 
		
		
	}
}
