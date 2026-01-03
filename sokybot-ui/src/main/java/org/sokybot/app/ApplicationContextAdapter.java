package org.sokybot.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.sokybot.IGroupContext;
import org.sokybot.IGroupListener;
import org.sokybot.ISokybotContext;
import org.sokybot.app.domain.GroupInfo;
import org.sokybot.app.repo.GroupInfoRepo;
import org.sokybot.exception.InvalidGameReferenceException;
import org.sokybot.exception.NameUniquenessConstraintViolationException;
import org.sokybot.service.IMainFrameConfigurator;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationContextAdapter implements ISokybotContext {

	
	@Autowired
	private ConfigurableApplicationContext ctx ; 
	
//	@Autowired
	//@Qualifier("appCacheStorage")
	//private ICacheStorage cacheStorage ; 
	
	@Autowired
	private GroupInfoRepo groupInfoRepo ; 
	
	private final List<IGroupListener> listeners = new ArrayList<>() ; 
	

	private Lock lock = new ReentrantLock();

	private Map<String, IGroupContext> groups = new ConcurrentHashMap<String, IGroupContext>();

	

	@PostConstruct
	private void reload() {
		log.info("Reloading groups...");
	
		this.groupInfoRepo.findAll()
		.forEach((groupInfo)->{
			check(groupInfo.getName() , groupInfo.getGamePath()) ; 
			IGroupContext groupCtx  = this.ctx.getBean(IGroupContext.class  , groupInfo , this.ctx) ; 
			this.groups.put(groupInfo.getName(),  groupCtx) ; 
			notifyGroupInstalled(groupCtx) ; 
			log.info("Group {} has been loaded ", groupInfo.getName());
			
		});
		
	}
	
	@Override
	public IMainFrameConfigurator getFrameConfigurator() {
		return this.ctx.getBean(IMainFrameConfigurator.class) ;  
	}

	@Override
	public IGroupContext[] getGroups() {
		
		return this.groups.values().toArray((len)->new IGroupContext[len]);
	}

	 
	@Override
	public String[] listNames() {
	  return this.groups.keySet().toArray((len)->new String[len]) ; 
	}

	@Override
	public Optional<IGroupContext> findGroupCtx(String name) {
	 return Optional.ofNullable(this.groups.get(name)) ;
	}
	@Override
	public void installGroup(String groupName, String gamePath) {
		installGroup(groupName, gamePath , new String[0]);
	}
	
	@Override
	public void addGroupListener(IGroupListener listener) {
	 
		this.listeners.add(listener) ; 
		
	}
	
	@Override
	public boolean isRunning() {
		return this.ctx.isRunning() ; 
	}
	@Override
	public String name() {
		return "SokyBot"; 
	}
	
	@Override
	public void removeGroupListener(IGroupListener listener) {
	 
		this.listeners.remove(listener); 
	}
	
	@Override
	public void installGroup(String name, String gamePath, String... options) {
		
	
		log.info("Installing new machine group with name {} at {}",name, gamePath);
		try {

			lock.lock();
			check(name, gamePath);
			GroupInfo info = new GroupInfo(name, gamePath) ; 
			IGroupContext groupContext = this.ctx.getBean(IGroupContext.class ,info , options , this.ctx) ; 
			
			this.groupInfoRepo.save(info) ; 
			//this.cacheStorage.store(info.getName(), info);
			
			groups.put(name, groupContext);
			//this.cacheStorage.flush();
			
			notifyGroupInstalled(groupContext);

		} finally {
			lock.unlock();
		}		
				
		
	}
	
	

	

	
	private void notifyGroupInstalled(IGroupContext ctx) { 
		 
		for(IGroupListener listener : this.listeners) { 
			listener.onGroupInstalled(ctx);
		}
		
	}


	
	private void check(String name, String gamePath) {
		if (name.isBlank())
			throw new IllegalArgumentException("Group name could not be blank");

		if (gamePath.isBlank())
			throw new IllegalArgumentException("Game path could not be blank");

		if (this.groups.containsKey(name))
			throw new NameUniquenessConstraintViolationException(
					"Each machine group is identifed by its name , so the name must be unique ", name);

		if (!SilkroadUtils.isValidSilkroadDirectory(gamePath))
			throw new InvalidGameReferenceException("Invalid game directory " + gamePath, gamePath);

	}

	
	
}
