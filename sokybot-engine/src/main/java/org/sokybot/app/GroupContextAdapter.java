/*
package org.sokybot.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.sokybot.IGroupContext;
import org.sokybot.IMachineContext;
import org.sokybot.IMachineListener;
import org.sokybot.ui.api.IPageViewer;
import org.sokybot.app.domain.GroupInfo;
import org.sokybot.app.domain.MachineInfo;
import org.sokybot.exception.NameUniquenessConstraintViolationException;
import org.sokybot.machinegroup.MachineGroupConfig;
import org.sokybot.persistence.service.GameInfoRepository;
import org.sokybot.persistence.service.MachineInfoRepository;
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

	private MachineInfoRepository machineInfoRepo;
	private ConfigurableApplicationContext groupCtx;
	private Lock lock = new ReentrantLock();
	private Map<String, IMachineContext> machines = new HashMap<>();
	private final List<IMachineListener> listeners = new ArrayList<>();
	private final GroupInfo groupInfo;

	public GroupContextAdapter(GroupInfo groupInfo, ConfigurableApplicationContext appCtx) {
		this(groupInfo, new String[0], appCtx);
	}

	public GroupContextAdapter(GroupInfo groupInfo, String[] args, ConfigurableApplicationContext appCtx) {
		this.groupInfo = groupInfo;
		this.machineInfoRepo = appCtx.getBean(MachineInfoRepository.class);

		this.groupCtx = new SpringApplicationBuilder(MachineGroupConfig.class)
				.parent(appCtx)
				.properties("groupName:" + this.groupInfo.getName(), 
						   "gamePath:" + this.groupInfo.getGamePath(),
						   "spring.config.location:classpath:machine-group-ctx.properties")
				.initializers((ctx) -> {
					ctx.setId(groupInfo.getName());
					ctx.getBeanFactory().registerSingleton("groupInfo", groupInfo);
				})
				.run(args);
	}

	@PostConstruct
	private void loadMachines() {
		log.info("GroupInfo: {}", this.groupInfo);
		this.machineInfoRepo.findByGroupId(this.groupInfo.getId()).forEach((machine) -> {
			log.info("Detected Machine [x] {}", machine);

			String machineName = machine.getMachineName();
			check(machineName);
			
			// Settings are now managed per-machine, not globally
			List<String> opts = new ArrayList<>();

			IMachineContext ctx = this.groupCtx.getBean(IMachineContext.class, machine,
					opts.toArray((n) -> new String[n]), this.groupCtx);

			machines.put(machineName, ctx);
			notifyMachineListeners(ctx);
		});

		log.info("Machines have been loaded");
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
			check(name);
			MachineInfo info = new MachineInfo(this.groupInfo, name);
			log.info("Machine info to store: {}", info);
			
			IMachineContext machineCxt = this.groupCtx.getBean(IMachineContext.class, info, options, this.groupCtx);

			this.machines.put(name, machineCxt);
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
		Objects.requireNonNull(name, "Machine name required");

		if (name.isBlank()) {
			throw new IllegalArgumentException("Invalid machine name");
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
*/
