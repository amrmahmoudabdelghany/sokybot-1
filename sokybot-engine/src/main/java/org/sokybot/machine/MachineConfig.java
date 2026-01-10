package org.sokybot.machine;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.sokybot.ICacheStorage;
import org.sokybot.app.AppConstants;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.actuator.ActiveAreaNavActuator;
import org.sokybot.machine.actuator.AttackingActuator;
import org.sokybot.machine.actuator.PickingActuator;
import org.sokybot.machine.actuator.MovingActuator;
import org.sokybot.machine.actuator.MonsterTargetingActuator;
import org.sokybot.machine.controller.CommandHandler;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.machine.model.ServerFeed;
import org.sokybot.machine.model.UserAction;
import org.sokybot.settings.BotType;
import org.sokybot.settings.Settings;
import org.sokybot.settings.TrainingAreaSettings;
import org.sokybot.settings.TrainingAreaSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigBuilder;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.spring.PicocliSpringFactory;

@Configuration
@ComponentScan({ "org.sokybot.machine" })
@EnableStateMachine
@EnableAspectJAutoProxy
@EnableAsync
@EnableTransactionManagement
public class MachineConfig extends StateMachineConfigurerAdapter<MachineState, IMachineEvent> {

	@Autowired
	ApplicationContext ctx;
	
	

	@Autowired
	@Qualifier("sokbotTaskExecutor")
	private TaskExecutor threadPoolTaskExecutor;

	@Autowired
	private ConcurrentTaskScheduler taskScheduler;

	@Autowired
	Settings settings ; 
	
	@Autowired
	Logger log;

	@Bean(name = "applicationEventMulticaster")
	ApplicationEventMulticaster applicationEventMulticaster() {
		SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
		eventMulticaster.setTaskExecutor(this.threadPoolTaskExecutor);
		eventMulticaster.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
		return eventMulticaster;
	}

	@Override
	public void configure(StateMachineStateConfigurer<MachineState, IMachineEvent> states) throws Exception {

		states.withStates()
				// .initial(MachineState.RUNNING)
				// .and()
				// .withStates()
				.initial(MachineState.CONFIG_COMMITTED)
				.state(MachineState.CONFIG_UNCOMMITTED)
				.and()
				.withStates()
				.initial(MachineState.WITHOUT_CLIENT)
				.state(MachineState.WITH_CLIENT)
				.and()
				.withStates()
				// .parent(MachineState.RUNNING)
				.initial(MachineState.READY)
				.state(MachineState.DISCONNECTING)
				.state(MachineState.INTERACTING)
				.and()
				.withStates()
				.parent(MachineState.INTERACTING)
				.initial(MachineState.STARTING)
				.choice(MachineState.CLIENTLESS)
				.state(MachineState.LAUNCHING)
				.state(MachineState.CONNECTING)
				.state(MachineState.REDIRECTING)
				.state(MachineState.HANDSHAKING)
				.state(MachineState.CHALENGING)
				.state(MachineState.REFUSING)
				.state(MachineState.IDENTIFYING)
				.state(MachineState.VERIFYING)
				.state(MachineState.UPDATING)
				.state(MachineState.DISCOVERING)
				.state(MachineState.LOGINING)
				.state(MachineState.AUTHENTICATING)
				.state(MachineState.LISTING)
				.state(MachineState.JOINING)
				
				.exit(MachineState.REFUSING)
				.exit(MachineState.UPDATING)
				// .and()
				// .withStates()
				// .parent(MachineState.PLAYING)
				// .initial(MachineState.IDLE)
				.and()
				.withStates()
				// .entry(MachineState.IDLE)
				.parent(MachineState.INTERACTING)
				.initial(MachineState.IDLE)
				.state(MachineState.IDLE)
				.state(MachineState.PLAYING)
				//.choice(MachineState.MOVING)
				.choice(MachineState.TRAINING)
				.choice(MachineState.TARGETING)
				.choice(MachineState.ATTACKING)
				.choice(MachineState.PICKING)
				.state(MachineState.WATING);

	}

