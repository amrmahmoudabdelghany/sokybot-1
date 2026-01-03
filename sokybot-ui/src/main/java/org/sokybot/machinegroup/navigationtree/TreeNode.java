package org.sokybot.machinegroup.navigationtree;


import javax.swing.Icon;


public class TreeNode  {

	 private String nodeName ; 
	 private Icon nodeIcon ;
	 
	 
	private TreeNode(String nodeName , Icon nodeIcon ) {
		this.nodeIcon = nodeIcon ; 
		this.nodeName = nodeName ; 
		
	}
	 
	public String getNodeName() {
		return nodeName;
	}
	
	public Icon getNodeIcon() {
		return nodeIcon;
	}
	
	 
	
	public static TreeNode makeTreeNode(String name , Icon icon ) { 
	  
	   if(icon == null) return  new TreeNode(name , null) ; 
	//	ImageIcon imageIcon =   new ImageIcon(Helper.fitimage(icon, 40, 40)) ; 
		//Icon i = new FlatSVGIcon() ; 
	   return new TreeNode(name , icon) ; 
	}
	

}
