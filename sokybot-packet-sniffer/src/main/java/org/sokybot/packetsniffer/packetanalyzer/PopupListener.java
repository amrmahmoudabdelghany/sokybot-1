package org.sokybot.packetsniffer.packetanalyzer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class PopupListener extends MouseAdapter  {

	private JPopupMenu popupMenu ; 
	
	
	public PopupListener() {
		this.popupMenu = new JPopupMenu() ; 
		
	}
	
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.isPopupTrigger()) { 
			this.popupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	public void addMenuItem(JMenuItem menuItem) { 
		this.popupMenu.add(menuItem) ; 
	}
	
}
