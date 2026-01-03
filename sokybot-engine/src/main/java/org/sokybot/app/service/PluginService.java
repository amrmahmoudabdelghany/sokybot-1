package org.sokybot.app.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.felix.atomos.Atomos;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.service.log.LoggerFactory;
import org.sokybot.service.IGameLoader;
import org.sokybot.utils.SokybotIOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PluginService implements SmartLifecycle, IPluginService {

	private Bundle bundle;

	@Autowired
	private ApplicationContext ctx;

	@PostConstruct
	void init() {

		log.info("Initializing osgi container");
		ServiceLoader<ConnectFrameworkFactory> loader = ServiceLoader.load(ConnectFrameworkFactory.class);
		ConnectFrameworkFactory factory = loader.findFirst().get();

		Framework framework = factory.newFramework(getFrameworkConfig(), Atomos.newAtomos().getModuleConnector());

		try {
			framework.init();
		} catch (BundleException e) {
			throw new IllegalStateException("Could not initialize osgi framework ", e);
		}
		this.bundle = framework;
		
		

	}

	private Map<String, String> getFrameworkConfig() {
	
		boolean prodConfig = Stream.of(this.ctx.getEnvironment().getActiveProfiles())
				.anyMatch((p) -> p.equalsIgnoreCase("prod"));
		Map<String, String> config = null;

		try {
			if (prodConfig) {

				config = SokybotIOUtils
						.readProperties(this.ctx.getResource("classpath:osgi.properties").getInputStream());

			} else {
				config = SokybotIOUtils
						.readProperties(this.ctx.getResource("classpath:osgi-dev.properties").getInputStream());
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Could not load osgi configurations ", e);
		}

		return config;
	}

	@Override
	public boolean isRunning() {
		return this.bundle.getState() == Framework.ACTIVE;
	}

	@Override
	public void start() {

		log.info("Starting osgi container");
		try {
			this.bundle.start();
		} catch (BundleException e) {
			throw new IllegalStateException("Could not start osgi framework ", e);
		}
	}

	@Override
	public void stop() {
		try {
			this.bundle.stop();
		} catch (BundleException e) {
			throw new IllegalStateException("Could not stop osgi framework ", e);
		}
	}

	@Override
	public <T> List<T> findService(Class<T> targetType) {

		BundleContext ctx = this.bundle.getBundleContext();
		List<T> res = new ArrayList<>();
		try {

			ServiceReference<?>[] references = ctx.getAllServiceReferences(targetType.getName(), null);
			if (references != null) {

				for (ServiceReference<?> r : references) {
					res.add(targetType.cast(ctx.getService(r)));
				}
				log.info("Found {} game loader(s)" , references.length);
			}else { 
				log.info("No game loaders registered");
			}
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(e);
		}

		return res;
	}

	
	@Override
	public void addServiceListener(ServiceListener listener, Class<?> targetType) {

		try {
			log.info("Listening for {} " , targetType.getName() );
			this.bundle.getBundleContext()
			.addServiceListener(listener, "(objectClass=" + IGameLoader.class.getName() + ")");
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(e);
		}

	}
	
	@Override
	public <T> void deployService(Class<T> clazz, T service) {
	 
		BundleContext ctx = this.bundle.getBundleContext() ; 
		
		ctx.registerService(clazz, service, null) ; 
		
	}
	
	@Override
	public <T> Optional<T> getService(ServiceReference<T> ref) {
		
		return Optional.ofNullable(this.bundle.getBundleContext().getService(ref)) ; 
		
	}

}
