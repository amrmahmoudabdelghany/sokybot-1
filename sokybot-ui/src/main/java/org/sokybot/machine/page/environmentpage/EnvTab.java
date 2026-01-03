package org.sokybot.machine.page.environmentpage;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.StrokeBorder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;

import info.clearthought.layout.TableLayout;

@Component
public class EnvTab extends JPanel {

	@Autowired
	private MonsterTable monsterTable;

	@Autowired
	private SroMapViewer mv;

	private JTabbedPane infoTab;

	@PostConstruct
	private void init() {
		this.infoTab = new JTabbedPane();
		this.infoTab.putClientProperty(TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB, true);
		this.infoTab.add("Monsters", this.monsterTable);

		int border = 5;

		double sizes[][] = { { border, TableLayout.PREFERRED, TableLayout.FILL, border }, // columns
				{ border, TableLayout.FILL, border } // rows
		};

		TableLayout layout = new TableLayout(sizes);

		setLayout(layout);

		add(getMV(), "1 , 1 , C , C");
		add(this.infoTab, "2 , 1 ");

	}

	private JPanel getMV() {

		JPanel border = new JPanel();
		border.setLayout(new BorderLayout());
		border.add(this.mv, BorderLayout.CENTER);
		
		border.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
	//	Border b = BorderFactory.create
		//border.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		return border;
	}

	public static void main(String args[]) {
		FlatDarkLaf.setup();
		JFrame frame = new JFrame();
		SroMapViewer map = new SroMapViewer();
		map.init();
		EnvTab tab = new EnvTab();
		tab.monsterTable = new MonsterTable();
		tab.mv = map;
		tab.init();

		frame.setLayout(new BorderLayout());
		frame.add(tab, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(800, 800));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}

}
