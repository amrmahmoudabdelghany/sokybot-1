package org.sokybot.machine.page.skillpage;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.sokybot.swing.SokyBotIcons;
import org.sokybot.machine.page.skillpage.SkillTab.MasteryEntry;
import org.sokybot.machine.page.skillpage.SkillTab.MonsterTypeEntry;
import org.sokybot.machinegroup.gamemodel.npc.MonsterType;
import org.sokybot.machinegroup.gamemodel.skill.Skill;

public class SkillTabComponentFactory {

	private final int ICON_WIDTH = 35;
	private final int ICON_HEIGHT = 35;

	private SkillTab skillTab;

	protected SkillTabComponentFactory(SkillTab tab) {
		this.skillTab = tab;

	}

	protected java.awt.Component createSkillTableBox() {
		this.skillTab.comMasteryFilter = new JComboBox<MasteryEntry>();
		this.skillTab.tblSkillModel = new SkillTableModel();
		this.skillTab.tblSkill = new JTable(this.skillTab.tblSkillModel);
		this.skillTab.tblSkill.setTableHeader(null);
		this.skillTab.tblSkill.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		this.skillTab.checkAttackSkills = new JCheckBox("Attack Skills");
		this.skillTab.checkBuffSkills = new JCheckBox("Buff Skills");
		this.skillTab.checkAttackSkills.setSelected(true);
		this.skillTab.checkBuffSkills.setSelected(true);
		this.skillTab.checkHideLowLv = new JCheckBox("Hide low lvl skills");

		Box skillFilterOpts = Box.createHorizontalBox();
		skillFilterOpts.add(this.skillTab.checkAttackSkills);
		skillFilterOpts.add(Box.createHorizontalStrut(5));
		skillFilterOpts.add(this.skillTab.checkBuffSkills);
		skillFilterOpts.add(Box.createGlue());
		skillFilterOpts.add(this.skillTab.checkHideLowLv);

		Box content = Box.createVerticalBox();
		content.add(this.skillTab.comMasteryFilter);

		content.add(new JScrollPane(this.skillTab.tblSkill));
		content.add(Box.createVerticalStrut(5));
		content.add(skillFilterOpts);
		content.add(Box.createGlue());

		content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		return makeTitle(content, "Trainer Skills");
	}

	protected java.awt.Component createBuffSkillBox() {
		this.skillTab.comBuffSkillFilter = new JComboBox<Skill>();
		this.skillTab.tblBuffSkillModel = new SkillTableModel();
		this.skillTab.tblBuffSkill = new JTable(this.skillTab.tblBuffSkillModel);
		this.skillTab.tblBuffSkill.setTableHeader(null);

		this.skillTab.btnAddBuffSkill = new JButton(SokyBotIcons.getIcon("icons/add-box.svg", ICON_WIDTH, ICON_HEIGHT));
		
		this.skillTab.btnRemoveBuffSkill = new JButton(
				SokyBotIcons.getIcon("icons/remove.svg", ICON_WIDTH, ICON_HEIGHT));
		this.skillTab.btnMoveUpBuffSkill = new JButton(
				SokyBotIcons.getIcon("icons/move-up.svg", ICON_WIDTH, ICON_HEIGHT));
		this.skillTab.btnMoveDownBuffSkill = new JButton(
				SokyBotIcons.getIcon("icons/move-down.svg", ICON_WIDTH, ICON_HEIGHT));


		this.skillTab.btnAddBuffSkill.setEnabled(false);
		this.skillTab.btnRemoveBuffSkill.setEnabled(false) ; 
		this.skillTab.btnMoveUpBuffSkill.setEnabled(false) ; 
		this.skillTab.btnMoveDownBuffSkill.setEnabled(false) ; 
		
		
		Box btnBox = Box.createVerticalBox();
		btnBox.add(Box.createGlue());
		btnBox.add(this.skillTab.btnAddBuffSkill);
		btnBox.add(Box.createVerticalStrut(4));
		btnBox.add(this.skillTab.btnRemoveBuffSkill);
		btnBox.add(Box.createVerticalStrut(4));
		btnBox.add(this.skillTab.btnMoveUpBuffSkill);
		btnBox.add(Box.createVerticalStrut(4));
		btnBox.add(this.skillTab.btnMoveDownBuffSkill);
		btnBox.add(Box.createGlue());

		Box buffTableBox = Box.createVerticalBox();
		buffTableBox.add(this.skillTab.comBuffSkillFilter);
		buffTableBox.add(new JScrollPane(this.skillTab.tblBuffSkill));

		Box contentBox = Box.createHorizontalBox();

		contentBox.add(buffTableBox);
		contentBox.add(Box.createHorizontalStrut(5));
		contentBox.add(btnBox);

		contentBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		return makeTitle(contentBox, "Buff Skills");

	}

