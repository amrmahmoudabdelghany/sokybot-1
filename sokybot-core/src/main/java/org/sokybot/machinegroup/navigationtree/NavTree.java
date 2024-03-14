package org.sokybot.machinegroup.navigationtree;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Enumeration;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class NavTree extends INavTree {

	private JTree tree;
	private DefaultMutableTreeNode root;

	@Value("${groupName}")
	private String groupName;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private TreeCellRenderer treeCellRenderer;

	@PostConstruct
	void init() {

		root = new DefaultMutableTreeNode(TreeNode.makeTreeNode(this.groupName, null));

		tree = new JTree(root, true);
		tree.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 5));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.getShowsRootHandles();
		tree.setRootVisible(true);
		tree.setCellRenderer(this.treeCellRenderer);

		setLayout(new BorderLayout());
		add(new JScrollPane(tree));
		setBorder(BorderFactory.createEtchedBorder());
		this.tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {

			public void valueChanged(TreeSelectionEvent e) {

				Object o[] = e.getPath().getPath();
				String nodePath = "";

				for (int i = 0; i < o.length; i++) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) o[i];
					TreeNode nodeData = (TreeNode) node.getUserObject();
					nodePath += nodeData.getNodeName() + ((i < o.length - 1) ? "." : "");
				}

				eventPublisher.publishEvent(new NavTreeSelectionEvent(this, nodePath));

			}
		});
	}

	
	@Override
	public boolean putLeafNode(String parentPath, TreeNode node) {

		return putTreeNode(parentPath, node, true);
	}

	@Override
	public boolean putNode(String parentPath, TreeNode node) {

		return putTreeNode(parentPath, node, false);
	}

	@Override
	public void removeNode(String nodePath ) { 
	  
	 DefaultMutableTreeNode node = 	getNodeAt(nodePath) ;
		
	 DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel() ; 
	 
	 model.removeNodeFromParent(node);
	 
	 
	}
	private boolean putTreeNode(String parentPath, TreeNode node, boolean isLeaf) {

		if (parentPath.isBlank())
			return false;

		DefaultMutableTreeNode targetNode = getNodeAt(parentPath);
		if (targetNode == null)
			return false;

		if (!targetNode.getAllowsChildren())
			return false;

		DefaultTreeModel treeModel = (DefaultTreeModel) this.tree.getModel();
		treeModel.insertNodeInto(new DefaultMutableTreeNode(node, !isLeaf), targetNode, targetNode.getChildCount());

	
		
		if (isLeaf)
			this.tree.expandPath(new TreePath(targetNode.getPath()));

		return true;
	}

	// sokybot
	// sokybot/isro
	// isro
	// isro/sea
	// sokybot/notexsitsnode
	// notexistsnode
	private DefaultMutableTreeNode getNodeAt(String path) {
		Object nodes[] = org.sokybot.utils.Helper.splite(path, '.');

		DefaultMutableTreeNode res = getNodeFrom((String) nodes[0], this.root);

		for (int i = 1; i < nodes.length && res != null; i++) {

			res = getNodeFrom((String) nodes[i], res);

		}

		return res;

	}

	private DefaultMutableTreeNode getNodeFrom(String nodeName, DefaultMutableTreeNode root) {

		TreeNode nodeData = (TreeNode) root.getUserObject();

		if (nodeData.getNodeName().equalsIgnoreCase(nodeName))
			return root;

		Enumeration<javax.swing.tree.TreeNode> children = root.children();

		while (children.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
			nodeData = (TreeNode) node.getUserObject();

			if (nodeData.getNodeName().equalsIgnoreCase(nodeName)) {
				return node;
			}
		}

		return null;
	}

}
