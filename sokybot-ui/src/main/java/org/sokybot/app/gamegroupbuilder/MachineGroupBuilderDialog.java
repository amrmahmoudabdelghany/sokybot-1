package org.sokybot.app.gamegroupbuilder;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.sing_group.gc4s.dialog.AbstractInputJDialog;
import org.sing_group.gc4s.input.filechooser.JFileChooserPanel;
import org.sing_group.gc4s.input.filechooser.JFileChooserPanelBuilder;
import org.sing_group.gc4s.input.filechooser.SelectionMode;
import org.sing_group.gc4s.input.text.BindJXTextField;
import org.sokybot.ISokybotContext;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;



@Component
@Scope("prototype")
public class MachineGroupBuilderDialog extends AbstractInputJDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Dimension lblDim;
	private JFileChooserPanel fileChooserPanel;
	private JLabel lblGroupName;
	private JTextField txtGroupName;
	private JLabel lblError;

	private Box page;

	//private IMachineGroupService botMachineGroupService  ; 
	private ISokybotContext ctx ; 
	private SilkroadFileBrowser silkroadFileBrowser ; 
	
	public MachineGroupBuilderDialog(  JFrame mainFrame ,SilkroadFileBrowser silkroadFileBrowser , ISokybotContext ctx) {//,  IMachineGroupService botMachineGroup ) {
			super(mainFrame); 
			//this.botMachineGroupService = botMachineGroup ; 
			this.silkroadFileBrowser  = silkroadFileBrowser ; 
			this.ctx = ctx; 
	}

	@PostConstruct
	private void init() {

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		getDescriptionPane().setBackground(null);
		
		this.lblDim = new Dimension(80, 0);
		this.lblGroupName = new JLabel("Group Name ");
		this.txtGroupName = new BindJXTextField("", "", (str) -> inputUpdated());
		this.lblError = new JLabel();

		
		// we need to configure fileChooserPanel
		this.fileChooserPanel = JFileChooserPanelBuilder.createOpenJFileChooserPanel()
				.withFileChooser(this.silkroadFileBrowser)
				.withFileChooserSelectionMode(SelectionMode.DIRECTORIES).withLabel("Game Path ")
				//TODO specify appropriate icon for the for the browse button.
				//.withBrowseIcon(new FlatSearchIcon())
				
				.build();
        
		this.fileChooserPanel.setClearSelectedFileActionEnabled(false);
		this.fileChooserPanel.getComponentLabelFile().setPreferredSize(this.lblDim);
		this.lblGroupName.setPreferredSize(this.lblDim);

		Box row = Box.createHorizontalBox();
		row.add(this.lblGroupName);
		row.add(this.txtGroupName);
		page.add(row);

		page.add(Box.createVerticalStrut(5));
		page.add(this.fileChooserPanel);

		page.add(Box.createVerticalStrut(5));
		page.add(this.lblError);

		this.fileChooserPanel.addFileChooserListener((ev) -> {
			inputUpdated();
		});

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

		
		// check if input is valid then forward request to super else display error
		// message

		if (isValidInputs()) {
			// create new bot group using groupService and then forward request to super
			//System.out.println("creating new bot group ........") ; 
			//this.botMachineGroupService.createMachineGroup(this.txtGroupName.getText(),
				//	this.fileChooserPanel.getSelectedFile().getAbsolutePath());
			this.ctx.installGroup(this.txtGroupName.getText(),
					this.fileChooserPanel.getSelectedFile().getAbsolutePath());
			
			super.onOkButtonEvent(event);
		} else {
			
		}
	}

	private boolean isValidInputs() {

		return true;
	}

	private void inputUpdated() {

		okButton.setEnabled(!this.txtGroupName.getText().isBlank() && this.fileChooserPanel.getSelectedFile() != null);

	}

	@Override
	protected String getDescription() {
		return "Create new bot group for a specific game  ";
	}

	protected String getDialogTitle() {
		return "Sokybot";
	}

//	public String getGameGroupName() {
//		return this.txtGroupName.getName();
//	}
//
//	public String getGamePath() {
//		return this.fileChooserPanel.getSelectedFile().getAbsolutePath();
//	}
//	
//	
//	
//	public void displayErrorMessage(String errorMessge) { 
//		this.lblError.setBackground(ColorUtils.COLOR_INVALID_INPUT);
//		this.lblError.setText(errorMessge);
//	}
//	

	public static void main(String args[]) {
		FlatDarkLaf.setup();

		SwingUtilities.invokeLater(() -> {

			MachineGroupBuilderDialog dialog = new SpringApplicationBuilder()
					.sources(MachineGroupBuilderDialog.class, SilkroadFileBrowser.class).headless(false)
					.web(WebApplicationType.NONE).properties("spring.application.name:sokybot").profiles("dev", "test")
					.logStartupInfo(false).run(args).getBean(MachineGroupBuilderDialog.class);

			dialog.setVisible(true);
			if (dialog.isCanceled()) {
				System.out.println("Request Canceled   ");
			} else {
				System.out.println("Create new bot group");

			}

		});


	}

}
