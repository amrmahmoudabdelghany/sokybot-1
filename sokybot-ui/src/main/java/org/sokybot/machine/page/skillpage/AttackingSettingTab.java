package org.sokybot.machine.page.skillpage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.annotation.PostConstruct;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.sokybot.app.AppSwingUtilities;
import org.sokybot.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import info.clearthought.layout.TableLayout;

import static org.sokybot.app.AppSwingUtilities.createHorizontalBox; 
import static org.sokybot.app.AppSwingUtilities.createVerticalBox; 


@Component
public class AttackingSettingTab extends JPanel {

	private JCheckBox checkDontAttackMob;
	private JCheckBox checkiterateSkillsPerMob;
	private JCheckBox checkUseNormalAttack;
	private JCheckBox checkUseNormalAttackEventMob;
	private JCheckBox checkuseImbueSkill;
	private JCheckBox checkDontFollowTarget;
	private JComboBox<String> imbueSkills;

	@Autowired
	private Settings settings;

	public AttackingSettingTab() {

		this.checkDontAttackMob = new JCheckBox("Don`t Attack Monsters");
		this.checkiterateSkillsPerMob = new JCheckBox("Iterate Skills Per Monster");
		this.checkUseNormalAttack = new JCheckBox("Use Normal Attack ");
		this.checkUseNormalAttackEventMob = new JCheckBox("Use Normal Attack On Event Monsters");
		this.checkuseImbueSkill = new JCheckBox("Use Imbue Skill ");
		this.checkDontFollowTarget = new JCheckBox("Do not follow target out of range ");
		this.imbueSkills = new JComboBox<>();
	}

	
	
	@PostConstruct
	private void init( ) { 
		
		
		bind() ; 
		this.checkDontAttackMob.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					System.out.println("Selected") ;
				}else if(e.getStateChange() == ItemEvent.DESELECTED) { 
					System.out.println("Deselected") ;
				}else { 
					System.out.println("Unexpected event") ; 
				}
			}
		});
		
		int border  = 5 ; 
		
		double [][] sizes = {
				{border ,TableLayout.PREFERRED ,  border} , //cols
				{border , TableLayout.PREFERRED,
					TableLayout.PREFERRED ,
					TableLayout.PREFERRED ,
					TableLayout.PREFERRED ,
					TableLayout.PREFERRED , 
					TableLayout.PREFERRED , 
					border}  // rows
		};
		
		Box imbueBox = createHorizontalBox(
				this.checkuseImbueSkill  ,this.imbueSkills  , Box.createHorizontalGlue()) ; 
		
		TableLayout table = new TableLayout(sizes) ; 
		setLayout(table);
		add(imbueBox , "1 , 1") ;
		add(this.checkDontAttackMob , "1 , 2") ;
		add(this.checkDontFollowTarget , "1 , 3") ;
		add(this.checkUseNormalAttack , "1 , 4") ;
		add(this.checkUseNormalAttackEventMob , "1 , 5") ;
		add(this.checkiterateSkillsPerMob , "1 , 6") ;
	}
	
	
	private void bind() { 
		
	}
	
	public static void main(String args[]) {

		AttackingSettingTab tab = new AttackingSettingTab();
		tab.init();
		JFrame frame = new JFrame();

		JPanel content = (JPanel) frame.getContentPane();

		content.setLayout(new GridBagLayout());

		content.add(tab);

		frame.setPreferredSize(new Dimension(400, 400));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}
	
}
