package org.sokybot.machine.controller;


import org.slf4j.Logger;
import org.sokybot.app.AppConstants;
import org.sokybot.app.service.IGameLoaderService;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.StateEntry;
import org.sokybot.machine.Transition;
import org.sokybot.machine.model.UserAction;
import org.sokybot.machine.network.ClientChannelInitializer;
import org.sokybot.machine.network.NetworkAttributes;
import org.sokybot.machine.network.ServerChannelInitializer;
import org.sokybot.machinegroup.gamemodel.setting.BotType;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.utils.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.EventHeader;

import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Controller;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

@Controller
@WithStateMachine
public class ConnectionHandler {

	@Autowired
	Settings config;

	@Autowired
	ApplicationContext ctx;

	@Autowired
	StateMachine<MachineState, IMachineEvent> machine;

	@Autowired
	Logger log;

	@Autowired
	ChannelGroup channelGroup;
	


	@StateEntry(source = {MachineState.STARTING} , target = MachineState.LAUNCHING)
	public void launchingClient(ExtendedState extendedState) {
		try {
			log.info("Listening on port 15000 ");
			listenToClient().addListener((channelCtx) -> {

				ctx.getBean(IGameLoaderService.class)
						.findGameLoader(AppConstants.CLIENT_DEFAULT_LOADER)
						.ifPresentOrElse((gl) -> {
							log.info("Lunching game client");
							int pid = gl.launch(getClientPath());
							extendedState.getVariables().put(AppConstants.CLIENT_PID, pid);
							machine.sendEvent(UserAction.CLIENT_ATTACHED);
						}, () -> {
							log.info("Could not find game loader");
							machine.sendEvent(UserAction.DISCONNECT);
						});

			}).channel().closeFuture().sync();

		} catch (InterruptedException e) {
			machine.sendEvent(UserAction.DISCONNECT);
		}

	}

	@StateEntry(source = {MachineState.STARTING , MachineState.LAUNCHING}  , target = MachineState.CONNECTING)
	public void connecting(ExtendedState extendedState) {
		Settings settings = this.ctx.getBean(Settings.class) ; 
		String targetGateway = settings.getTargetGateway() ; 
		int port =this.ctx.getBean(ISroMaterialDAO.class).getPort() ;
		// connect to server
		log.info("Connecting to server " + targetGateway + "/" +  port);
		try {
			
			connect(targetGateway,port).sync();

		} catch (InterruptedException e) {
			this.machine.sendEvent(UserAction.DISCONNECT) ; 
		}

	}

	@StateEntry(target = MachineState.DISCONNECTING)
	public void disconnecting() {
		log.info("Disconnecting from  {} ", this.config.getTargetGateway());

		this.channelGroup
				.disconnect((channel) -> channel.attr(NetworkAttributes.TRANSPORT).get() == NetworkPeer.SERVER);

		//this.executor.execute(()->);
		this.machine.sendEvent(UserAction.KILL_CLIENT);
	}

	
	@StateEntry(source =  MachineState.LOGINING , target = MachineState.REDIRECTING)	
	public void redirect(@EventHeader(name = AppConstants.MACHINE_AGENT_HOST , required =  true )String agentHost , 
						  @EventHeader(name = AppConstants.MACHINE_AGENT_PORT , required = true ) short agentPort , 
						  @EventHeader(name = AppConstants.MACHINE_LOGIN_ID , required = true) int loginId ) throws InterruptedException { 
		log.info("Redirecting to agent server");
		//0 - extract agent address 
		//1 - inject client  
		//2 - listen to client 
		//3 - connect to agent 
		
		
		if(this.config.getBotType() != BotType.CLIENTLESS) { 
			listenToClient()
			.sync()
				.addListener((serverCtx)->{
					log.info("Binding on {} " , AppConstants.CLIENT_DEFAULT_PORT ) ; 
					injectClient(patchPacket(loginId, "127.0.0.1", AppConstants.CLIENT_DEFAULT_PORT));
					
				}).channel().closeFuture().sync();
		}
		
		this.channelGroup.disconnect((channel)->channel.attr(NetworkAttributes.TRANSPORT).get() == NetworkPeer.SERVER).sync() ; 
		//this.channelGroup.disconnect() ;
		
		connect(agentHost, agentPort).sync() ; 
		
		log.info("Agent Address {}:{} " , agentHost , agentPort);
		
		
	}
 
	private void injectClient(MutablePacket packet) throws InterruptedException { 
		this.channelGroup.writeAndFlush(packet, 
				channel ->channel.attr(NetworkAttributes.TRANSPORT).get() == NetworkPeer.CLIENT ) 
		.sync().addListener((ctx)->{
			log.info("Client Injected ") ; 
		});
		
	}
	private MutablePacket patchPacket(int loginId , String localHost , int port) { 
		int patchLen = 13 + localHost.length();

		return  MutablePacket.getBuilder(patchLen , ServerOpcode.LOGIN_RESPONSE)
				.packetEncoding(Encoding.ENCRYPTED)
				.dataEncoding(Encoding.PLAIN)
				.put((byte) 0x01) // login result																				// indicator
				.putInt(loginId)
				.putShort((short) localHost.length())
				.putBytes(localHost.getBytes())
				.putInt(port)
				.putShort((short) 0x00)				
				.build();
	}

	@Transition(source = MachineState.WITH_CLIENT  , target  = MachineState.WITHOUT_CLIENT)
	public void disconnectClient(ExtendedState extendedState) {

		
		Integer cpid = extendedState.get(AppConstants.CLIENT_PID, Integer.class);
		if (cpid != null) {
			this.channelGroup
			.disconnect((channel) -> channel.attr(NetworkAttributes.TRANSPORT).get() == NetworkPeer.CLIENT);

			if (cpid != -1) {
				Helper.killProcess(cpid.intValue());
				extendedState.getVariables().put(AppConstants.CLIENT_PID, -1);
			}

		}
	}

	private ChannelFuture connect(String host , int port) {
		return new Bootstrap().group(this.ctx.getBean(EventLoopGroup.class))
				.channel(NioSocketChannel.class)
				.handler(this.ctx.getBean(ServerChannelInitializer.class))
				.connect(host , port);

	}

	private ChannelFuture listenToClient() {
		return new ServerBootstrap().group(this.ctx.getBean(EventLoopGroup.class))
				.channel(NioServerSocketChannel.class)
				.childHandler(this.ctx.getBean(ClientChannelInitializer.class))
				.option(ChannelOption.SO_BACKLOG, 0)
				.bind(AppConstants.CLIENT_DEFAULT_PORT);

	}

	private String getClientPath() {
		String clientPath = ctx.getBean(ISroMaterialDAO.class).getGamePath() + "\\sro_client.exe";
		// TODO validate client path before return
		return clientPath;
	}

}
