package org.sokybot.app;

import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import lombok.extern.slf4j.Slf4j;

import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXRootPane;
import org.noos.xing.mydoggy.ToolWindowManager;
import org.slf4j.LoggerFactory;
import org.sokybot.ISokybotContext;
import org.sokybot.app.logger.AppenderWrapper;
import org.sokybot.app.mainframe.WindowPreparedEvent;
import org.sokybot.app.service.IGameLoaderService;
import org.sokybot.app.service.IPluginService;
import org.sokybot.common.GuiAppender;
import org.sokybot.service.IMainFrameConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import javax.swing.*;
import java.awt.*;

@Configuration
@Slf4j
public class AppRunner {

	@Autowired
	ApplicationContext ctx;

//
//    ApplicationRunner loadPerspective(ToolWindowManager toolWindowManager) {
//        return args -> {
//
//            toolWindowManager.getPersistenceDelegate() ;
//            if(Files.exists(Paths.get(""))) {
//                toolWindowManager.getPersistenceDelegate().
//            }
//        } ;
//    }
	
	
	
	@Bean
	@Order(0)
	ApplicationRunner checkLoaders(ApplicationContext ctx) {
		return (args) -> {
			 
			System.out.println("Avilable Loaders : " + 
			ctx.getBean(IGameLoaderService.class).listAvailables()) ; 

		};

	}

	

	@Bean
	@Order(1)
	ApplicationRunner deployAppCtx() { 
		return args->{
			
			log.debug("Deploy app context");
			ISokybotContext appCtx = this.ctx.getBean(ISokybotContext.class) ; 
			this.ctx.getBean(IPluginService.class).deployService(ISokybotContext.class, appCtx);
		};
	}
	@Bean
	@Order(2)
	ApplicationRunner redirectAppLogger() {
		return args -> {

			
			log.debug("Confguring root logger");
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			Logger root = loggerContext.getLogger("org.sokybot");

			AppenderWrapper wrapper = (AppenderWrapper) root.getAppender("WRAPPER");

			Appender<ILoggingEvent> appender = ctx.getBean(GuiAppender.class);
			appender.setContext(loggerContext);

			wrapper.origin(appender);
			root.setLevel(Level.INFO);
			root.setAdditive(false);

		};
	}

//	@Bean
//	@Order(2)
//	ApplicationRunner launchOsgiContainer() {
//		return args -> {
//
//			log.debug("deploying service to osgi container");
//
//			Bundle bundle = ctx.getBean(Bundle.class);
//
//			BundleContext bndCtx = bundle.getBundleContext();
//			JMenuBar menuBar = ctx.getBean(JMenuBar.class);
//			bndCtx.registerService(JMenuBar.class.getName(), menuBar, null);
//
//			IMainFrameConfigurator mainFrameConfigurator = ctx.getBean(IMainFrameConfigurator.class);
//			bndCtx.registerService(IMainFrameConfigurator.class.getName(), mainFrameConfigurator, null);
//
//			log.debug("Starting system bundle ");
//			bundle.start();
//		};
//
//	}

	@Profile({"dev" , "test"})
	@Bean
	ApplicationRunner addSomeToolWindows(@Qualifier("feed")Icon logIcon ) {
		return args -> {
			log.info("adding console and log tool windows");
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JScrollPane(new JTextArea()), BorderLayout.CENTER);

			this.ctx.getBean(IMainFrameConfigurator.class)
					.addExtraWindow("Console", "Sokybot console", logIcon, panel);

		};
	}

	


	// very last operation
	@Bean
	ApplicationRunner displayFrame(ApplicationContext ctx) {

		return (args) -> {

			log.debug("assembling frame components");
			JXFrame mainFrame = ctx.getBean(JXFrame.class);

			JXRootPane rootPane = mainFrame.getRootPaneExt();
			JToolBar toolBar = ctx.getBean(JToolBar.class);
			rootPane.setToolBar(toolBar);

			JMenuBar menuBar = getMenuBar(ctx);

			rootPane.setJMenuBar(menuBar);

			ToolWindowManager toolWindowManager = ctx.getBean(ToolWindowManager.class);

			if (toolWindowManager instanceof Component) {
				// rootPane.getCont((Container) toolWindowManager );
				rootPane.getContentPane().add((Component) toolWindowManager);
			} else {
				System.exit(0);
			}

			mainFrame.pack();
			ctx.publishEvent(new WindowPreparedEvent(this, mainFrame, toolBar, menuBar));

			// GraphicsEnvironment.getLocalGraphicsEnvironment()
			// .getScreenDevices()[0].setFullScreenWindow(mainFrame);

		//	FlatDarkLaf.setup();
		//	FlatDarkLaf.updateUI();
			mainFrame.setVisible(true);
			
		};

	}

	private JMenuBar getMenuBar(ApplicationContext ctx) {
		JMenuBar menuBar = ctx.getBean(JMenuBar.class);
		menuBar.setHelpMenu(ctx.getBean("helpMenu", JMenu.class));

		return menuBar;
	}

	@Bean
	@Profile("dev")
	ApplicationRunner installWindowInspector() {
		return args -> {
			FlatInspector.install("alt shift 3");
			FlatUIDefaultsInspector.install("alt shift 4");
		};
	}

}
