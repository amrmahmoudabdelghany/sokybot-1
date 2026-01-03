package org.sokybot.machine;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.sokybot.machine.network.PacketListener;
import org.sokybot.machine.network.PacketListenerAdapter;
import org.sokybot.machine.network.PacketListener.PacketSource;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.packet.ImmutablePacket;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;

@Configuration
@Order(0)
public class PacketListenerInstaller implements BeanPostProcessor {

	@Autowired
	private IPacketPublisher packetPublisher;

	@Autowired
	private ApplicationContext ctx;

	@Override
	public Object postProcessAfterInitialization(Object targetBean, String beanName) throws BeansException {

		Stream.of(ClassUtils.getUserClass(targetBean.getClass()).getDeclaredMethods())
				.filter((m) -> m.isAnnotationPresent(PacketListener.class))
				.forEach((method) -> {

					PacketListener listener = method.getAnnotation(PacketListener.class);

					if (hasValidParams(method)) {
						int opcodes[] = listener.opcode();
						PacketSource ps = listener.packetSource();
						IPacketObserver obs = this.ctx.getBean(PacketListenerAdapter.class, targetBean, method, ps);

						if (opcodes.length > 0) {

							for (int opcode : opcodes) {

								this.packetPublisher.subscribe(obs, opcode);
							}

						} else {

							this.packetPublisher.subscribe(obs, IPacketPublisher.ANY);
						}
					}else { 
						throw new IllegalStateException("Invalid method paramters " + method.getName());
					}
				});

		return targetBean;
	}

	private boolean hasValidParams(Method m) {
		return Stream.of(m.getParameters())
				.allMatch((p) -> p.getType() == ImmutablePacket.class
						|| this.ctx.getBeanNamesForType(p.getType()).length > 0);
	}



}
