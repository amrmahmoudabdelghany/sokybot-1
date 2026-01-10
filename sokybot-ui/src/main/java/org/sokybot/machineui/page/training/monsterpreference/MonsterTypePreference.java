package org.sokybot.machineui.page.training.monsterpreference;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import org.sokybot.settings.Settings;

// Simplified placeholder for now - need to port full TableModel logic later
public class MonsterTypePreference extends JPanel {
    private static final long serialVersionUID = 1L;
    private Settings settings;

    public MonsterTypePreference(Settings settings) {
        this.settings = settings;
        setLayout(new BorderLayout());
        add(new javax.swing.JLabel("Monster Type Preferences Table Placeholder"), BorderLayout.CENTER);
    }
}
