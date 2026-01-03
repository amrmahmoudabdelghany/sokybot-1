package org.sokybot.app.machinebuilder;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.sing_group.gc4s.dialog.AbstractInputJDialog;
import org.sokybot.ISokybotContext;
import org.sokybot.app.AppConstants;
import org.sokybot.app.machinebuilder.Order.OrderType;
import org.sokybot.service.ISroDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MachineBuilderDialog extends AbstractInputJDialog implements ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Autowired
	private GameDataPanel gameDataPanel;

	@Autowired
	private MachineDataPanel machineDataPanel;

	// @Autowired
	// private IMachineGroupService machineGroupService;

	// @Autowired
	// private ISokybotContext sokybotContext ;

	@Autowired
	private ApplicationContext ctx;

	// private Set<String> groupNames ;

	private String[] groupNames;

	private JComboBox<String> cmbGroups;

	private Box page;

	private String description;

	private Order order = null;

	
	public MachineBuilderDialog(JFrame parrent) {

		super(parrent);

		this.description = "Create new bot ";
		
	}

	public MachineBuilderDialog(JFrame parent, Order order) {
		super(parent);
		this.order = order;
		this.description = order.getDescription();
	}

	@PostConstruct
	private void init() {

		this.cmbGroups = new JComboBox<>();
		getDescriptionPane().setBackground(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.gameDataPanel.setBorder(BorderFactory.createTitledBorder("Game Data"));
		this.machineDataPanel.setBorder(BorderFactory.createTitledBorder("Bot Data"));
		Box groupField = Box.createHorizontalBox();
		groupField.add(new JLabel("Group(s)"));
		groupField.add(Box.createHorizontalStrut(5));
		groupField.add(this.cmbGroups);
		groupField.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 3));

		if (order != null && order.getOrderType() == OrderType.RESET) {

			this.cmbGroups.addItem(order.getGroupName());
			this.cmbGroups.setEnabled(false);
			this.cmbGroups.setSelectedIndex(0);
			update() ; 
			super.okButton.setText("Save");
			super.okButton.setEnabled(true);
			super.cancelButton.setEnabled(false);
			this.machineDataPanel.setTrainerName(order.getMachineName());
			this.machineDataPanel.setTrainerNameEnabled(false);
		

		} else {

			this.groupNames = this.ctx.getBean(ISokybotContext.class).listNames();
			super.okButton.setText("Create");
			super.okButton.setEnabled((this.groupNames.length > 0));
			this.cmbGroups.addItemListener(this);
			Stream.of(this.groupNames).forEach((groupName) -> {
				this.cmbGroups.addItem(groupName);
			});

		}

		this.page.add(groupField);
		this.page.add(this.gameDataPanel);
		this.page.add(Box.createVerticalStrut(5));
		this.page.add(this.machineDataPanel);

		pack();

	}

	@Override
	protected java.awt.Component getInputComponentsPane() {

		if (this.page == null) {
			this.page = Box.createVerticalBox();

			this.page.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		}
		return page;
	}

	@Override
	protected String getDescription() {

		return this.description;
	}

	@Override
	protected String getDialogTitle() {

		return "Machine Builder";
	}

	private void update() { 
		String selectedGroup = (String) this.cmbGroups.getSelectedItem();
		this.ctx.getBean(ISokybotContext.class).findGroupCtx(selectedGroup).ifPresent((groupCtx) -> {
			ISroDAO targetGame = groupCtx.getGameDAO();
			this.gameDataPanel.setVersion(targetGame.getVersion());
			this.gameDataPanel.setPort(targetGame.getPort());
			this.gameDataPanel.setLanguage(targetGame.getLanguage().orElse("UNKNOWN"));
			this.gameDataPanel.setCountry(targetGame.getCountry().orElse("UNKNOWN"));

			this.gameDataPanel.setGameLocal(targetGame.getLocal().orElse((byte) 0));
			this.gameDataPanel.setDivHosts(targetGame.getDivHosts());

		});		
	}
	@Override
	public void itemStateChanged(ItemEvent e) {

		if (e.getSource() == this.cmbGroups) {

			if (e.getStateChange() == ItemEvent.SELECTED) {
				
				update() ; 

			}

		}

	}

	@Override
	protected void onOkButtonEvent(ActionEvent event) {

		String selectedGroup = (String) this.cmbGroups.getSelectedItem();
		String trainer = this.machineDataPanel.getTrainerName();
		String targetHost = this.gameDataPanel.getSelectedHost();
		List<String> optList = new ArrayList<>();

		if (!trainer.isBlank()) {
			optList.add(AppConstants.MACHINE_TARGET_GATEWAY + "=" + targetHost);

			if (this.machineDataPanel.isAutoLogin()) {
				optList.add("--" + AppConstants.MACHINE_AUTO_LOGIN);
				optList.add(AppConstants.MACHINE_USER_NAME + "=" + this.machineDataPanel.getUsername());
				optList.add(AppConstants.MACHINE_PASSWORD + "=" + this.machineDataPanel.getPassword());
				optList.add(AppConstants.MACHINE_PASSCODE + "=" + this.machineDataPanel.getPasscode());
				optList.add(AppConstants.MACHINE_TARGET_AGENT + "=" + this.machineDataPanel.getAgentServerName());

			}
			this.ctx.getBean(ISokybotContext.class)
					.findGroupCtx(selectedGroup)

					.ifPresent((groupCtx) -> {
						groupCtx.installMachine(trainer, optList.toArray(String[]::new));
					});
			// this.machineGroupService.createMachine(selectedGroup, trainer ,
			// optList.toArray(String[]::new));

			super.onOkButtonEvent(event);
		}

	}
}