	protected java.awt.Component createAttackSkillBox() {
		this.skillTab.comAttackSkillFilter = new JComboBox<MonsterTypeEntry>();
		this.skillTab.tblAttackSkillModel = new SkillTableModel();
		this.skillTab.tblAttackSkill = new JTable(this.skillTab.tblAttackSkillModel);
		this.skillTab.tblAttackSkill.setTableHeader(null) ; 
		
		this.skillTab.btnAddAttackSkill = new JButton(SokyBotIcons.getIcon("icons/add-box.svg", ICON_WIDTH, ICON_HEIGHT));
		this.skillTab.btnRemoveAttackSkill = new JButton(SokyBotIcons.getIcon("icons/remove.svg", ICON_WIDTH, ICON_HEIGHT));
		this.skillTab.btnMoveUpAttackSkill = new JButton(SokyBotIcons.getIcon("icons/move-up.svg", ICON_WIDTH, ICON_HEIGHT));
		this.skillTab.btnMoveDownAttackSkill = new JButton(SokyBotIcons.getIcon("icons/move-down.svg", ICON_WIDTH, ICON_HEIGHT));

	   
		this.skillTab.btnAddAttackSkill.setEnabled(false) ; 
		this.skillTab.btnRemoveAttackSkill.setEnabled(false) ; 
		this.skillTab.btnMoveUpAttackSkill.setEnabled(false) ; 
		this.skillTab.btnMoveDownAttackSkill.setEnabled(false) ; 
		
		
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("General", MonsterType.Normal));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Champion", MonsterType.Champion));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Giant", MonsterType.Giant));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Unique", MonsterType.Unique));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Strong", MonsterType.Strong));

		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("General (Party)", MonsterType.Party));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Champion (Party)", MonsterType.PartyChampion));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Giant (Party)", MonsterType.PartyGiant));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Unique (Party)", MonsterType.PartyUnique));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Strong (Party)", MonsterType.PartyStrong));

		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Elite", MonsterType.Elite));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Event", MonsterType.Event));
		this.skillTab.comAttackSkillFilter.addItem(new MonsterTypeEntry("Quest", MonsterType.Quest));

		Box btnBox = Box.createVerticalBox();
		btnBox.add(Box.createGlue());
		btnBox.add(this.skillTab.btnAddAttackSkill);
		btnBox.add(Box.createVerticalStrut(4));
		btnBox.add(this.skillTab.btnRemoveAttackSkill);
		btnBox.add(Box.createVerticalStrut(4));
		btnBox.add(this.skillTab.btnMoveUpAttackSkill);
		btnBox.add(Box.createVerticalStrut(4));
		btnBox.add(this.skillTab.btnMoveDownAttackSkill);
		btnBox.add(Box.createGlue());

		Box attackTableBox = Box.createVerticalBox();
		attackTableBox.add(this.skillTab.comAttackSkillFilter);
		attackTableBox.add(new JScrollPane(this.skillTab.tblAttackSkill));

		Box contentBox = Box.createHorizontalBox();

		contentBox.add(attackTableBox);
		contentBox.add(Box.createHorizontalStrut(5));
		contentBox.add(btnBox);

		contentBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		return makeTitle(contentBox, "Attack Skills");
	}

	protected java.awt.Component createSettingBox() {
		JPanel panel = new JPanel();

		panel.setBorder(BorderFactory.createTitledBorder("Settings"));

		return panel;
	}

	private java.awt.Component makeTitle(java.awt.Component comp, String title) {
		JPanel border = new JPanel(new BorderLayout());
		border.add(comp, BorderLayout.CENTER);
		border.setBorder(BorderFactory.createTitledBorder(title));
		return border;

	}

}
