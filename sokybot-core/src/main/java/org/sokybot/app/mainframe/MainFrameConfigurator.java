package org.sokybot.app.mainframe;

import org.noos.xing.mydoggy.*;
import org.sokybot.service.IMainFrameConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.stream.Stream;

@Service
public class MainFrameConfigurator implements IMainFrameConfigurator {

	@Autowired
	private ToolWindowManager toolWindowManager;

	@Autowired
	private JMenuBar menuBar;

	
	
	
	
	@Override
	public void addExtraWindow(String id, String title, Icon icon, Component comp) {

		ToolWindow toolWindow = toolWindowManager.registerToolWindow(id , title, icon, comp, ToolWindowAnchor.BOTTOM);
		
	//	toolWindow.setAggregateMode(true);
	
		toolWindow.setAvailable(true);
		// toolWindow.setVisible(true);
		// RepresentativeAnchorDescriptor
		RepresentativeAnchorDescriptor representativeAnchorDescriptor = toolWindow.getRepresentativeAnchorDescriptor();
		representativeAnchorDescriptor.setPreviewEnabled(true);
		representativeAnchorDescriptor.setPreviewDelay(1500);
		representativeAnchorDescriptor.setPreviewTransparentRatio(0.4f);
		
		// DockedTypeDescriptor
		DockedTypeDescriptor dockedTypeDescriptor = (DockedTypeDescriptor) toolWindow
				.getTypeDescriptor(ToolWindowType.DOCKED);
		dockedTypeDescriptor.setAnimating(true);
		dockedTypeDescriptor.setHideRepresentativeButtonOnVisible(false);
		dockedTypeDescriptor.setDockLength(300);
		dockedTypeDescriptor.setPopupMenuEnabled(true);
		dockedTypeDescriptor.setIdVisibleOnTitleBar(false);
		
		
		JMenu toolsMenu = dockedTypeDescriptor.getToolsMenu();
		toolsMenu.add(new AbstractAction("Hello World!!!") {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "Hello World!!!");
			}
		});
		dockedTypeDescriptor.setToolWindowActionHandler(new ToolWindowActionHandler() {
			public void onHideButtonClick(ToolWindow toolWindow) {
			//	JOptionPane.showMessageDialog(null, "Hiding...");
				toolWindow.setType(ToolWindowType.SLIDING);
				//toolWindow.setVisible(false);
			}
		});

		dockedTypeDescriptor.setAnimating(true);

		// SlidingTypeDescriptor
		SlidingTypeDescriptor slidingTypeDescriptor = (SlidingTypeDescriptor) toolWindow
				.getTypeDescriptor(ToolWindowType.SLIDING);
		slidingTypeDescriptor.setEnabled(true);
		slidingTypeDescriptor.setTransparentMode(true);
		slidingTypeDescriptor.setTransparentRatio(0.8f);
		slidingTypeDescriptor.setTransparentDelay(0);
		slidingTypeDescriptor.setAnimating(true);
		slidingTypeDescriptor.setIdVisibleOnTitleBar(false);
		
		
		// FloatingTypeDescriptor
		FloatingTypeDescriptor floatingTypeDescriptor = (FloatingTypeDescriptor) toolWindow
				.getTypeDescriptor(ToolWindowType.FLOATING);
		floatingTypeDescriptor.setEnabled(true);
		floatingTypeDescriptor.setLocation(150, 200);
		floatingTypeDescriptor.setSize(320, 200);
		floatingTypeDescriptor.setModal(false);
		floatingTypeDescriptor.setTransparentMode(true);
		floatingTypeDescriptor.setTransparentRatio(0.2f);
		floatingTypeDescriptor.setTransparentDelay(1000);
		floatingTypeDescriptor.setAnimating(true);
		floatingTypeDescriptor.setIdVisibleOnTitleBar(false);

	}

	@Override
	public void addPage(String pageId, Icon icon, String title, Component component) {

		ContentManager contentManager = this.toolWindowManager.getContentManager();

		boolean exists = Stream.of(contentManager.getContents()).anyMatch((cont) -> cont.getId().equals(pageId));

		if (exists) {

			return;
		}

		//TabbedContentManagerUI<TabbedContentUI> contentManagerUI = (TabbedContentManagerUI<TabbedContentUI>) contentManager
			//	.getContentManagerUI();

		Content content = contentManager.addContent(pageId, title, icon, component);
		
		ContentUI contentUI = contentManager.getContentManagerUI().getContentUI(content);

//		contentUI.setCloseable(false);
//		contentUI.setDetachable(false);
//		contentUI.setMaximizable(false);
//		contentUI.setMinimizable(false);
		contentUI.setTransparentMode(false);
		contentUI.setTransparentRatio(0.7f);
		contentUI.setTransparentDelay(1000);
		
		

	}

}
