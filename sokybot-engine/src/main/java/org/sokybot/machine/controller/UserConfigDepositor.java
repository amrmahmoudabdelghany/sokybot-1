package org.sokybot.machine.controller;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import org.slf4j.Logger;
import org.sokybot.ICacheStorage;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.Transition;
import org.sokybot.machine.event.userevent.UserConfigUpdatedEvent;
import org.sokybot.machine.model.UserAction;
import org.sokybot.settings.Settings;
import org.sokybot.persistence.service.SettingsRepository;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;

@Aspect
@WithStateMachine
public class UserConfigDepositor {

	
	@Autowired
	private ApplicationContext ctx ; 
	
	@Autowired
	private Logger log;

	@Value("${" + AppConstants.GROUP_NAME + "}")
	private String groupName;

	@Value("${" + AppConstants.MACHINE_NAME + "}")
	private String machineName;

	private List<ProceedingJoinPoint> invocations = new ArrayList<>();

	@Autowired
	private StateMachine<MachineState, IMachineEvent> machine;

	@Autowired
	private SettingsRepository settingsRepo;

	@Autowired
	private Settings userConfig;

	@Pointcut("execution(* org.sokybot.settings.Settings.set*(..))")
	public void setterMethods() {
	}

	@Pointcut("execution(* org.sokybot.settings.Settings.remove*(..))")
	public void removeMethods() {
	}

	@Pointcut("execution(* org.sokybot.settings.Settings.add*(..))")
	public void addMethods() {
	}

	@Pointcut("execution(* org.sokybot.settings.Settings.swap*(..))")
	public void swapMethods() {
	}

	@Around(" setterMethods()||addMethods()||removeMethods()||swapMethods() ")
	public Object defer(ProceedingJoinPoint joinPoint) {

		if (joinPoint.getSignature().getName().startsWith("get")) {
			try {
			return	joinPoint.proceed(); //TODO what about return ?? 
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		} else {
			// TODO try to get current value and see if it differ from the passed argument
			// the defer the invocation else proceed it
			System.out.println("Execution of method UserConfig." + joinPoint.getSignature() + " is defered");
			this.invocations.add(joinPoint);
			this.machine.sendEvent(UserAction.CONFIG_MODIFIED);
			return null ; 
		}
	}

	@Transition(source = MachineState.CONFIG_UNCOMMITTED, target = MachineState.CONFIG_COMMITTED)
	public void commit() {

		this.invocations.forEach((joinPoint) -> {
			try {
				joinPoint.proceed();

			 String methodName = 	
					 joinPoint.getSignature()
					 .getName();
			 
			  
			 this.ctx.publishEvent(new UserConfigUpdatedEvent(UserConfigDepositor.this, methodName.toUpperCase())); 
			
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
		this.invocations.clear();

		try {
			this.settingsRepo.save(getTargetObject(this.userConfig, Settings.class)) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//try {
		//	this.goupCacheStorage.store(groupName + "." + machineName,
		//			getTargetObject(this.userConfig, Settings.class));
		//} catch (Exception e) {
		//	e.printStackTrace();
	//	}
		// this.cacheStorage.flush();
		// Document machineDoc =
		// this.machineRegister.find(FluentFilter.where(Constants.GROUP_NAME)
		// .eq(this.groupName)
		// .and(FluentFilter.where(Constants.MACHINE_NAME).eq(this.machineName))).firstOrNull();

		// if (machineDoc == null)
		// throw new IllegalStateException("Machine configuration is missing");

		// this.machineRegister.update(machineDoc);

		log.info("User configuration updated");
	}

	

	private <T> T getTargetObject(Object proxy, Class<T> targetClass) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else if (AopUtils.isCglibProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else {
			return (T) proxy;
		}
	}

}
