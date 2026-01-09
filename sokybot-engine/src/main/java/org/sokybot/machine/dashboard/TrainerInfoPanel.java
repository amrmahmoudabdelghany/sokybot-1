package org.sokybot.machine.dashboard;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;

import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.sokybot.app.AppConstants;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateChanged;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machine.service.IChatManager;
import org.sokybot.game.asset.IMediaAssetProvider;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.utils.Helper;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.statemachine.annotation.WithStateMachine;

import com.formdev.flatlaf.FlatDarculaLaf;

// @WithStateMachine
public class TrainerInfoPanel extends JPanel implements ActionListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JLabel lblCharIcon;
	private JLabel lblCharName;
	private JLabel lblLvl;
	private JLabel lblSkillPoint;
	private JLabel lblGold;
	private JLabel lblPosX;
	private JLabel lblPosY;
	private JProgressBar pbHP;
	private JProgressBar pbMP;
	private JProgressBar pbEXP;
	private JProgressBar pbZerk;

	private JTextField chatText;

	private JButton btnSend;

	private final int CHAR_IMAGE_WIDTH = 120;
	private final int CHAR_IMAGE_HEIGHT = 100;
	private final int CHAR_IMAGE_CRADIUS = 40;

	// @Autowired
	private Trainer trainer;

	// @Autowired
	private IGameDataLookup gameDao;

	// @Autowired
	private IMediaAssetProvider assetProvider;

	// @Autowired
	private IChatManager chatManager;

	// @Value("${" + AppConstants.MACHINE_NAME + "}")
	private String trainerName;

	public TrainerInfoPanel() {

		BufferedImage bi = Helper.makeRoundedCorner(
				new BufferedImage(CHAR_IMAGE_WIDTH, CHAR_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB), CHAR_IMAGE_CRADIUS);

		this.lblCharIcon = new JLabel(new ImageIcon(bi));

		this.chatText = new JTextField(this.trainerName + ":" + "Hello world");
		this.lblLvl = new JLabel("0");
		this.btnSend = new JButton("Send");
		this.lblCharName = new JLabel("None");
		this.lblSkillPoint = new JLabel("0");
		this.lblGold = new JLabel("0");
		this.lblPosX = new JLabel("0");
		this.lblPosY = new JLabel("0");
		this.pbHP = new JProgressBar(JProgressBar.HORIZONTAL);
		this.pbMP = new JProgressBar(JProgressBar.HORIZONTAL);
		this.pbEXP = new JProgressBar(JProgressBar.HORIZONTAL);
		this.pbZerk = new JProgressBar(JProgressBar.HORIZONTAL);


	}

	private BasicProgressBarUI createBPUI() {
		return new BasicProgressBarUI() {
			@Override
			protected Color getSelectionBackground() {
				return Color.WHITE;
			}

			@Override
			protected Color getSelectionForeground() {
				return Color.WHITE;
			}
		 @Override
		public void paint(Graphics g, JComponent c) {
		
			 g.setColor(c.getBackground()); 
			 g.fillRect(0, 0, getWidth(), getHeight());
			 super.paint(g, c);
		}

		};
	}

	// @PostConstruct
	void init() {

		
		// this.pbHP.setValue(40);
		this.pbHP.setForeground(Color.decode("#800000"));
		// this.pbHP.setBackground(Color.decode("#ffb3b3"));
		this.pbHP.setBackground(new Color(255, 179, 179, 80));
		this.pbHP.setStringPainted(true);
		this.pbHP.setUI(createBPUI());
		this.pbHP.setOpaque(false);
		
	//	this.pbHP.setOpaque(true) ; 
		;
		// this.pbMP.setValue(50);
		this.pbMP.setForeground(Color.decode("#000080"));
		// this.pbMP.setBackground(Color.decode("#b3b3ff"));
		this.pbMP.setBackground(new Color(179, 179, 255, 80));
		this.pbMP.setStringPainted(true);
		this.pbMP.setUI(createBPUI());
		this.pbMP.setOpaque(false);
		
		// this.pbEXP.setValue(60);
		this.pbEXP.setForeground(Color.decode("#00802b"));
		// this.pbEXP.setBackground(Color.decode("#b3ffcc"));
		this.pbEXP.setBackground(new Color(179, 255, 204, 80));
		this.pbEXP.setStringPainted(true);
		this.pbEXP.setUI(createBPUI());
		this.pbEXP.setOpaque(false);
		
		// this.pbZerk.setValue(70);
		this.pbZerk.setForeground(Color.decode("#004080"));
		// this.pbZerk.setBackground(Color.decode("#b3d9ff"));
		this.pbZerk.setBackground(new Color(179, 217, 255, 80));
		this.pbZerk.setStringPainted(true);
		this.pbZerk.setUI(createBPUI());
		this.pbZerk.setOpaque(false);
		
		this.pbHP.setString("HP 0%");
		this.pbMP.setString("MP 0%");
		this.pbEXP.setString("EXP 0%");
		this.pbZerk.setString("Zerk 0%");

		//this.pbHP.setPreferredSize(new Dimension(128, 15));
		//this.pbMP.setPreferredSize(new Dimension(128, 15));
		//this.pbEXP.setPreferredSize(new Dimension(128, 15));
		//this.pbZerk.setPreferredSize(new Dimension(128, 15));

		;
		this.btnSend.addActionListener(this);

		this.trainer.addPropertyChangeListener(Trainer.ENTITY_PROPERTY, this);
		this.trainer.addPropertyChangeListener(Trainer.CHAR_NAME_PROPERTY, this);
		this.trainer.addPropertyChangeListener(Trainer.CHAR_LVL_PROPERTY, this);
		this.trainer.addPropertyChangeListener(Trainer.GOLD_PROPERTY, this);
		this.trainer.addPropertyChangeListener(Trainer.SP_PROPERTY, this);
		this.trainer.addPropertyChangeListener(Trainer.CURRENT_EXP, this) ; ;

		this.trainer.addPropertyChangeListener(Trainer.ZERK_COUNT_PROPERTY, this);
		
		this.trainer.addPropertyChangeListener(Trainer.POSITION_PROPERTY, this);
		//this.trainer.addPropertyChangeListener(ObservableTrainer.YOFFSET_PROPERTY, this);
		
		this.trainer.addPropertyChangeListener(Trainer.MAX_HP_PROPERTY, this);
		this.trainer.addPropertyChangeListener(Trainer.CURRENT_HP_PROPERTY, this);

		this.trainer.addPropertyChangeListener(Trainer.MAX_MP_PROPERTY, this);
		this.trainer.addPropertyChangeListener(Trainer.CURRENT_MP_PROPERTY, this);


		//setLayout(new GridBagLayout());
	//	GridBagConstraints gbc = new GridBagConstraints();
	//	gbc.gridx = 0;
	///	gbc.gridy = 0;
	//	gbc.insets = new Insets(5, 5, 5, 5);

		BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
		setLayout(layout);
		
		Box line = Box.createHorizontalBox();
		line.add(Box.createHorizontalGlue());
		line.add(this.lblCharIcon);
		line.add(Box.createHorizontalGlue());

		add(line);
		add(Box.createVerticalStrut(7));

		line = Box.createHorizontalBox()  ; 
		line.add(Box.createHorizontalStrut(5)) ; 
		line.add(createPbComp()) ; 
		line.add(Box.createHorizontalStrut(5)) ; 
		
		add(line) ; 

		add(Box.createVerticalStrut(7));

		line = Box.createHorizontalBox();
		line.add(createBoldLabel("Char :"));
		line.add(Box.createHorizontalStrut(5));
		line.add(this.lblCharName);
		line.add(Box.createHorizontalStrut(5));
		line.add(Box.createHorizontalGlue());
		line.add(createBoldLabel("Lvl :"));
		line.add(this.lblLvl);
		line.add(Box.createHorizontalGlue());
		

		add(line);
		add(Box.createVerticalStrut(3));
		
		line = Box.createHorizontalBox();
		line.add(createBoldLabel("Gold :"));
		line.add(Box.createHorizontalStrut(5));
		line.add(this.lblGold);
		line.add(Box.createHorizontalGlue());
		

		add(line);
		add(Box.createVerticalStrut(3));
		
		line = Box.createHorizontalBox();

		line.add(createBoldLabel("Skill Point :"));
		// line.add(Box.createHorizontalStrut(5));
		line.add(this.lblSkillPoint);
		line.add(Box.createHorizontalGlue());


		add(line);
		add(Box.createVerticalStrut(3));
		
		line = Box.createHorizontalBox();
		line.add(createBoldLabel("X :"));
		line.add(Box.createHorizontalStrut(5));
		line.add(this.lblPosX);
		line.add(Box.createHorizontalGlue());

		add(line);
		add(Box.createVerticalStrut(3));

		line = Box.createHorizontalBox() ; 
		line.add(createBoldLabel("Y :"));
		line.add(Box.createHorizontalStrut(5));
		line.add(this.lblPosY);
		line.add(Box.createGlue());

		add(line) ; 
		add(Box.createVerticalStrut(5)) ; 
		//this.add(infoBox, gbc);
		// gbc.gridy = 6;
		// this.add(this.chatText, gbc);
		// gbc.gridy = 7;
		// this.add(this.btnSend, gbc);

	}

	private JLabel createBoldLabel(String txt) {
		JLabel lbl = new JLabel(txt);
		Font f = lbl.getFont();
		lbl.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		;
		return lbl;
	}

	
	private JComponent createPbComp() { 
		
		Box box  = Box.createVerticalBox() ; 
		
		box.add(this.pbHP) ; 
		box.add(this.pbMP) ; 
		box.add(this.pbZerk) ; 
		box.add(this.pbEXP) ; 
		box.setOpaque(false);
		
		return box ; 
		
	}
	private JComponent createPbComp2() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		// gbc.insets = new Insets(0, 0, 0, 5);

		//panel.add(new JLabel("<html><b>HP :</b></html>"), gbc);
		panel.add(new JLabel(""), gbc);
		gbc.gridx = 1;
		panel.add(this.pbHP, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;

		panel.add(new JLabel("<html><b>MP :</b></html>"), gbc);
		gbc.gridx = 1;
		panel.add(this.pbMP, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;

		panel.add(new JLabel("<html><b>Zerk :</b></html>"), gbc);
		gbc.gridx = 1;
		panel.add(this.pbZerk, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;

		panel.add(new JLabel("<html><b>EXP :</b></html>"), gbc);
		gbc.gridx = 1;
		panel.add(this.pbEXP, gbc);

		return panel;
	}
	
	@StateChanged(target = MachineState.DISCONNECTING)
	public void reset() { 
		BufferedImage bi = Helper.makeRoundedCorner(
				new BufferedImage(CHAR_IMAGE_WIDTH, CHAR_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB), CHAR_IMAGE_CRADIUS);

		this.lblCharIcon.setIcon(new ImageIcon(bi));

		this.trainer.setName("None");
		this.trainer.setLevel((byte)0) ; 
		this.trainer.setGold(0);
		this.trainer.setSkillPoint(0) ; 
		this.trainer.setXOffset(0); 
		this.trainer.setYOffset(0) ; 
		this.trainer.setCharHP(0) ; 
		this.trainer.setCharMP(0);
		this.trainer.setCharEXPOffset(0);
		this.trainer.setZerkCount((byte)0) ; 
	
		//this.lblCharName.setText("None");
		//this.lblLvl.setText("0") ; 
		//this.lblGold.setText("0") ; 
		//this.lblSkillPoint.setText("0") ; ;
		//this.lblPosX.setText("0") ; 
		//this.lblPosY.setText("0") ;
		
		
		//this.pbHP.setValue(0);
		//this.pbHP.setString("HP 0%");
	
		//this.pbMP.setValue(0);
		//this.pbMP.setString("MP 0%");
	
		//this.pbZerk.setValue(0);
		//this.pbZerk.setString("Zerk 0%");
	
		//this.pbEXP.setValue(0);
		//this.pbEXP.setString("EXP 0%");
	
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == this.btnSend) {
			String[] strs = this.chatText.getText().split(":");
			if (strs.length == 2) {
				this.chatManager.logMessage(strs[0], strs[1]);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		String prop = evt.getPropertyName();

		switch (prop) {

		case Trainer.ENTITY_PROPERTY:
			
			this.assetProvider.findCharacterIcon(this.trainer.getRefId()).ifPresent((image)->{

				this.lblCharIcon.setIcon(
						new ImageIcon(org.sokybot.swing.SwingUtils.processImage(image, CHAR_IMAGE_WIDTH, CHAR_IMAGE_HEIGHT, CHAR_IMAGE_CRADIUS)));
			});
			break;
		case Trainer.CHAR_NAME_PROPERTY:
			
			this.lblCharName.setText(this.trainer.getName());
			break;
		case Trainer.CHAR_LVL_PROPERTY :
			this.lblLvl.setText(String.valueOf(this.trainer.getLevel()));	
			break ; 
		case Trainer.CURRENT_EXP : 
			this.gameDao.getLvlEXP(this.trainer.getLevel()).ifPresent((exp)->{
				double percentage = ((this.trainer.getCharEXPOffset()*100) / exp) ; 
				this.pbEXP.setValue((int)percentage) ;
				this.pbEXP.setString(String.format("EXP %.2f%%", percentage )) ;
				
			});  
			break ; 
		case Trainer.GOLD_PROPERTY :
			this.lblGold.setText(String.valueOf(this.trainer.getGold()));			
			break ; 
		case Trainer.SP_PROPERTY : 
			this.lblSkillPoint.setText(String.valueOf(this.trainer.getSkillPoint()));		
			break ; 
		case Trainer.MAX_HP_PROPERTY:
		case Trainer.CURRENT_HP_PROPERTY:
			int hpval = this.trainer.getHPPercentage();
			this.pbHP.setValue(hpval);
			this.pbHP.setString("HP " + String.valueOf(hpval) + "%");
			
			break;
		case Trainer.MAX_MP_PROPERTY:
		case Trainer.CURRENT_MP_PROPERTY:
			int mpval = this.trainer.getMPPercentage();
			this.pbMP.setValue(mpval);
			this.pbMP.setString("MP " + String.valueOf(mpval) + "%");
			
			break;
		case Trainer.ZERK_COUNT_PROPERTY: 
			int zerkVal = this.trainer.getZerkPercentage() ; 
			this.pbZerk.setValue(zerkVal) ; 
			this.pbZerk.setString("Zerk " + String.valueOf(zerkVal) + "%");
			
			break ; 
		case Trainer.POSITION_PROPERTY:
		
				this.lblPosX.setText(String.valueOf(this.trainer.getX())) ;
				this.lblPosY.setText(String.valueOf(this.trainer.getY())) ;
				
			break ; 
		}
		

	}

	private static int counter = 0 ; 
	public static void main(String args[]) {

		FlatDarculaLaf.setup();

		JFrame frame = new JFrame();
		
		JPanel blackpanel = new JPanel() ; 
		blackpanel.setBackground(Color.black) ; 
		blackpanel.setPreferredSize(new Dimension(100 , 100));
		JButton btnPlus = new JButton("+");
		JButton btnMin = new JButton("-") ;
		Box btnBox = Box.createHorizontalBox() ; 
		btnBox.add(btnPlus ) ; 
		btnBox.add(btnMin) ; 
		
		JPanel content = (JPanel) frame.getContentPane();

		TrainerInfoPanel infoPanel = new TrainerInfoPanel();

		infoPanel.trainer = new Trainer();
		infoPanel.init();
		infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		content.setLayout(new BorderLayout());
		content.add(infoPanel, BorderLayout.NORTH);
		content.add(blackpanel , BorderLayout.CENTER) ;
		content.add(btnBox , BorderLayout.SOUTH) ; 
		frame.setLocationRelativeTo(null);
		frame.pack();
		btnPlus.addActionListener((evt)->{
			counter++ ; 
			infoPanel.pbHP.setValue(counter); 
		});	
		btnMin.addActionListener((evt)->{
			counter-- ; 
			infoPanel.pbHP.setValue(counter); 
		});	
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setPreferredSize(new Dimension(500, 500));
		frame.setVisible(true);
	}
}
