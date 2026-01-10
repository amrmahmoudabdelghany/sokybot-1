package org.sokybot.machineui.page.item;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

import org.sokybot.machineui.page.IMachinePage;
import org.sokybot.runtime.IMachineContext;

public class ItemPage extends JPanel implements IMachinePage {
    private static final long serialVersionUID = 1L;
    private final IMachineContext context;
    private JTabbedPane tabbedPane;

    public ItemPage(IMachineContext context) {
        this.context = context;
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        this.tabbedPane = new JTabbedPane();
        
        // Add Filter Tab
        this.tabbedPane.addTab("Pick Filter", new ItemFilterTab());
        
        // TODO: Add Inventory Tab later
        
        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public String getName() {
        return "Items";
    }

    @Override
    public javax.swing.Icon getIcon() {
        return null;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }
}
