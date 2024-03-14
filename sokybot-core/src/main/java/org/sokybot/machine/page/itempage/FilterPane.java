package org.sokybot.machine.page.itempage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.sokybot.machinegroup.gamemodel.Gender;
import org.sokybot.machinegroup.gamemodel.Race;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.machinegroup.gamemodel.item.ItemType;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;

import info.clearthought.layout.TableLayout;

//@Component
public class FilterPane extends JPanel  {

	// general
	protected final JCheckBox chkGGMall = new JCheckBox("Male");
	protected final JCheckBox chkGGFemale = new JCheckBox("Female");
	protected final JCheckBox chkGRChinese = new JCheckBox("Chinese");
	protected final JCheckBox chkGREuropean = new JCheckBox("European");

	protected final JSpinner spDegreeStart = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
	protected final JSpinner spDegreeEnd = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

	// Equipment
	protected final JCheckBox chkArmor = new JCheckBox("Armor");
	protected final JCheckBox chkProtector = new JCheckBox("Protector");
	protected final JCheckBox chkGarment = new JCheckBox("Garment");

	protected final JCheckBox chkHead = new JCheckBox("Head");
	protected final JCheckBox chkShoulder = new JCheckBox("Shoulder");
	protected final JCheckBox chkChest = new JCheckBox("Chest");
	protected final JCheckBox chkBoot = new JCheckBox("Boot");
	protected final JCheckBox chkLeg = new JCheckBox("Leg");
	protected final JCheckBox chkHand = new JCheckBox("Hand");

	// Accessories
	protected final JCheckBox chkRing = new JCheckBox("Ring");
	protected final JCheckBox chkNecklace = new JCheckBox("Necklace");
	protected final JCheckBox chkEarring = new JCheckBox("Earring");

	// Weapons
	protected final JCheckBox chkWCBlade = new JCheckBox("Blade");
	protected final JCheckBox chkWCSword = new JCheckBox("Sword");
	protected final JCheckBox chkWCGlave = new JCheckBox("Glave");
	protected final JCheckBox chkWCSpear = new JCheckBox("Spear");
	protected final JCheckBox chkWCBow = new JCheckBox("Bow");
	protected final JCheckBox chkWEStaff = new JCheckBox("Staff");
	protected final JCheckBox chkWEOneHandSword = new JCheckBox("1-H Sword");
	protected final JCheckBox chkWETwoHandSword = new JCheckBox("2-H Sword");
	protected final JCheckBox chkWECRod = new JCheckBox("C-Rod");
	protected final JCheckBox chkWEWRod = new JCheckBox("W-Rod");
	protected final JCheckBox chkWEXBow = new JCheckBox("X-Bow");
	protected final JCheckBox chkWEDagger = new JCheckBox("Dagger");
	protected final JCheckBox chkWEHarp = new JCheckBox("Harp");
	protected final JCheckBox chkWEAxe = new JCheckBox("Axe");
	protected final JCheckBox chkWShield = new JCheckBox("Shield");

	// Consumables

	protected JCheckBox chkHPPotion;
	protected JCheckBox chkHPGrain;
	protected JCheckBox chkMPPotion;
	protected JCheckBox chkMPGrain;
	protected JCheckBox chkPurificationPill;
	protected JCheckBox chkUniversalPill;

	// Alchemy

	protected JCheckBox chkElixir;
	protected JCheckBox chkTablet;


	protected int getEndDegree() {
		return (Integer) this.spDegreeEnd.getValue();
	}

	protected int getStartDegree() {
		return (Integer) this.spDegreeStart.getValue();
	}

	@PostConstruct
	protected void init() {
		// this.itemsFilter = genderFilter() ;
		// this.itemsFilter.and(raceFilter()) ;
		int border = 5;

		double size[][] = { { border, TableLayout.PREFERRED, border }, // cols
				{ border, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED,
						border }// rows
		};

		this.spDegreeStart.setMaximumSize(new Dimension(50, 20));
		this.spDegreeEnd.setMaximumSize(new Dimension(50, 20));
		setLayout(new TableLayout(size));

		add(addBorder(createGeneralBox(), "General Filter"), "1 , 1");
		add(addBorder(createClothingBox(), "Closthes"), "1 , 2");
		add(addBorder(createAccessoryBox(), "Accessory"), "1 , 3");
		add(addBorder(createWeaponBox(), "Weapons"), "1 , 4");
	}

