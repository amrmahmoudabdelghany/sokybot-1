package org.sokybot.swingcomponents;

import javax.swing.Box;
import javax.swing.JFrame;

import org.sing_group.gc4s.input.text.ExtendedJXTextField;
import org.sing_group.gc4s.utilities.ColorUtils;

import com.formdev.flatlaf.FlatDarkLaf;

public class ExtendedJXTextFieldExample {

	
	
	public static void main(String args[]) { 
		
		FlatDarkLaf.setup() ; 
		
		
		JFrame frame = new JFrame() ; 
		
		
		Box box = Box.createVerticalBox() ; 
	  ExtendedJXTextField txtField = 	new ExtendedJXTextField("") ;
	  txtField.setEmptyTextFieldColor(ColorUtils.COLOR_INVALID_INPUT); 
	   box.add(txtField);	
	 
	 txtField = 	new ExtendedJXTextField("") ;
	  txtField.setEmptyTextFieldColor(ColorUtils.COLOR_INVALID_INPUT); 
	 box.add(txtField) ; 
	 
	 
	   txtField = 	new ExtendedJXTextField("Enter any text") ;
	  txtField.setEmptyTextFieldColor(ColorUtils.COLOR_INVALID_INPUT);
	  box.add(txtField) ; 
	  
 		frame.add(box) ; 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();  
		frame.setLocationRelativeTo(null); 
		frame.setVisible(true);
		
		
	}
}
