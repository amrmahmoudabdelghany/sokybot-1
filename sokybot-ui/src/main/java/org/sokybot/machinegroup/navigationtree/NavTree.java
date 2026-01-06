package org.sokybot.machinegroup.navigationtree;

import java.awt.BorderLayout;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@Component(service = INavTree.class)
public class NavTree extends INavTree {
    
    private JTree tree;
    protected JTree navTree;
    private DefaultMutableTreeNode root;

    // Configurable group name, could come from config admin
    private String groupName = "SokyBot"; 

    private EventAdmin eventAdmin; // Field declaration without @Reference

    @Reference // @Reference moved to setter method
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }
    
    // Optional reference, fallback to default
    // @Reference(target = "(component.name=parametersTreeCellRenderer)") 
    // Assuming the renderer is a component or we use default. 
    // Actually, let's just use DefaultTreeCellRenderer for now to unblock
    private TreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer();

    @Activate
    public void start() {
        init();
    }

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
                if (e.getPath() == null) return;
                
                Object o[] = e.getPath().getPath();
                String nodePath = "";

                for (int i = 0; i < o.length; i++) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) o[i];
                    TreeNode nodeData = (TreeNode) node.getUserObject();
                    nodePath += nodeData.getNodeName() + ((i < o.length - 1) ? "." : "");
                }

                // Publish OSGi Event
                NavTreeSelectionEvent selectionEvent = new NavTreeSelectionEvent(NavTree.this, nodePath);
                Dictionary<String, Object> props = new Hashtable<>();
                props.put("event", selectionEvent);
                props.put("selectedPath", nodePath);
                
                Event osgiEvent = new Event("org/sokybot/ui/NAV_TREE_SELECTION", props);
                if (eventAdmin != null) {
                    eventAdmin.postEvent(osgiEvent);
                }
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
    public void removeNode(String nodePath) {
        DefaultMutableTreeNode node = getNodeAt(nodePath);
        if (node != null) {
            DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
            model.removeNodeFromParent(node);
        }
    }

    private boolean putTreeNode(String parentPath, TreeNode node, boolean isLeaf) {
        if (parentPath == null || parentPath.isBlank())
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

    private DefaultMutableTreeNode getNodeAt(String path) {
        if (path == null) return null;
        Object nodes[] = org.sokybot.utils.Helper.splite(path, '.');
        if (nodes.length == 0) return null;

        DefaultMutableTreeNode res = getNodeFrom((String) nodes[0], this.root);

        for (int i = 1; i < nodes.length && res != null; i++) {
            res = getNodeFrom((String) nodes[i], res);
        }
        return res;
    }

    private DefaultMutableTreeNode getNodeFrom(String nodeName, DefaultMutableTreeNode root) {
        if (root == null) return null;
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
