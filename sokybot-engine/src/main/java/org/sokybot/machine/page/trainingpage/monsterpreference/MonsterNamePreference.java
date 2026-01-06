package org.sokybot.machine.page.trainingpage.monsterpreference;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.sokybot.machinegroup.gamemodel.setting.MonsterPreference;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.statemachine.config.configurers.DefaultHistoryTransitionConfigurer;
// import org.springframework.stereotype.Component;

// @Component
public class MonsterNamePreference extends JPanel {

	private JTextField txtFilter;
	private JTable tblRes;
	private NamePreferencesTableModel model;
	private Settings settings;
	private ISroMaterialDAO sroMaterialDAO;

	// @Autowired
	public MonsterNamePreference(Settings settings, ISroMaterialDAO sroMaterialDAO) {
		this.settings = settings;
		this.tblRes = new JTable();
		this.tblRes.setDefaultRenderer(MonsterPreference.class, new PreferenceCellRender());
		this.tblRes.setDefaultEditor(MonsterPreference.class, new PreferenceCellEditor());
		this.txtFilter = new JTextField();
		this.model = new NamePreferencesTableModel(settings);
		this.tblRes.setModel(model);
		this.sroMaterialDAO = sroMaterialDAO;
		init();
	}

	private void init() {
		BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(layout);
		add(this.txtFilter);
		add(new JScrollPane(tblRes));
		this.txtFilter.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				String filter = txtFilter.getText();
				model.clear();
				if (!filter.isBlank()) {
					sroMaterialDAO.findAllMonsterLike(filter).forEach((npc) -> {
						model.addEntry(npc.getName(), String.valueOf(npc.getLevel()), MonsterPreference.NONE);
					});
				}
			}
		});
	}

}
