package org.sokybot.machine.page.trainingpage.monsterpreference;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.sokybot.persistence.entities.MonsterType;
import org.sokybot.machinegroup.gamemodel.setting.MonsterPreference;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;

// @Component
public class MonsterTypePreference extends JPanel {

	private Settings settings;

	private JTable preTable;
	private TypePreferencesTableModel model;

	// @Autowired
	public MonsterTypePreference(Settings settings) {
		this.settings = settings;
		this.preTable = new JTable();
		this.model = new TypePreferencesTableModel(settings);
		this.preTable.setModel(model);
		this.preTable.setDefaultRenderer(MonsterPreference.class, new PreferenceCellRender());
		this.preTable.setDefaultEditor(MonsterPreference.class, new PreferenceCellEditor());
		init();
	}

	private void init() {
		setLayout(new BorderLayout());
		this.add(new JScrollPane(this.preTable), BorderLayout.CENTER);

		addRowData();
	}

	private void addRowData() {
		MonsterType[] typs = MonsterType.values();

		for (MonsterType type : typs) {

			switch (type) {
			case Normal:
				this.model.addEntry("General", type, settings.getMonsterPreference(type));
				break;
			case Party:
				this.model.addEntry("General Party", type, settings.getMonsterPreference(type));
				break;
			case Champion:
				this.model.addEntry("Champion", type, settings.getMonsterPreference(type));
				break;
			case PartyChampion:
				this.model.addEntry("Champion Party", type, settings.getMonsterPreference(type));
				break;
			case Giant:
				this.model.addEntry("Giant", type, settings.getMonsterPreference(type));
				break;
			case PartyGiant:
				this.model.addEntry("Giant Party", type, settings.getMonsterPreference(type));
				break;
			case Strong:
				this.model.addEntry("Strong", type, settings.getMonsterPreference(type));
				break;
			case PartyStrong:
				this.model.addEntry("Strong Party", type, settings.getMonsterPreference(type));
				break;
			case Unique:
				this.model.addEntry("Unique", type, settings.getMonsterPreference(type));
				break;
			case PartyUnique:
				this.model.addEntry("Unique Party", type, settings.getMonsterPreference(type));
				break;
			case Event:
				this.model.addEntry("Event", type, settings.getMonsterPreference(type));
				break;
			case Quest:
				this.model.addEntry("Quest", type, settings.getMonsterPreference(type));
				break;
			case Elite:
				this.model.addEntry("Elite", type, settings.getMonsterPreference(type));
				break;
			case Elite1:
			case Elite2:
			case UNKNOWN:
				break;

			}
		}

	}

	public static void main(String args[]) {

		FlatDarkLaf.setup();

		JFrame frame = new JFrame();

		MonsterTypePreference box = new MonsterTypePreference(new Settings());
		box.setBorder(BorderFactory.createTitledBorder("Monster Preferences"));
		frame.setLayout(new BorderLayout());

		frame.add(box, BorderLayout.CENTER);
		frame.setPreferredSize(new Dimension(400, 400));

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
