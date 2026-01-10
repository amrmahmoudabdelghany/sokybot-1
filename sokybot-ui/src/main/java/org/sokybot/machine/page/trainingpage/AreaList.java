package org.sokybot.machine.page.trainingpage;



import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.math.NumberUtils;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.settings.Settings;
import org.sokybot.settings.TrainingArea;
import org.sokybot.settings.TrainingAreaSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.components.FlatTextField;

import info.clearthought.layout.TableLayout;

@Component
public class AreaList extends JPanel implements ActionListener, ListSelectionListener {

	protected JList<String> areaList;
	// protected FlatTextField txtAreaName ;
	protected JButton btnActive;
	protected JButton btnRemove;
	protected JButton btnAdd;

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private AreaListModel areaListModel;

	@Autowired
	private TrainingAreaSettings settings;

	private Map<String, JPanel> cards = new HashMap<>();

	private CardLayout card;

	private JPanel cardContainer;

	// @Autowired
	// private LocationBox locationBox;

	protected AreaList() {

	}

	@PostConstruct
	private void init() {
		this.areaList = new JList<String>(areaListModel);
		
		this.areaList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			   if(e.getClickCount() == 2) { 
				   setActive(areaList.locationToIndex(e.getPoint()));
			   }
			}
		});
		this.btnActive = new JButton("Active");
		this.btnRemove = new JButton("-");
		this.btnAdd = new JButton("+");

		this.card = new CardLayout(5, 0);
		this.cardContainer = new JPanel();
		this.cardContainer.setLayout(card);

		this.btnActive.addActionListener(this);
		this.btnAdd.addActionListener(this);
		this.btnRemove.addActionListener(this);

		this.areaList.addListSelectionListener(this);
		double border = 5;

		double size[][] = { { border, 0.25, TableLayout.PREFERRED, border }, // cols
				{ border, TableLayout.PREFERRED, border } // rows
		};
		setLayout(new TableLayout(size));
		add(createListBox(), "1 , 1");
		add(this.cardContainer, "2 , 1");
		String areas[] = settings.getTrainingAreaNames();

		for (String areaName : areas) {
			addCard(settings.getArea(areaName));
		}

		this.areaList.setSelectedIndex(this.areaListModel.getActiveIndex());
	}

	private void addCard(TrainingArea area) {
		String name = area.getName();
		JPanel card = createLocationBoxPanel(area);
		this.cardContainer.add(card, name);
		this.cards.put(name, card);
	}

	private void showArea(String name) {
		this.card.show(this.cardContainer, name);
	}

	private JPanel getLocationCardByName(String name) {

		java.awt.Component[] comps = this.cardContainer.getComponents();
		for (java.awt.Component c : comps) {

			String cName = c.getName();
			if (cName != null && !cName.isBlank())
				if (cName.equals(name)) {
					return (JPanel) c;
				}
		}
		throw new IllegalStateException("Could not find Card with name " + name);
	}

	private void setActive(int index) { 
		String currentActive = this.settings.getActiveArea().getName() ; 
		this.cards.get(currentActive).setBorder(BorderFactory.createTitledBorder(" " + currentActive + " " ));
	
		this.areaListModel.setActive(index);
		TrainingArea area = this.settings.getActiveArea();
		String name = area.getName();
		name = " " + name + ((this.settings.isActiveArea(area) ? " [Active] " : " "));

		this.cards.get(area.getName()).setBorder(BorderFactory.createTitledBorder(name));
			
	}
	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == btnActive) {
			setActive(this.areaList.getSelectedIndex());
		} else if (e.getSource() == btnAdd) {
			prompateAreaName();

		} else if (e.getSource() == btnRemove) {
			int selectedIndex = this.areaList.getSelectedIndex();
			try {
				String targetName = this.areaListModel.get(selectedIndex) ; 
				
				this.areaList.setSelectedIndex(this.areaListModel.getActiveIndex());
				this.areaListModel.remove(selectedIndex);
			   JPanel targetCard = 	this.cards.remove(targetName) ; 
			   this.cardContainer.remove(targetCard);
			} catch (IllegalStateException ex) {
				JOptionPane.showMessageDialog(this, ex.getMessage());
			}

		}

	}

	private void prompateAreaName() {

		FlatTextField txtName = new FlatTextField();
		JLabel lblError = new JLabel(" ");
		lblError.setForeground(Color.RED);
		txtName.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				lblError.setText(" ");
				txtName.setOutline(null);

			}

		});
		JOptionPane opt = new JOptionPane(new JComponent[] { new JLabel("Enter New Area Name "), txtName, lblError },
				JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

		JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this));

		opt.addPropertyChangeListener((prop) -> {
			if (prop.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
				Object newVal = prop.getNewValue();
				if (newVal instanceof Integer) {
					int val = (int) newVal;
					String userInput = txtName.getText();
					if (val == JOptionPane.OK_OPTION) {
						if (userInput == null || userInput.isBlank()) {
							lblError.setText("Please enter a valid value");
							opt.setValue(JOptionPane.UNINITIALIZED_VALUE);
							txtName.setOutline("error");
						} else if (settings.containsAreaName(userInput)) {
							lblError.setText("Area name must be unique");
							opt.setValue(JOptionPane.UNINITIALIZED_VALUE);
							txtName.setOutline("error");
						} else {
							handleAreaNameInput(userInput);
							dialog.dispose();

						}

					} else if (val == JOptionPane.CANCEL_OPTION || val == JOptionPane.CLOSED_OPTION) {
						dialog.dispose();
					}
				}
			}
		});

		dialog.setTitle("Area Name");
		dialog.setContentPane(opt);
		dialog.setLocationRelativeTo(this);
		dialog.pack();
		dialog.setModal(true);
		dialog.setVisible(true);

	}

	private JPanel createLocationBoxPanel(TrainingArea area) {
		LocationBox box = this.ctx.getBean(LocationBox.class, ctx.getBean(Trainer.class), area);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(box, BorderLayout.CENTER);
		String name = area.getName();
		name = " " + name + ((this.settings.isActiveArea(area) ? " [Active] " : " "));
		panel.setBorder(BorderFactory.createTitledBorder(name));
		return panel;
	}

	private void handleAreaNameInput(String userInput) {
		TrainingArea area = settings.getArea(userInput);
		this.areaListModel.addElement(area.getName());
		addCard(area);
		this.areaList.setSelectedIndex(this.areaListModel.getSize() - 1);

	}

	private Box createListBox() {
		Box box = Box.createVerticalBox();
		box.add(Box.createGlue());
		box.add(new JScrollPane(this.areaList));
		box.add(Box.createVerticalStrut(2));
		box.add(createBtnBox());
		box.add(Box.createGlue());
		return box;
	}

	private Box createBtnBox() {
		Box box = Box.createHorizontalBox();

		box.add(this.btnAdd);
		box.add(Box.createHorizontalStrut(5));
		box.add(this.btnRemove);
		box.add(Box.createHorizontalGlue());
		box.add(Box.createHorizontalStrut(5));
		box.add(this.btnActive);

		return box;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			String selVal = this.areaList.getSelectedValue();
			if (selVal != null) {

				if (selVal.contains("[Active]"))
					selVal = selVal.substring(0, selVal.indexOf("[")).trim();

				// this.locationBox.setSelectedArea(selVal);
				showArea(selVal);
			}
		}
	}

	public static void main(String args[]) {
		FlatDarkLaf.setup();
		JFrame frame = new JFrame();

		TrainingAreaSettings s = new TrainingAreaSettings();
		TrainingArea area = null;
		area = s.getArea("Amr");
		area.setAreaX(10);
		area.setAreaY(20);
		area.setAreaR(30);

		area = s.getArea("Mahmoud");
		area.setAreaX(10);
		area.setAreaY(20);
		area.setAreaR(30);

		area = s.getArea("Ong");
		area.setAreaX(10);
		area.setAreaY(20);
		area.setAreaR(30);

		area = s.getArea("Koko");
		area.setAreaX(10);
		area.setAreaY(20);
		area.setAreaR(30);

		s.setActiveArea("Amr");
		AreaListModel model = new AreaListModel(s);
		model.init();
		AreaList areaList = new AreaList();
		areaList.areaListModel = model;
		// areaList.locationBox = new LocationBox(null, area);
		areaList.settings = s;
		areaList.init();
		frame.setLayout(new BorderLayout());
		frame.add(areaList, BorderLayout.CENTER);
		frame.setLocationRelativeTo(null);
		frame.setPreferredSize(new Dimension(600, 400));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
