package org.sokybot.machine.controller;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.gamemodel.IGameModel;
import org.sokybot.machine.gamemodel.ISpawnListener;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machine.network.PacketListener;
import org.sokybot.machine.service.IChatManager;
import org.sokybot.machine.service.ITrainerManager;
import org.sokybot.machinegroup.gamemodel.ISpawnable;
import org.sokybot.machinegroup.gamemodel.chat.ChatType;
import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.persistence.entities.MonsterType;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.gamemodel.setting.TrainingAreaSettings;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.machinegroup.mapnavigation.RuteFinder;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
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
	private Trainer trainer; // for debug

	@Autowired
	private Settings config;
	
	@Autowired
	private TrainingAreaSettings areaSettings ;

	@Autowired
	private IGameModel gameModel;
	
	@Autowired
	private ScheduledExecutorService taskExecutor;


	@PacketListener(opcode = ClientOpcode.CHAT_REQUEST)
	public void onClientChat(ImmutablePacket packet) {

		IStreamReader reader = packet.getStreamReader();

		ChatType chatType = ChatType.of(reader.getByte());
		byte chatIndex = reader.getByte();
		log.info("Chat index {} , Chat Type {} ", chatIndex, chatType.name());

		if (chatType == ChatType.PM) {
			String reciver = reader.getString();
			if (reciver.equals(this.trainerName)) {
				 log.info("Chat Message Reciver {} " , reciver);
				String message = reader.getString();

				this.ctx.getBean(CommandLine.class).execute(message);
			}

		}

	}

	@Command(name = "useSkill")
	public void useSkill() { 
		log.info("on Invoce useSkill command");
		List<String> attackSkills =  this.config.getAttakListFor(MonsterType.Normal) ; 
		
		 this.gameModel.getSelected().map((spawn)->{
			
			 if(spawn instanceof Monster) { 
				return  (Monster) spawn; 
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
					this.ctx.getBean(ISroMaterialDAO.class).findPath(new Position(this.trainer.getX(), 0, this.trainer.getY()),
					new Position(targetX, 0, targetY));

			path.add(0, new Vector2D(targetX , targetY)) ;

			for(int i = 0; i < path.size() ; i++) { 
	          Vector2D p  = path.get(i) ; 
	          long time = (Math.round(this.trainer.distance(p.x, p.y) / (this.trainer.getRunSpeed() * 0.1)  )* 1000) ; 

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
