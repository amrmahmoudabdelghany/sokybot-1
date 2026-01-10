package org.sokybot.groupui;

import org.sokybot.runtime.IGroupContext;
import org.sokybot.machinegroup.PageContainer;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.sokybot.machinegroup.navigationtree.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.icons.FlatSearchIcon;

/**
 * Manages UI components for a machine group.
 * 
 * Currently a placeholder for future group-level UI components.
 */
public class GroupUIInstance {
    
    private static final Logger log = LoggerFactory.getLogger(GroupUIInstance.class);
    
    private final String groupName;
    private final IGroupContext context;
    private final PageContainer pageContainer;
    private final INavTree navTree;
    
    public GroupUIInstance(
            String groupName,
            IGroupContext context,
            PageContainer pageContainer,
            INavTree navTree) {
        this.groupName = groupName;
        this.context = context;
        this.pageContainer = pageContainer;
        this.navTree = navTree;
    }
    
    /**
     * Creates group-level UI components.
     */
    public void createPages() {
        log.debug("Creating UI for group: {}", groupName);
        
        // Create navigation tree node for this group
        TreeNode groupNode = TreeNode.makeTreeNode(groupName, new FlatSearchIcon());
        navTree.putNode(groupName, groupNode);
        
        // TODO: Create group-level pages if needed
        
        log.info("Created UI for group: {}", groupName);
    }
    
    /**
     * Destroys group-level UI components.
     */
    public void destroyPages() {
        log.debug("Destroying UI for group: {}", groupName);
        
        // Remove navigation tree node
        navTree.removeNode(groupName);
        
        // TODO: Remove group-level pages if any
        
        log.info("Destroyed UI for group: {}", groupName);
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public IGroupContext getContext() {
        return context;
    }
}
