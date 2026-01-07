package org.sokybot.swingcomponents;

import java.awt.GridLayout;

import javax.swing.BorderFactory;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.sing_group.gc4s.dialog.AbstractInputJDialog;
import org.sing_group.gc4s.input.filechooser.JFileChooserPanel;
import org.sing_group.gc4s.input.filechooser.Mode;

import com.formdev.flatlaf.FlatDarkLaf;

public class AbstractDialogTest {

	
	
	public static void main(String args[]) { 
		
		FlatDarkLaf.setup() ; 
		
		AbstractInputJDialog dialog = new InputJDialog(null) ; 
		dialog.setVisible(true);
		
		if(dialog.isCanceled()) { 
			System.out.println("User Cancel request") ; 
		}
		
		
//		JDialog dialog = new JDialog() ; 
//		
//		dialog.setLayout(new BorderLayout());
//		dialog.add(new JFileChooserPanel(Mode.OPEN , "Select a file:") , BorderLayout.CENTER) ; 
//		
//		dialog.setVisible(true);
//		
	}
	
	
	private static class InputJDialog extends AbstractInputJDialog {
		private static final long serialVersionUID = 1L;

		protected InputJDialog(JFrame parent) {
			super(parent);
			 getDescriptionPane().setBackground(null);
			 super.okButton.setText("Create");
		}
		
		 

		protected String getDialogTitle() {
			return "Demo dialog";
		}

		protected String getDescription() {
			return "This is a custom dialog.";
		}
		
       
		protected JPanel getInputComponentsPane() {
			JPanel toret = new JPanel();
			toret.setLayout(new GridLayout(0, 1));
			toret.add(new JFileChooserPanel(Mode.OPEN, "Select a file:"));
			toret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			return toret;
		}
		
		
	} 
}