	@Override
	public void configure(StateMachineTransitionConfigurer<MachineState, IMachineEvent> transitions) throws Exception {

		PickingActuator pickingActuator = this.ctx.getBean(PickingActuator.class);

		//MovingActuator movingActuator = this.ctx.getBean(MovingActuator.class);
		AttackingActuator attackActuator = this.ctx.getBean(AttackingActuator.class);
		MonsterTargetingActuator targetingActuator = this.ctx.getBean(MonsterTargetingActuator.class);
		ActiveAreaNavActuator walkingActuator  = this.ctx.getBean(ActiveAreaNavActuator.class) ; 
		transitions.withLocal()
				.source(MachineState.WITHOUT_CLIENT)
				.target(MachineState.WITH_CLIENT)
				.event(UserAction.CLIENT_ATTACHED)
				.and()
				.withLocal()
				.source(MachineState.WITH_CLIENT)
				.target(MachineState.WITHOUT_CLIENT)
				.event(UserAction.KILL_CLIENT)
				.and()
				.withLocal()
				.source(MachineState.CONFIG_UNCOMMITTED)
				.target(MachineState.CONFIG_COMMITTED)
				.event(UserAction.CONFIG_COMMIT)
				.and()
				.withLocal()
				.source(MachineState.CONFIG_COMMITTED)
				.target(MachineState.CONFIG_UNCOMMITTED)
				.event(UserAction.CONFIG_MODIFIED)
				.and()
				.withLocal()
				.source(MachineState.READY)
				.target(MachineState.INTERACTING)
				.event(UserAction.CONNECT)
				.and()
				.withLocal()
				.source(MachineState.INTERACTING)
				.target(MachineState.DISCONNECTING)
				.event(UserAction.DISCONNECT)
				.and()
				.withLocal()
				.source(MachineState.DISCONNECTING)
				.target(MachineState.READY)
				.and()
				.withLocal()
				.source(MachineState.STARTING)
				.target(MachineState.CLIENTLESS)
				.and()
				.withChoice()
				.source(MachineState.CLIENTLESS)
				.first(MachineState.CONNECTING, (ctx) -> settings.getBotType() == BotType.CLIENTLESS)
				.last(MachineState.LAUNCHING)
				.and()
				.withLocal()
				.source(MachineState.LAUNCHING)
				.target(MachineState.CONNECTING)
				.event(ClientFeed.CLIENT_CONNECTED)
				.and()
				.withLocal()
				.source(MachineState.CONNECTING)
				.target(MachineState.HANDSHAKING)
				.event(ServerFeed.SETUP)
				.and()
				.withLocal()
				.source(MachineState.REDIRECTING)
				.target(MachineState.HANDSHAKING)
				.event(ServerFeed.SETUP)
				.and()
				.withLocal()
				.source(MachineState.HANDSHAKING)
				.target(MachineState.CHALENGING)
				.event(ServerFeed.CHALLENGE)
				.and()
				.withLocal()
				.source(MachineState.CHALENGING)
				.target(MachineState.REFUSING)
				.event(ClientFeed.CONNECTION_REFUSED)
				.and()
				.withExit()
				.source(MachineState.REFUSING)
				.target(MachineState.DISCONNECTING)
				.and()
				.withLocal()
				.source(MachineState.CHALENGING)
				.target(MachineState.IDENTIFYING)
				.event(ClientFeed.CONNECTION_ACCEPTED)
				.and()
				.withLocal()
				.source(MachineState.IDENTIFYING)
				.target(MachineState.VERIFYING)
				.event(ServerFeed.GATEWAY_CONNECTED)
				.and()
				.withLocal()
				.source(MachineState.VERIFYING)
				.target(MachineState.UPDATING)
				.event(ServerFeed.INCOMPATIBLE)
				.and()
				.withExit()
				.source(MachineState.UPDATING)
				.target(MachineState.DISCONNECTING)
				.and()
				.withLocal()
				.source(MachineState.VERIFYING)
				.target(MachineState.DISCOVERING)
				.event(ServerFeed.COMPATIBLE)
				.and()
				.withLocal()
				.source(MachineState.DISCOVERING)
				.target(MachineState.LOGINING)
				.event(ServerFeed.AGENT_lISTED)
				.and()
				.withLocal()
				.source(MachineState.LOGINING)
				.target(MachineState.REDIRECTING)
				.event(ServerFeed.LOGIN_SUCCESS)
				.and()
				.withLocal()
				.source(MachineState.IDENTIFYING)
				.target(MachineState.AUTHENTICATING)
				.event(ServerFeed.AGENT_CONNECTED)
				.and()
				.withLocal()
				.source(MachineState.AUTHENTICATING)
				.target(MachineState.LISTING)
				.event(ServerFeed.AUTHENTICATED)
				.and()
				.withLocal()
				.source(MachineState.LISTING)
				.target(MachineState.JOINING)
				.event(ServerFeed.LISTED)
				.and()

				.withExternal()
				.source(MachineState.JOINING)
				.target(MachineState.IDLE)
				.event(ClientFeed.GAME_READY)
				.and()
				.withLocal()
				.source(MachineState.IDLE)
				.target(MachineState.PLAYING)
				.event(UserAction.START_TRAINING)
				.and()
				.withLocal() // we can discard this transition
				.source(MachineState.PLAYING)
				.target(MachineState.IDLE)
				.event(UserAction.STOP_TRAINING)
				.and()
				.withLocal()
				.source(MachineState.PLAYING ) 
				.target(MachineState.TRAINING)
				.and()
				.withChoice()
				.source(MachineState.TRAINING)
				.first(MachineState.TARGETING, walkingActuator)
				.last(MachineState.WATING, walkingActuator)
				.and()
				.withChoice()
				.source(MachineState.TARGETING)
				.first(MachineState.ATTACKING, targetingActuator)
				.last(MachineState.WATING, targetingActuator)
				.and()
				.withChoice()
				.source(MachineState.ATTACKING)
				.first(MachineState.PICKING, attackActuator)
				.last(MachineState.WATING, attackActuator)
				.and()
				.withChoice()
				.source(MachineState.PICKING)
				.first(MachineState.WATING, pickingActuator)
				.last(MachineState.WATING, pickingActuator)
				.and()
				.withLocal()
				.source(MachineState.WATING)
				.target(MachineState.PLAYING)
				.timerOnce(500)
				.and()
				.withLocal()
				.source(MachineState.WATING)
				.target(MachineState.IDLE)
				.event(UserAction.STOP_TRAINING);

		/*
		 * .and() .withLocal() .source(MachineState.PREPARING)
		 * .target(MachineState.TARGETING) .and() .withChoice()
		 * .source(MachineState.TARGETING) .first(MachineState.ATTACKING, attackActuator
		 * , attackActuator) .last(MachineState.MOVING , moveActuator) .and()
		 * .withLocal() .source(MachineState.MOVING) .target(MachineState.TARGETING);
		 */
	}

	@Override
	public void configure(StateMachineConfigurationConfigurer<MachineState, IMachineEvent> config) throws Exception {

		config.withConfiguration().taskExecutor(threadPoolTaskExecutor).taskScheduler(taskScheduler).autoStartup(true);
	}


	
	@Bean
	IFactory picoFactory() {
		return new PicocliSpringFactory(ctx);
	}

	@Bean
	CommandLine commandLine() {
		return new CommandLine(this.ctx.getBean(CommandHandler.class), this.ctx.getBean(IFactory.class));
	}

}
