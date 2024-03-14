package org.sokybot.app;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.sokybot.IMachineContext;
import org.sokybot.IMachinePageViewer;
import org.sokybot.app.domain.MachineInfo;
import org.sokybot.machine.MachineConfig;
import org.sokybot.network.IPacketPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MachineContextAdapter implements IMachineContext {

	private ConfigurableApplicationContext machineContext;

	private final MachineInfo machineInfo;

	public MachineContextAdapter(MachineInfo info, ConfigurableApplicationContext groupCtx) {
		this(info, new String[0], groupCtx);
	}

	public MachineContextAdapter(MachineInfo info, String args[], ConfigurableApplicationContext groupCtx) {
		System.out.println("Passing Args " + Arrays.toString(args) + " to MachineContextAdapter") ; 
		this.machineInfo = info;
		this.machineContext = new SpringApplicationBuilder(MachineConfig.class)
				.properties(Map.of(AppConstants.MACHINE_NAME, info.getMachineName(), AppConstants.GROUP_NAME,
						info.getGroup().getName(), "spring.config.location", "classpath:machine.properties"))
				.parent(groupCtx)
				.initializers((ctx) -> {
					ctx.getBeanFactory().registerSingleton("machineInfo", info);
				})
				.run(args);
	}

	@Override
	public IMachinePageViewer machinePageViewer() {

		return this.machineContext.getBean(IMachinePageViewer.class);
	}

	@Override
	public IPacketPublisher packetPublisher() {
		return this.machineContext.getBean(IPacketPublisher.class);
	}

	@Override
	public String name() {
		return this.machineInfo.getMachineName() ; 
	}
	
	@Override
	public String fullName() {
	 
		return this.machineInfo.getGroup().getName() + "." + name() ; 
	}

	
	@Override
	public boolean isRunning() {
		return this.machineContext.isRunning() ;
	}
}
