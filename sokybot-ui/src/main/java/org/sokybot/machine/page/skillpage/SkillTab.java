package org.sokybot.machine.page.skillpage;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.sokybot.machine.event.MasteryLvlUpEvent;
import org.sokybot.machine.event.trainerevent.SkillLvlupEvent;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.machine.event.trainerevent.TrainerSkillsUpdatedEvent;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.npc.MonsterType;
import org.sokybot.settings.Settings;
import org.sokybot.machinegroup.gamemodel.skill.Mastery;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.machinegroup.gamemodel.skill.SkillEntity;
import org.sokybot.machinegroup.gamemodel.skill.SkillType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;

import info.clearthought.layout.TableLayout;
import lombok.AllArgsConstructor;
import lombok.Data;

@Component
public class SkillTab extends JPanel implements ItemListener, ListSelectionListener, ActionListener {

	@Autowired
	private Trainer trainer;

	@Autowired
	private Settings userConfig;

	protected JComboBox<MasteryEntry> comMasteryFilter;
	protected SkillTableModel tblSkillModel;
	protected JTable tblSkill;
	protected JCheckBox checkBuffSkills;
	protected JCheckBox checkAttackSkills;
	protected JCheckBox checkHideLowLv;

	// attack skill comps

	protected JComboBox<MonsterTypeEntry> comAttackSkillFilter;
	protected SkillTableModel tblAttackSkillModel;
	protected JTable tblAttackSkill;
	protected JButton btnAddAttackSkill;
	protected JButton btnRemoveAttackSkill;
	protected JButton btnMoveUpAttackSkill;
	protected JButton btnMoveDownAttackSkill;
	protected JCheckBox checkNoAttack;

	// buff skill comps

	protected JComboBox<Skill> comBuffSkillFilter;
	protected SkillTableModel tblBuffSkillModel;
	protected JTable tblBuffSkill;
	protected JButton btnAddBuffSkill;
	protected JButton btnRemoveBuffSkill;
	protected JButton btnMoveUpBuffSkill;
	protected JButton btnMoveDownBuffSkill;

	private SkillTabComponentFactory compFactory = new SkillTabComponentFactory(this);

	@PostConstruct
	private void init() {

		float border = 5f;
		float gab = 5;
		double size[][] = { { border, 350, gab, 300, border }, // cols
				{ border, 0.35, 0.35, gab, TableLayout.FILL, border } // rows
		};

		TableLayout tableLayout = new TableLayout(size);
		setLayout(tableLayout);

		add(compFactory.createSkillTableBox(), "1 , 1 , 1 , 2 ");
		add(compFactory.createAttackSkillBox(), "3 ,  1 ");
		add(compFactory.createBuffSkillBox(), "3 , 2");
		add(compFactory.createSettingBox(), "1 , 4 , 3 , 4");

		// this.comMasteryFilter.setModel(new
		// MasteryFilterModel(this.trainer.getMastryList()));

		binding();

		updateAttackSkillList();
	}

	private void binding() {

		this.comMasteryFilter.addItemListener(this);
		this.checkAttackSkills.addItemListener(this);
		this.checkBuffSkills.addItemListener(this);

		this.comAttackSkillFilter.addItemListener(this);

		this.tblSkill.getSelectionModel().addListSelectionListener(this);
		
		this.btnAddAttackSkill.addActionListener(this);
		this.btnRemoveAttackSkill.addActionListener(this);
		this.btnMoveUpAttackSkill.addActionListener(this);
		this.btnMoveDownAttackSkill.addActionListener(this);
		
		this.tblAttackSkill.getSelectionModel().addListSelectionListener(this);

	}

	@EventListener
	public void onTrainerLoaded(TrainerLoadedEvent event) {

		// fill mastry combo box
	
		updateTrainerMasteries();

		updateSkillList();
 
		updateAttackSkillList();
	}

	
	@EventListener
	public void onSkillsUpdated(TrainerSkillsUpdatedEvent event) { 
		updateSkillList();  
		updateAttackSkillList();
	}
	
	@EventListener
	public void onMasteryLvlUp(MasteryLvlUpEvent event ) { 
		updateTrainerMasteries();
	}
	
