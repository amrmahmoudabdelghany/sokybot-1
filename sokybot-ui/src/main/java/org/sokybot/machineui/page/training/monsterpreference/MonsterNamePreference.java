package org.sokybot.machineui.page.training.monsterpreference;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import org.sokybot.settings.Settings;
import org.sokybot.machine.gamemodel.Trainer;

// Simplified placeholder for now - need to port full TableModel logic later
public class MonsterNamePreference extends JPanel {
    private static final long serialVersionUID = 1L;
    private Settings settings;
    private Trainer trainer;

    public MonsterNamePreference(Settings settings, Trainer trainer) {
        this.settings = settings;
        this.trainer = trainer;
        setLayout(new BorderLayout());
        add(new javax.swing.JLabel("Monster Name Preferences Table Placeholder"), BorderLayout.CENTER);
    }
}
