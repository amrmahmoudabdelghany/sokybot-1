package org.sokybot.machinegroup.navigationtree;


import javax.swing.JPanel;

public abstract class INavTree extends JPanel  {

	
	
	public abstract boolean putNode(String parentPath ,TreeNode node );
	
	public abstract boolean putLeafNode(String parentPath , TreeNode node) ; 
	
	public abstract void removeNode(String nodePath) ; 
}
