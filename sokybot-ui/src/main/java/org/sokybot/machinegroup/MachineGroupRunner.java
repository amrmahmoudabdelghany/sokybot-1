package org.sokybot.machinegroup;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.sokybot.service.IMainFrameConfigurator;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Configuration
public class MachineGroupRunner {

	

	private JComponent createMultiSplitMainContainer(ApplicationContext ctx) {

		JXMultiSplitPane jxMultiSplitPane = new JXMultiSplitPane();

		String layoutDef = "(ROW (LEAF name =left weight=0.2)(LEAF name=center weight=0.6)(LEAF name =right weight=0.2)";
		jxMultiSplitPane.getMultiSplitLayout().setModel(MultiSplitLayout.parseModel(layoutDef));

		jxMultiSplitPane.add(ctx.getBean(INavTree.class), "left");
		jxMultiSplitPane.add(ctx.getBean(PageContainer.class), "center");
		jxMultiSplitPane.add(ctx.getBean(DashboardContainer.class), "right");

		return jxMultiSplitPane;
	}

	private JComponent createDefaultMainContainer(ApplicationContext ctx) {

		JPanel mainCont = new JPanel(new BorderLayout());

		DashboardContainer dashboardContainer = ctx.getBean(DashboardContainer.class) ; 	
	    dashboardContainer.setPreferredSize(new Dimension(180, 0));
		
			
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ctx.getBean(INavTree.class),
				ctx.getBean(PageContainer.class));
		split.setDividerLocation(250);
		split.setDividerSize(2);
		mainCont.add(split, BorderLayout.CENTER);
		mainCont.add(dashboardContainer , BorderLayout.EAST) ; 
		
		
		return mainCont ; 

	}

	
	
	
	
	@Bean
	@Order(1)
	@Profile({"test" , "dev" ,"prod"})
	ApplicationRunner installGUI(ApplicationContext ctx,
			// @Value("${groupName}") String groupName ,
			Icon gameIcon) {
		return (args) -> {
				String groupName = ctx.getEnvironment().getProperty("groupName") ; 
				
			ctx.getBean(IMainFrameConfigurator.class).addPage(groupName, gameIcon, 
					groupName, createDefaultMainContainer(ctx));
			

		};

	}
	
	


}
