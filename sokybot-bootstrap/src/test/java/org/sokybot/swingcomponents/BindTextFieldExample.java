package org.sokybot.swingcomponents;


import javax.swing.Box;
import javax.swing.JFrame;

import org.sing_group.gc4s.input.text.BindJXTextField;

public class BindTextFieldExample {

	
	public static void main(String args[]) { 
		
		Box box = Box.createVerticalBox() ; 
		
		box.add(new BindJXTextField("f1 prompate", "f1 test", (str)->System.out.println(str))) ; 
		box.add(new BindJXTextField("f2 prompate", "f2 test", (str)->System.out.println(str))) ; 
		
		
		JFrame frame = new JFrame() ; 
		
		frame.add(box)  ; 
		
		frame.pack();  
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		
		
	}
}
