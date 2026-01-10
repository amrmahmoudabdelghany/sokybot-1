package org.sokybot.machine.controller.settingdepositor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.Transition;
import org.sokybot.machine.event.userevent.UserConfigUpdatedEvent;
import org.sokybot.machine.model.UserAction;
import org.sokybot.settings.TrainingArea;
import org.sokybot.persistence.service.TrainingAreaRepository;
import org.springframework.aop.aspectj.AspectJAroundAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;
import org.springframework.aop.aspectj.SimpleAspectInstanceFactory;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;

@Aspect
@WithStateMachine
public class TrainingAreaDepositor {

	@Autowired
	private TrainingAreaRepository areaRepo;

	@Autowired
	private StateMachine<MachineState, IMachineEvent> machine;

	@Autowired
	private ApplicationContext ctx ;
	
	private ProxyFactoryBean beanFactory = new ProxyFactoryBean();

	public TrainingAreaDepositor() throws AopConfigException, NoSuchMethodException, SecurityException {

	}

	@PostConstruct
	private void init() {

		try {
			beanFactory = new ProxyFactoryBean();
			beanFactory.setProxyTargetClass(true);
			beanFactory.setSingleton(false);
			beanFactory.setTargetClass(TrainingArea.class);
			beanFactory.addAdvisor(advisor());
			// beanFactory.addAdvice(createTrainingAreaAdvice());
		} catch (AopConfigException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@After("execution(* org.sokybot.settings.TrainingAreaSettings.setActiveArea(String))")
	public void onSetActiveArea() { 
		
		 this.ctx.publishEvent(new UserConfigUpdatedEvent(TrainingAreaDepositor.this, "SETACTIVEAREA"));
	}
	@Around("execution(* org.sokybot.settings.TrainingAreaSettings.removeTrainingArea(String))")
	public Object onRemoveArea(ProceedingJoinPoint joinPoint) throws Throwable {
		TrainingArea removedArea = (TrainingArea) joinPoint.proceed();
		this.areaRepo.delete(removedArea);
		return removedArea;
	}

	@Around("execution(* org.sokybot.settings.TrainingAreaSettings.getArea(String))")
	public Object onGetArea(ProceedingJoinPoint joinPoint) throws Throwable {

		String areaName = (String) joinPoint.getArgs()[0];
		areaName = "area-" + areaName;
		TrainingArea area = (TrainingArea) joinPoint.proceed();

		if (area.getId() == null) {
			area = this.areaRepo.saveAndFlush(area);
			System.out.println("Saving Area :[id] " + area.getId() + " [name] " + area.getName());
		}

		beanFactory.setTarget(area);
		return beanFactory.getObject();// Returning proxied TrainingArea
	}

	private List<MethodInvocation> invocations = new ArrayList<>();

	@Transition(source = MachineState.CONFIG_UNCOMMITTED, target = MachineState.CONFIG_COMMITTED)
	public void commit() {
		Set<TrainingArea> changedAreas = new HashSet<>();

		this.invocations.forEach((invocation) -> {
			try {
				invocation.proceed();

				TrainingArea area = (TrainingArea) invocation.getThis();
				changedAreas.add(area);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
		changedAreas.forEach((area) -> areaRepo.save(area));
		this.invocations.clear();

	}

	private AspectJExpressionPointcutAdvisor advisor() {
		AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
		advisor.setExpression("execution(* org.sokybot.settings.TrainingArea.set*(..))");
		advisor.setAdvice(new TrainingAreaInterceptor());

		return advisor;
	}

	private class TrainingAreaInterceptor implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			System.out.println("Method Return Type is " + invocation.getMethod().getReturnType()) ; 
			if (invocation.getMethod().getReturnType().equals(Void.TYPE)) {
			
				invocations.add(invocation);
				machine.sendEvent(UserAction.CONFIG_MODIFIED);
				
				return null;
			}
			throw new IllegalStateException("Cannot deffer none void methods") ; 
			//return invocation.proceed();
		}

	}

}
