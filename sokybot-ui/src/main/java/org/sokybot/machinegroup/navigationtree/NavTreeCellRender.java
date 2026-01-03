package org.sokybot.machinegroup.navigationtree;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer; 


@org.springframework.stereotype.Component
public class NavTreeCellRender implements TreeCellRenderer {

	
	private JLabel label  ; 
	
	
    public NavTreeCellRender() {
	  label = new JLabel() ; 
    }
	
	
	

	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
	     
		
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value ; 
		
		Object nodeData = node.getUserObject() ; 
		
		if(nodeData instanceof TreeNode) { 
			
			TreeNode treeNode = (TreeNode) nodeData ; 
			
			 this.label.setText(treeNode.getNodeName());
			 this.label.setIcon(treeNode.getNodeIcon());
			
		}else { 
		
			this.label.setText("" + value);
		}
		
		
		return this.label ; 
	}
}
