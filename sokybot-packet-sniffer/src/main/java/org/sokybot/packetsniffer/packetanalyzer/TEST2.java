package org.sokybot.packetsniffer.packetanalyzer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;

public class TEST2 {

	public static void main(String args[]) {

		JFrame frame = new JFrame();
		JPanel cont = (JPanel) frame.getContentPane();

		Box content = Box.createVerticalBox() ; 
		
		
		cont.setLayout(new BorderLayout());

		
		

		JButton btn = new JButton("Add"); 
		
		btn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
		
				JPanel packetBox = new JPanel(new BorderLayout()) ; 
				
				JTextArea comp1 = new JTextArea();
				JTextArea comp2 = new JTextArea();
				JTextArea comp3 = new JTextArea();
				
				comp1.setText("Opcode");
				comp2.setText("Source");
				comp3.setText("+");
				
				comp1.setBackground(Color.red);
				comp2.setBackground(Color.blue);
				comp3.setBackground(Color.yellow);
				
				packetBox.add(comp1 , BorderLayout.LINE_START) ; 
				packetBox.add(comp2 , BorderLayout.CENTER) ; 
				packetBox.add(comp3 , BorderLayout.LINE_END) ; 
				content.add(packetBox) ; 
				//content.updateUI();
				//content.validate();
				
				frame.pack();  
				
			}
		});

		Box btnBox = Box.createVerticalBox() ; 
		
		JButton btnRemove = new JButton("Remove") ; 
		btnRemove.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				content.removeAll();
				frame.pack();
			}
		});
		
		btnBox.add(btn) ;
		btnBox.add(btnRemove) ; 
	
		cont.add(btnBox , BorderLayout.PAGE_END) ; 
		
		cont.add(new JScrollPane(content) , BorderLayout.CENTER) ; 
		
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
