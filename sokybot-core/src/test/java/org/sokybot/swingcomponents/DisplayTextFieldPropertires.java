package org.sokybot.swingcomponents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JTextField;

public class DisplayTextFieldPropertires {

	
	
	public static void main(String args[]) { 
		
		JFrame frame = new JFrame() ; 
		 frame.add(createContentPanel()) ; 
		 frame.pack();  
		 frame.setVisible(true);
		 frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		 
		
	}
	
	
	private static Box createContentPanel() { 
		 
		Box page = Box.createVerticalBox() ; 
		
		JTextField txt = new JTextField() ; 
		
		PropertyChangeListener listener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
		 
				System.out.println(evt.getPropertyName() + " : " + evt.getNewValue() ) ; 
			}
		} ; 
		
		
		
		
		txt.addPropertyChangeListener(listener);
		
		page.add(txt) ; 
		
		
		txt = new JTextField()  ; 
		
		txt.addPropertyChangeListener(listener);
		
		page.add(txt) ; 
		
		
		return page ; 
	}
}
