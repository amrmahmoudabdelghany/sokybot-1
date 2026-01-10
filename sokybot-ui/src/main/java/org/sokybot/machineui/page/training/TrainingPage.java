package org.sokybot.machineui.page.training;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.sokybot.runtime.IMachineContext;
import org.sokybot.machineui.page.IMachinePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainingPage extends JPanel implements IMachinePage {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(TrainingPage.class);

    private final IMachineContext context;
    private JTabbedPane tabbedPane;

    public TrainingPage(IMachineContext context) {
        this.context = context;
        init();
    }

    private void init() {
        this.tabbedPane = new JTabbedPane();
        
        this.tabbedPane.addTab("Area", new AreaTab(context));
        this.tabbedPane.addTab("Monsters", new org.sokybot.machineui.page.training.monsterpreference.MonsterPreferenceTab(context));
        
        setLayout(new java.awt.BorderLayout());
        add(tabbedPane, java.awt.BorderLayout.CENTER);
    }

    @Override
    public String getName() {
        return "Training";
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