	private JPanel addBorder(JPanel panel, String title) {

		panel.setBorder(BorderFactory.createTitledBorder(title));
		return panel;
	}

	private JPanel createGeneralBox() {

		JPanel box = new JPanel();
		int border = 5;

		double size[][] = { { border, 0.25, 0.25, 0.25, border }, // cols
				{ border, 0.25, 0.25, 0.25, 0.25, border } // rows
		};

		box.setLayout(new TableLayout(size));

		box.add(new JLabel("Gender  "), "1 , 1");
		box.add(this.chkGGMall, " 2 , 1");
		box.add(this.chkGGFemale, "3 , 1");

		box.add(new JLabel("Race  "), " 1 , 2");
		box.add(this.chkGRChinese, "2 , 2");
		box.add(this.chkGREuropean, "3 , 2");

		box.add(new JLabel("Degree  "), " 1 , 3");
		box.add(this.spDegreeStart, "2 , 3");
		box.add(this.spDegreeEnd, "3 , 3");

		// box.add(createTypicalLine(new JLabel("Degree ") , this.spDegreeStart , new
		// JLabel(" To ") , this.spDegreeEnd) , "1 , 4 , 3 , 4 " ) ;

		return box;
	}

	private JPanel createClothingBox() {

		JPanel box = new JPanel();
		int border = 5;

		double size[][] = { { border, 0.25, 0.25, 0.25, border }, // cols
				{ border, 0.25, 0.25, 0.25, 0.25, border } // rows
		};

		box.setLayout(new TableLayout(size));

		box.add(this.chkArmor, " 1 , 1 ");
		box.add(this.chkProtector, " 2 , 1 ");
		box.add(this.chkGarment, " 3 , 1 ");

		box.add(this.chkHead, "1 , 2");
		box.add(this.chkShoulder, "2 ,2 ");

		box.add(this.chkChest, "1 , 3");
		box.add(this.chkBoot, "2 , 3 ");

		box.add(this.chkLeg, "1 , 4");
		box.add(this.chkHand, "2 , 4");

		return box;
	}

	

	private JPanel createAccessoryBox() {
		JPanel box = new JPanel();
		int border = 5;

		double size[][] = { { border, 0.25, 0.25, 0.25, border }, // cols
				{ border, TableLayout.PREFERRED, border } // rows
		};

		box.setLayout(new TableLayout(size));

		box.add(this.chkRing, "1 , 1");
		box.add(this.chkNecklace, "2 , 1 ");
		box.add(this.chkEarring, "3 , 1");

		return box;
	}

	private JPanel createWeaponBox() {
		JPanel box = new JPanel();
		int border = 5;

		double size[][] = { { border, 0.25, 0.25, 0.25, border }, // cols
				{ border, TableLayout.PREFERRED, TableLayout.PREFERRED, border, TableLayout.PREFERRED,
						TableLayout.PREFERRED, TableLayout.PREFERRED, border } // rows
		};

		box.setLayout(new TableLayout(size));

		box.add(this.chkWCBlade, "1 , 1");
		box.add(this.chkWCSpear, "2 , 1 ");
		box.add(this.chkWCGlave, "3 , 1");

		box.add(this.chkWCSword, "1 , 2");
		box.add(this.chkWCBow, "2 , 2");

		box.add(this.chkWEOneHandSword, "1 , 4");
		box.add(this.chkWETwoHandSword, "2 , 4");
		box.add(this.chkWEXBow, "3 , 4");

		box.add(this.chkWEDagger, "1 , 5 ");
		box.add(this.chkWEAxe, "2 , 5");
		box.add(this.chkWEHarp, "3 , 5");

		box.add(this.chkWEStaff, "1 , 6");
		box.add(this.chkWECRod, "2 , 6");
		box.add(this.chkWEWRod, "3 , 6");

		return box;

	}

	

	private Box createTypicalLine(java.awt.Component... comps) {
		Box line = Box.createHorizontalBox();

		for (int i = 0; i < comps.length - 1; i++) {
			line.add(comps[i]);
			line.add(Box.createHorizontalStrut(3));
		}
		line.add(comps[comps.length - 1]);

		return line;
	}



}
