package org.sokybot.swingcomponents;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.MultiSplitLayout;

public class JXMultiSplitePaneExample {

	
	
	public static void main(String args[]) { 
		
		
		JFrame frame = new JFrame() ; 
		 
		
		//JXMultiSplitPane jxMultiSplitPane = new JXMultiSplitPane() ; 
		
		//   String layoutDef = "(ROW weight=1.0 (LEAF name=resources weight=0.5) (LEAF name=cost weight=0.5))";
		
		String layoutDef = "(ROW (LEAF name = p1 weight=0.2)"
				+ " (LEAF name = p2 weight=0.6) (LEAF name =p3 weight= 0.2))" ;
		MultiSplitLayout.Node node =  MultiSplitLayout.parseModel(layoutDef);
		MultiSplitLayout multiSpliteLayout = new MultiSplitLayout(node) ; 
		multiSpliteLayout.setDividerSize(2);
		JPanel mainCont = new JPanel(multiSpliteLayout) ; 
		
		  //jxMultiSplitPane.getMultiSplitLayout().setModel(node);
		  
		  mainCont.add(createColoredJPanel(Color.red ,"p1") , "p1") ;
		  mainCont.add(createColoredJPanel(Color.yellow , "p2") , "p2") ;
		  mainCont.add(createColoredJPanel(Color.green , "p3"), "p3") ; 
		  
		  //jxMultiSplitPane.add(createColoredJPanel(Color.black , "test")) ;
		
		  
		//jxMultiSplitPane.setDividerSize(2);
		frame.setPreferredSize(new Dimension(800 , 400));
		frame.add(mainCont); 
		frame.pack(); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);; 
		frame.setLocationRelativeTo(null);
		
	}
	
	
	private static JPanel createColoredJPanel(Color color , String title) { 
		
		JPanel panel = new JPanel() ; 
		
		panel.add(new JLabel(title));
		panel.setBackground(color);
		
		return panel ; 
	}
}
