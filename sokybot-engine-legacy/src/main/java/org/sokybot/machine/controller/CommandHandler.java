package org.sokybot.machine.controller;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.gameevents.events.chat.ChatMessageEvent;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.settings.MonsterType;
import org.sokybot.settings.Settings;
import org.sokybot.settings.TrainingAreaSettings;
import org.sokybot.game.dto.Skill;
import org.sokybot.machinegroup.mapnavigation.RuteFinder;
import org.sokybot.game.navigation.IRuteFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
@Command
public class CommandHandler {

	@Autowired
	private ApplicationContext ctx;

	@Value("${" + AppConstants.MACHINE_NAME + "}")
	private String trainerName;

	@Autowired
	private IChatManager chatManager;

	@Autowired
	private Logger log;

	@Autowired
	private ITrainer trainer; // for debug

	@Autowired
	private Settings config;
	
	@Autowired
	private TrainingAreaSettings areaSettings ;

	@Autowired
	private IGameModel gameModel;
	
	@Autowired
	private ScheduledExecutorService taskExecutor;

	@Autowired
	private BundleContext bundleContext;

	private ServiceRegistration<EventHandler> eventHandlerRegistration;

	@PostConstruct
	public void init() {
		// Register as OSGi EventHandler to listen to ChatMessageEvent
		if (bundleContext != null) {
			try {
				Dictionary<String, Object> properties = new Hashtable<>();
				// Subscribe to all ChatMessageEvent topics (for all machines)
				// Format: "sokybot/game/*/ChatMessageEvent"
				properties.put(EventConstants.EVENT_TOPIC, "sokybot/game/*/ChatMessageEvent");
				
				// Create EventHandler that delegates to this controller
				EventHandler handler = this::handleChatMessageEvent;
				
				eventHandlerRegistration = bundleContext.registerService(
						EventHandler.class,
						handler,
						properties);
				
				log.info("CommandHandler registered as OSGi EventHandler for ChatMessageEvent");
			} catch (Exception e) {
				log.error("Failed to register CommandHandler as EventHandler", e);
			}
		}
	}

	@PreDestroy
	public void cleanup() {
		if (eventHandlerRegistration != null) {
			try {
				eventHandlerRegistration.unregister();
				log.info("CommandHandler EventHandler unregistered");
			} catch (Exception e) {
				log.error("Error unregistering CommandHandler EventHandler", e);
			}
		}
	}

	/**
	 * Handles ChatMessageEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ClientOpcode.CHAT_REQUEST) to event-driven approach.
	 * 
	 * Note: Original code listened to CLIENT packets, but that was likely incorrect.
	 * We now listen to SERVER packets (CHAT_UPDATE) which contain incoming chat messages.
	 */
	private void handleChatMessageEvent(Event osgiEvent) {
		try {
			ChatMessageEvent event = (ChatMessageEvent) osgiEvent.getProperty("event");
			String machineName = (String) osgiEvent.getProperty("machineName");
			
			// Only handle events for this machine
			// machineName is extracted from fullName (format: "groupName.machineName")
			String[] parts = machineName != null ? machineName.split("\\.") : new String[0];
			String actualMachineName = parts.length > 1 ? parts[1] : machineName;
			
			if (!this.trainerName.equals(actualMachineName)) {
				return;
			}

			// Only handle private messages
			// For private messages received from server, senderName is who sent it to us
			// If we receive a private message, it's addressed TO this trainer
			if (event.getChatType() == ChatMessageEvent.ChatType.PRIVATE) {
				log.info("Received private chat message from {}: {}", 
					event.getSenderName(), event.getMessage());
				
				// Execute command from private message
				this.ctx.getBean(CommandLine.class).execute(event.getMessage());
			}
		} catch (Exception e) {
			log.error("Error handling ChatMessageEvent", e);
		}
	}

	@Command(name = "useSkill")
	public void useSkill() { 
		log.info("on Invoce useSkill command");
		List<String> attackSkills =  this.config.getAttakListFor(MonsterType.Normal) ; 
		
		 this.gameModel.getSelected().map((spawn)->{
			
			 if(spawn instanceof IMonster) { 
				return  (IMonster) spawn; 
			}
			return null ; 
				 
		 }).ifPresent((monster)->{
			 this.trainer.findSkill(attackSkills.get(0))
			  .ifPresent((skill)->{

					 this.ctx.getBean(ITrainerManager.class)
					 .useSkill(skill.getRefId(), monster.getUniqueId()) ; 
					 log.info("On use skill " + attackSkills.get(0) + " on monster " + monster.getUniqueId()) ;  
			  });
		 });
		
		
		
	
		
	}

	

	
	
	Future<?> lastWalk  = null ; 
	@Command(name = "walk")
	public void walk() {
 
		if(lastWalk != null && !lastWalk.isDone()) { 
			lastWalk.cancel(true) ; 
			lastWalk = null ; 
			
		}
		lastWalk = this.taskExecutor.submit(()->{
			
			log.info("On Walk");
			int targetX = areaSettings.getActiveArea().getAreaX();
			int targetY = areaSettings.getActiveArea().getAreaY() ; 
			
			
			List<Vector2D> path = 
					this.ctx.getBean(IRuteFinder.class).findPath(new Position(this.trainer.getX(), 0, this.trainer.getY()),
					new Position(targetX, 0, targetY), 1.0f);

			path.add(0, new Vector2D(targetX , targetY)) ;

			for(int i = 0; i < path.size() ; i++) { 
	          Vector2D p  = path.get(i) ; 
	          long time = (Math.round(this.trainer.distance((int)p.x, (int)p.y) / (this.trainer.getRunSpeed() * 0.1)  )* 1000) ; 

	          this.chatManager.logMessage(this.trainerName + " ", "On Walking to ( " + p.x + " , " + p.y + " )");
	  		 this.ctx.getBean(ITrainerManager.class).walk((int)p.x, (int)p.y);
	  		   try {
				Thread.sleep(time) ;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		        
			}
			
			
			
		});
		
	}


	@Command(name = "replace")
	public void replace() {
		this.chatManager.logMessage(this.trainerName + "_bot", "On Replace  ");
	}

	@Command(name = "berserk")
	public void berserk() { 
		this.ctx.getBean(ITrainerManager.class).enterBerserkMode();
	}

}