	private void updateTrainerMasteries() {

		comMasteryFilter.removeAllItems();
		comMasteryFilter.addItem(new MasteryEntry(-1, "All"));

		trainer.getMastryList().getAllMasteries().stream().filter((m) -> m.getMasteryLevel() > 0).forEach((m) -> {
			comMasteryFilter.addItem(new MasteryEntry(m.getMasteryID(), m.toString()));

		});

	}

	private void updateSkillList() {
		// this.tblSkill.removeRowSelectionInterval(0, this.tblSkill.getRowCount() - 1)
		// ;

		ListSelectionModel listSelectionModel = this.tblSkill.getSelectionModel();
		listSelectionModel.clearSelection();

		;
		this.tblSkillModel.removeAll();
		MasteryEntry masteryArg = (MasteryEntry) this.comMasteryFilter.getSelectedItem();

		this.trainer.getSkills().stream().filter((s) -> {

			if (masteryArg == null || masteryArg.id == -1)
				return true;
			else if (masteryArg.id == s.getMasteryId())
				return true;
			else
				return false;
		}).filter((skill) -> {
			SkillType skillType = skill.getType();

			return (isBuffSkill(skillType) && checkBuffSkills.isSelected())
					|| (isAttackSkill(skillType) && checkAttackSkills.isSelected());

		}).map(skill->new SkillTableEntry(null, skill.getName(), skill.getSkillLvl())).forEach((skill) -> {
			this.tblSkillModel.addSkill(skill);

		});

	}

	@Data
	@AllArgsConstructor
	protected static class MasteryEntry {

		private int id;

		private String name;

		@Override
		public String toString() {
			return name;
		}

	}

	@Data
	@AllArgsConstructor
	protected static class MonsterTypeEntry {

		private String name;
		private MonsterType type;

		@Override
		public String toString() {
			return name;
		}

	}
	
	
	@AllArgsConstructor
	protected static class SkillTableEntry { 
		
		protected Icon skillIcon ; 
		protected String skillName ; 
		protected int skillLvl ; 
		
		
	}

