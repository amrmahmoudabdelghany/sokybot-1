package org.sokybot.machineui.page.training.monsterpreference;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.sokybot.settings.Settings;
import org.sokybot.runtime.IMachineContext;

import com.formdev.flatlaf.FlatDarkLaf;

import info.clearthought.layout.TableLayout;

public class MonsterPreferenceTab extends JPanel {

    private static final long serialVersionUID = 1L;
    private MonsterTypePreference monsterTypePreference;
    private MonsterNamePreference monsterNamePreference;

    public MonsterPreferenceTab(IMachineContext context) {
        Settings settings = context.getSettings();
        if (settings != null) {
            this.monsterTypePreference = new MonsterTypePreference(settings);
            this.monsterNamePreference = new MonsterNamePreference(settings, null); // TODO: Pass trainer if needed
            init();
        } else {
            add(new javax.swing.JLabel("Settings not available"));
        }
    }

    private void init() {
        double border = 5;
        double padding = 10;
        double size[][] = { 
            { border, 0.40, border }, // cols
            { border, 0.60, padding, 0.40, border } // rows
        };

        setLayout(new TableLayout(size));

        this.monsterTypePreference.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.monsterNamePreference.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel b = new JPanel(new BorderLayout());
        b.add(this.monsterTypePreference, BorderLayout.CENTER);
        b.setBorder(BorderFactory.createTitledBorder("Type Filter"));
        add(b, "1 ,1 ");
        b = new JPanel(new BorderLayout());
        b.add(this.monsterNamePreference, BorderLayout.CENTER);
        b.setBorder(BorderFactory.createTitledBorder("Name Filter"));
        add(b, "1 , 3");
    }
}
