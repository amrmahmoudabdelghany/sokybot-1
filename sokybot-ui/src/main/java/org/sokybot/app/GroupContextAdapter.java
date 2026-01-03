package org.sokybot.app;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.sokybot.ICacheStorage;
import org.sokybot.IGroupContext;
import org.sokybot.IMachineContext;
import org.sokybot.IMachineListener;
import org.sokybot.IPageViewer;
import org.sokybot.app.domain.GroupInfo;
import org.sokybot.app.domain.MachineInfo;
import org.sokybot.app.machinebuilder.GameDataPanel;
import org.sokybot.app.machinebuilder.MachineBuilderDialog;
import org.sokybot.app.machinebuilder.MachineDataPanel;
import org.sokybot.app.machinebuilder.Order;
import org.sokybot.app.repo.MachineInfoRepo;
import org.sokybot.exception.NameUniquenessConstraintViolationException;
import org.sokybot.machinegroup.MachineGroupConfig;
import org.sokybot.machinegroup.repo.SettingRepo;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.service.ISroDAO;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Scope("prototype")
@Slf4j
public class GroupContextAdapter implements IGroupContext {

	// private ICacheStorage cacheStorage ;

	private MachineInfoRepo machineInfoRepo;

	private ConfigurableApplicationContext groupCtx;

	private Lock lock = new ReentrantLock();

	private Map<String, IMachineContext> machines = new HashMap<String, IMachineContext>();

	private final List<IMachineListener> listeners = new ArrayList<>();

	private final GroupInfo groupInfo;

	public GroupContextAdapter(GroupInfo groupInfo, ConfigurableApplicationContext appCtx) {
		this(groupInfo, new String[0], appCtx);
	}

	public GroupContextAdapter(GroupInfo groupInfo, String[] args, ConfigurableApplicationContext appCtx) {

		this.groupInfo = groupInfo;
		// this.cacheStorage = appCtx.getBean("appCacheStorage", ICacheStorage.class) ;
		this.machineInfoRepo = appCtx.getBean(MachineInfoRepo.class);

		this.groupCtx = new SpringApplicationBuilder(MachineGroupConfig.class).parent(appCtx)
				.properties("groupName:" + this.groupInfo.getName(), "gamePath:" + this.groupInfo.getGamePath(),
						"spring.config.location:classpath:machine-group-ctx.properties")
				.initializers((ctx) -> {
					ctx.setId(groupInfo.getName());
					ctx.getBeanFactory().registerSingleton("groupInfo", groupInfo);
				})
				.run(args);
	}

	@PostConstruct
	private void loadMachines() {

		log.info("GroupInfo : {} ", this.groupInfo);
		this.machineInfoRepo.findMachineInfoByGroupId(this.groupInfo.getId()).forEach((machine) -> {
			log.info("Detected Machine [x] {} ", machine);

			String machineName = machine.getMachineName();
			check(machineName);
			SettingRepo settingRepo = this.groupCtx.getBean(SettingRepo.class);
			List<String> opts = new ArrayList<>(); 
			if (!settingRepo.existsById(this.groupInfo.getName() + "." + machineName)) {
				opts.add("--"+ AppConstants.MACHINE_PRIMARY_RESET) ; 
			} 

				IMachineContext ctx = this.groupCtx.getBean(IMachineContext.class, machine , opts.toArray((n)->new String[n]), this.groupCtx);

				machines.put(machineName, ctx);

				notifyMachineListeners(ctx);
			

		});

		log.info("Machines has been loaded");
	}


	@Override
	public ISroDAO getGameDAO() {
		return this.groupCtx.getBean(ISroDAO.class);
	}

	@Override
	public IPageViewer pageViewer() {

		return this.groupCtx.getBean(IPageViewer.class);
	}

	@Override
	public IMachineContext[] getMachines() {

		return this.machines.values().toArray((len) -> new IMachineContext[len]);
	}

	@Override
	public Optional<IMachineContext> findMachineCtx(String name) {
		return Optional.ofNullable(this.machines.get(name));
	}

	@Override
	public void installMachine(String name) {
		installMachine(name, new String[0]);
	}

	@Override
	public void installMachine(String name, String... options) {

		try {

			lock.lock();
			check(name);// TODO update this method
			MachineInfo info = new MachineInfo(this.groupInfo, name);
			log.info("Machine info To Store : {} ", info);
			IMachineContext machineCxt = this.groupCtx.getBean(IMachineContext.class, info, options, this.groupCtx);

			// this.cacheStorage.store(info.getGroupName()+"-"+info.getMachineName(), info);
			this.machines.put(name, machineCxt);
			// this.cacheStorage.flush();
			this.machineInfoRepo.save(info);

			notifyMachineListeners(machineCxt);

		} finally {
			lock.unlock();
		}

	}

	private void notifyMachineListeners(IMachineContext machineCtx) {
		for (IMachineListener listener : this.listeners) {
			listener.onMachineInstalled(machineCtx);
		}
	}

	@Override
	public void addMachineListener(IMachineListener machineListener) {

		this.listeners.add(machineListener);
	}

	@Override
	public void removeMachineListener(IMachineListener machineListener) {

		this.listeners.remove(machineListener);
	}

	private void check(String name) {
		Objects.requireNonNull(name, "Machine name required ");

		if (name.isBlank()) {
			throw new IllegalArgumentException("Invalid machine name ");
		}

		if (this.machines.containsKey(name)) {
			throw new NameUniquenessConstraintViolationException("Machine name must be unique", name);
		}
	}

	@Override
	public String name() {

		return this.groupInfo.getName();
	}

	@Override
	public boolean isRunning() {

		return this.groupCtx.isRunning();
	}
}