	private void updateAttackSkillList() {
		MonsterTypeEntry type = (MonsterTypeEntry) this.comAttackSkillFilter.getSelectedItem();

		this.tblAttackSkill.getSelectionModel().clearSelection();
		this.tblAttackSkillModel.removeAll();
		List<SkillTableEntry> skills   = 
		this.userConfig.getAttakListFor(type.type)
				.stream().map((skillName)->{ 
					return trainer.findSkill(skillName).map((skill)->{
						
						   return new SkillTableEntry(null, skill.getName(), skill.getSkillLvl()) ;
					}).orElse(new SkillTableEntry(null, skillName, 0)) ;
				}).collect(Collectors.toList()) ; 
		this.tblAttackSkillModel.addAll(skills);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {

		Object source = e.getSource();

		if (source == this.comMasteryFilter || source == this.checkAttackSkills || source == this.checkBuffSkills) {

			if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {

				System.out.println("Updating Skill List ");
				updateSkillList();

			}
		} else if (source == this.comAttackSkillFilter) {

			if (e.getStateChange() == ItemEvent.SELECTED) {

				updateAttackSkillList();

			}
		}

	}

	@Override
	public void valueChanged(ListSelectionEvent e) {

		if (e.getValueIsAdjusting())
			return;

		int viewRow = this.tblSkill.getSelectedRow();

		Object source = e.getSource();

		if (source == this.tblSkill.getSelectionModel()) {

			if (viewRow < 0)
				return;

			// int modelRow = this.tblSkill.convertRowIndexToModel(viewRow);

			SkillTableEntry skillEntry = this.tblSkillModel.getRowObject(viewRow); ; 
			
			this.trainer.findSkill(skillEntry.skillName) 
			.ifPresent((target)->{
				SkillType skillType = target.getType();
				boolean isBuffSkill = isBuffSkill(skillType) && !isAttackSkill(skillType);

				this.btnAddAttackSkill.setEnabled(!isBuffSkill && !this.tblAttackSkillModel.contains(target.getName()));
				this.btnAddBuffSkill.setEnabled(isBuffSkill && !this.tblBuffSkillModel.contains(target.getName()));
	
			});
			
			
		} else if (source == this.tblAttackSkill.getSelectionModel()) {

			// boolean isSelectionEmpty =
			// this.tblAttackSkill.getSelectionModel().isSelectionEmpty();
			int selectedRow = this.tblAttackSkill.getSelectedRow();

			this.btnRemoveAttackSkill.setEnabled(selectedRow >= 0);
			this.btnMoveDownAttackSkill
					.setEnabled(selectedRow >= 0 && selectedRow < this.tblAttackSkill.getRowCount() - 1);// before last
																											// row
			this.btnMoveUpAttackSkill.setEnabled(selectedRow > 0);
		}

	}

	private SkillTableEntry getSelectedSkill() {
		int selectedRow = this.tblSkill.getSelectedRow();

		if (selectedRow >= 0) {

			return this.tblSkillModel.getRowObject(selectedRow);

		}

		return null;

	}

	public MonsterType getSelectedMonsterType() {
		return ((MonsterTypeEntry) this.comAttackSkillFilter.getSelectedItem()).type;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		Object source = e.getSource();

		if (source == this.btnAddAttackSkill) {

			SkillTableEntry selectedSkill = getSelectedSkill();

			if (selectedSkill == null)
				return;

			if (!this.tblAttackSkillModel.contains(selectedSkill.skillName)) {
				this.tblAttackSkillModel.addSkill(selectedSkill);

				this.userConfig.addSkill(getSelectedMonsterType(), selectedSkill.skillName);

				this.btnAddAttackSkill.setEnabled(false);
			}
		} else if (source == this.btnRemoveAttackSkill) {

			int selectedRow = this.tblAttackSkill.getSelectedRow();

			SkillTableEntry target = this.tblAttackSkillModel.getRowObject(selectedRow);
			this.userConfig.removeSkill(getSelectedMonsterType(), target.skillName);

			if (selectedRow >= 0) {

				this.tblAttackSkillModel.removeRow(selectedRow);
				this.tblAttackSkill.getSelectionModel();
				int rem = this.tblAttackSkill.getRowCount();

				if (selectedRow < rem || rem > 0) {

					selectedRow = (selectedRow == rem) ? selectedRow - 1 : selectedRow;
					this.tblAttackSkill.setRowSelectionInterval(selectedRow, selectedRow);

				}
			}

		} else if (source == this.btnMoveUpAttackSkill) {

			int selectedRow = this.tblAttackSkill.getSelectedRow();

			if (selectedRow <= 0)
				return;
			
			this.tblAttackSkillModel.move(selectedRow, selectedRow-1);
			this.userConfig.swapAttackSkill(getSelectedMonsterType(), selectedRow, selectedRow - 1);
			this.tblAttackSkill.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);

		}else if(source == this.btnMoveDownAttackSkill) { 
			int selectedRow = this.tblAttackSkill.getSelectedRow();

			if (selectedRow >=  this.tblAttackSkill.getRowCount() -1)
				return;
			
			this.tblAttackSkillModel.move(selectedRow, selectedRow + 1);
			this.userConfig.swapAttackSkill(getSelectedMonsterType(), selectedRow, selectedRow + 1);
			this.tblAttackSkill.getSelectionModel().setSelectionInterval(selectedRow + 1, selectedRow + 1);
			
			
		}

	}

	private boolean isBuffSkill(SkillType skillType) {

		return skillType != SkillType.Other && skillType != SkillType.Passive && skillType != SkillType.Imbue;
	}

	private boolean isAttackSkill(SkillType skillType) {
		return skillType == SkillType.Other;
	}

	public static void main(String args[]) {

		FlatDarkLaf.setup();

		JFrame frame = new JFrame();

		SkillTab skillTab = new SkillTab();
		skillTab.init();
		frame.add(skillTab);
		skillTab.tblSkillModel.addSkill(new SkillTableEntry(null, "SkillName", 10));
		skillTab.comMasteryFilter.addItem(new MasteryEntry(-1, "All"));
		// skillTab.tblSkillModel.fireTableDataChanged();
		frame.setPreferredSize(new Dimension(600, 600));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}

}
