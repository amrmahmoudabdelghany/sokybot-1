package org.sokybot.app.builders;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import org.sing_group.gc4s.dialog.AbstractInputJDialog;
import org.sokybot.ISokybotContext;
import org.sokybot.app.AppConstants;
import org.sokybot.app.builders.Order.OrderType;
import org.sokybot.service.ISroDAO;

public class MachineBuilderDialog extends AbstractInputJDialog implements ItemListener {
    private static final long serialVersionUID = 1L;
    private GameDataPanel gameDataPanel;
    private MachineDataPanel machineDataPanel;
    private ISokybotContext ctx;
    private String[] groupNames;
    private JComboBox<String> cmbGroups;
    private Box page;
    private String description;
    private Order order = null;

    public MachineBuilderDialog(ISokybotContext ctx) {
        super((javax.swing.JFrame)null);
        this.ctx = ctx;
        this.description = "Create new bot";
        init();
    }

    public MachineBuilderDialog(ISokybotContext ctx, Order order) {
        super((javax.swing.JFrame)null);
        this.ctx = ctx;
        this.order = order;
        this.description = order.getDescription();
        init();
    }

    private void init() {
        this.gameDataPanel = new GameDataPanel();
        this.machineDataPanel = new MachineDataPanel();
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
            update();
            super.okButton.setText("Save");
            super.okButton.setEnabled(true);
            super.cancelButton.setEnabled(false);
            this.machineDataPanel.setTrainerName(order.getMachineName());
            this.machineDataPanel.setTrainerNameEnabled(false);
        } else {
            this.groupNames = this.ctx.listNames();
            super.okButton.setText("Create");
            super.okButton.setEnabled((this.groupNames.length > 0));
            this.cmbGroups.addItemListener(this);
            Stream.of(this.groupNames).forEach(this.cmbGroups::addItem);
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
    protected String getDescription() { return this.description; }

    @Override
    protected String getDialogTitle() { return "Machine Builder"; }

    private void update() {
        String selectedGroup = (String) this.cmbGroups.getSelectedItem();
        this.ctx.findGroupCtx(selectedGroup).ifPresent((groupCtx) -> {
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
        if (e.getSource() == this.cmbGroups && e.getStateChange() == ItemEvent.SELECTED) {
            update();
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
            this.ctx.findGroupCtx(selectedGroup).ifPresent((groupCtx) -> {
                groupCtx.installMachine(trainer, optList.toArray(String[]::new));
            });
            super.onOkButtonEvent(event);
        }
    }
}
