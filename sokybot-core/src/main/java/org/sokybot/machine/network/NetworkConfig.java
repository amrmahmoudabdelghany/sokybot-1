package org.sokybot.machine.network;

import org.sokybot.network.IPacketPublisher;
import org.sokybot.security.Blowfish;
import org.sokybot.security.CRCSecurity;
import org.sokybot.security.CountSecurity;
import org.sokybot.security.IBlowfish;
import org.sokybot.security.ICRCSecurity;
import org.sokybot.security.ICountSecurity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Configuration
public class NetworkConfig {

	
	
	@Bean
	IBlowfish blowfish() { 
		return new Blowfish() ; 
	}
	
	@Bean
	ICRCSecurity crcSecurity() { 
		return new CRCSecurity() ; 
	}
	
	@Bean
	ICountSecurity countSecurity() { 
		return new CountSecurity() ; 
	}
	
	@Bean
	ChannelGroup channelGroup() { 
		return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE) ;
	}
	
	
}
