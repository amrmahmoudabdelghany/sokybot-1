package org.sokybot.app;

import java.awt.Dimension;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import org.jdesktop.swingx.JXFrame;
import org.noos.xing.mydoggy.ContentManagerUI;
import org.noos.xing.mydoggy.TabbedContentManagerUI;
import org.noos.xing.mydoggy.ToolWindowManager;
import org.noos.xing.mydoggy.ToolWindowManagerDescriptor;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import org.sokybot.app.gamegroupbuilder.MachineGroupBuilderDialog;
import org.sokybot.app.machinebuilder.MachineBuilderDialog;
import org.sokybot.app.mainframe.WindowPreparedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;


@EnableCaching
@Slf4j
@EnableAsync
@SpringBootApplication
public class AppConfig {

	@Autowired
	private ApplicationContext ctx;

	@EventListener
	void registerCreateGameGroupBtn(WindowPreparedEvent event) {

		JToolBar toolBar = event.getToolBar();

		JButton btn = new JButton("Create Group");

		toolBar.add(btn);

		btn.addActionListener((ev) -> {
			ctx.getBean(MachineGroupBuilderDialog.class).setVisible(true);
		});

		btn = new JButton("Create Bot");
		toolBar.add(btn);

		btn.addActionListener((evt) -> this.ctx.getBean(MachineBuilderDialog.class , this.ctx.getBean("mainFrame" , JFrame.class) ).setVisible(true));

	}

	
	@Bean
	@Primary
	ScheduledExecutorService threadPoolExecutor() { 	
		ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(4) ;
		ex.setMaximumPoolSize(8); 
		ex.setKeepAliveTime(1, TimeUnit.MINUTES);
		return ex ; 
	}
	@Bean
	EventLoopGroup eventLoopGroup() {
		return new NioEventLoopGroup(1 );
	}

	

	
	@Bean
	@Qualifier("sokbotTaskExecutor")
	ConcurrentTaskExecutor concurrentTaskExecutor() { 
		
		return new ConcurrentTaskExecutor(threadPoolExecutor())  ; 
	}

	@Bean
	ConcurrentTaskScheduler concurrentTaskScheduler() { 
		return new ConcurrentTaskScheduler(threadPoolExecutor()) ; 
	}
//	@Bean
//	CacheManager cacheManager() {
//		
//		return new ConcurrentMapCacheManager("sokybot-cache");
//	}
//	
	//@Bean
	//TaskExecutor threadPoolTaskExecutor() {

		//ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		//executor.setCorePoolSize(4);
		//executor.setMaxPoolSize(10);
	
		//executor.setQueueCapacity(-1);
		//executor.setThreadNamePrefix("sokybot-thread-pool");
		//executor.initialize();
		//return executor;
//	}

	@Bean
	@Profile("prod")
	JXFrame mainFrame(@Value("${spring.application.name}") String appName) {
		log.info("creating main frame");
		JXFrame frame = new JXFrame(appName, true);

		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		return frame;
	}

	@Bean("mainFrame")
	@Profile("dev")
	JXFrame mainFrameForDev(@Value("${spring.application.name}") String appName) {
		log.info("creating main frame");
		JXFrame frame = new JXFrame(appName, true);
		frame.setPreferredSize(new Dimension(1400, 700));
		frame.setStartPosition(JXFrame.StartPosition.CenterInScreen);

		frame.setExtendedState(JXFrame.MAXIMIZED_BOTH);

		return frame;
	}

	@Bean
	ToolWindowManager toolWindowManager() {

		ToolWindowManager toolWindowManager = new MyDoggyToolWindowManager();

		ToolWindowManagerDescriptor toolWindowManagerDescriptor = toolWindowManager.getToolWindowManagerDescriptor();
		toolWindowManagerDescriptor.setNumberingEnabled(false);
		toolWindowManagerDescriptor.setPreviewEnabled(false);

		// toolWindowManagerDescriptor.setCornerComponent(Corner.NORD_EAST, new
		// JLabel("Hello World!!!"));
		ContentManagerUI<?> contentManagerUI = toolWindowManager.getContentManager().getContentManagerUI();

		contentManagerUI.setCloseable(false);
		contentManagerUI.setDetachable(false);
		contentManagerUI.setMinimizable(false);
		contentManagerUI.setMaximizable(false);

		if (contentManagerUI instanceof TabbedContentManagerUI) {
			TabbedContentManagerUI<?> tabbedContentManagerUI = (TabbedContentManagerUI<?>) contentManagerUI;
			tabbedContentManagerUI.setShowAlwaysTab(true);
		}
		// TODO externalize this config
		// UIManager.put(MyDoggyKeySpace.TWRA_MOUSE_IN_BORDER,
		// UIManager.getColor("ToolBar.background"));
		// UIManager.put(MyDoggyKeySpace.TWRA_MOUSE_OUT_BORDER,
		// UIManager.getColor("ToolBar.background"));
		// UIManager.put(MyDoggyKeySpace.TWRA_BACKGROUND_INACTIVE,
		// UIManager.getColor("ToolBar.background"));// Button.background
		// UIManager.put(MyDoggyKeySpace.TWRA_BACKGROUND_ACTIVE_START,
		// UIManager.getColor("ToolBar.highlight"));
		// UIManager.put(MyDoggyKeySpace.TWRA_BACKGROUND_ACTIVE_START,
		// UIManager.getColor("ToolBar.highlight"));
		// UIManager.put(MyDoggyKeySpace.TWRA_BACKGROUND_ACTIVE_START, Color.BLACK);

		// UIManager.put(MyDoggyKeySpace.TWRA_BACKGROUND_ACTIVE_END,
		// UIManager.getColor("ToolBar.highlight"));
		// UIManager.put(MyDoggyKeySpace.TWRA_FOREGROUND,
		// UIManager.getColor("Button.default.foreground"));

		return toolWindowManager;

	}

	//@Bean
	//Nitrite db() {

		//return Nitrite.builder()
			//	.loadModule(mvstoreModule())
			//	.loadModule(new JacksonMapperModule())
		//		.openOrCreate("soky", "soky");

	//}

	//private NitriteModule mvstoreModule() {
		//return MVStoreModule.withConfig()
				//.filePath(System.getProperty("user.dir") + "\\sokybot.data")
			//	.autoCommit(true)
		//		.build();

	//}

	//@Bean
	//ICacheStorage appCacheStorage() {
	//	return new NitriteCache(db().getCollection(AppConstants.APP_CACHE_STOREAGE));
	//}

}
