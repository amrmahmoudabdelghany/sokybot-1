package org.sokybot.app.builders;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.sing_group.gc4s.dialog.AbstractInputJDialog;
import org.sing_group.gc4s.input.filechooser.JFileChooserPanel;
import org.sing_group.gc4s.input.filechooser.JFileChooserPanelBuilder;
import org.sing_group.gc4s.input.filechooser.SelectionMode;
import org.sing_group.gc4s.input.text.BindJXTextField;
import org.sokybot.runtime.ISokybotContext;

public class MachineGroupBuilderDialog extends AbstractInputJDialog {
    private static final long serialVersionUID = 1L;
    private JFileChooserPanel fileChooserPanel;
    private JLabel lblGroupName;
    private JTextField txtGroupName;
    private JLabel lblError;
    private Box page;
    private ISokybotContext ctx;
    private SilkroadFileBrowser silkroadFileBrowser;

    public MachineGroupBuilderDialog(SilkroadFileBrowser silkroadFileBrowser, ISokybotContext ctx) {
        super((javax.swing.JFrame)null);
        this.silkroadFileBrowser = silkroadFileBrowser;
        this.ctx = ctx;
        init();
    }

    private void init() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getDescriptionPane().setBackground(null);
        
        this.lblGroupName = new JLabel("Group Name ");
        this.lblGroupName.setPreferredSize(new Dimension(80, 20));
        this.txtGroupName = new BindJXTextField("", "", (str) -> inputUpdated());
        this.lblError = new JLabel();

        this.fileChooserPanel = JFileChooserPanelBuilder.createOpenJFileChooserPanel()
                .withFileChooser(this.silkroadFileBrowser)
                .withFileChooserSelectionMode(SelectionMode.DIRECTORIES).withLabel("Game Path ")
                .build();
        
        this.fileChooserPanel.setClearSelectedFileActionEnabled(false);
        this.fileChooserPanel.getComponentLabelFile().setPreferredSize(new Dimension(80, 20));

        Box row = Box.createHorizontalBox();
        row.add(this.lblGroupName);
        row.add(this.txtGroupName);
        page.add(row);

        page.add(Box.createVerticalStrut(5));
        page.add(this.fileChooserPanel);
        page.add(Box.createVerticalStrut(5));
        page.add(this.lblError);

        this.fileChooserPanel.addFileChooserListener((ev) -> inputUpdated());
        pack();
    }

    @Override
    protected Box getInputComponentsPane() {
        if (this.page == null) {
            this.page = Box.createVerticalBox();
            this.page.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }
        return page;
    }

    @Override
    protected void onOkButtonEvent(ActionEvent event) {
        if (isValidInputs()) {
            this.ctx.installGroup(this.txtGroupName.getText(),
                    this.fileChooserPanel.getSelectedFile().getAbsolutePath());
            super.onOkButtonEvent(event);
        }
    }

    private boolean isValidInputs() {
        return !this.txtGroupName.getText().isBlank() && this.fileChooserPanel.getSelectedFile() != null;
    }

    private void inputUpdated() {
        okButton.setEnabled(isValidInputs());
    }

    @Override
    protected String getDescription() {
        return "Create new bot group for a specific game";
    }

    @Override
    protected String getDialogTitle() {
        return "Sokybot";
    }
}
