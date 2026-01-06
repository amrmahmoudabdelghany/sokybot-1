package org.sokybot.engine;

import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.model.UserAction;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sokybot.IMachinePageViewer;
import org.sokybot.proxy.IConnectionListener;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.proxy.IProxyConnectionFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.statemachine.StateMachine;

@Configuration
@ComponentScan({"org.sokybot"})
public class EngineConfig {

    @Autowired
    ApplicationContext ctx;

    @Autowired
    Logger log;

    @Autowired
    StateMachine<MachineState, IMachineEvent> machine;

    /**
     * Expose OSGi BundleContext as Spring bean for service registration
     */
    @Bean
    public BundleContext bundleContext() {
        BundleContext context = EngineActivator.getBundleContext();
        if (context == null) {
            log.warn("BundleContext not available - running outside OSGi container");
        }
        return context; // May be null if not running in OSGi
    }

    @Bean
    public IMachinePageViewer machinePageViewer(BundleContext bundleContext) {
        if (bundleContext == null) return null;
        return bundleContext.getService(bundleContext.getServiceReference(IMachinePageViewer.class));
    }

    /**
     * Get IProxyConnectionFactory from OSGi service registry.
     */
    @Bean
    public IProxyConnectionFactory proxyConnectionFactory(BundleContext bundleContext) {
        if (bundleContext == null) {
            log.warn("BundleContext not available - proxy factory unavailable");
            return null;
        }
        ServiceReference<IProxyConnectionFactory> ref = bundleContext.getServiceReference(IProxyConnectionFactory.class);
        if (ref == null) {
            log.warn("IProxyConnectionFactory service not found in OSGi registry");
            return null;
        }
        return bundleContext.getService(ref);
    }

    /**
     * Create IProxyConnection for this machine using the factory.
     * The ConnectionHandler implements IConnectionListener and will receive callbacks.
     */
    @Bean
    public IProxyConnection proxyConnection(IProxyConnectionFactory factory, IConnectionListener listener) {
        if (factory == null) {
            log.warn("IProxyConnectionFactory not available - returning null proxy");
            return null;
        }
        String machineId = ctx.getId();
        log.info("Creating proxy connection for machine: {}", machineId);
        return factory.createConnection(machineId, listener);
    }

    /**
     * Expose IPacketPublisher from the proxy connection.
     * This is required for PacketListenerInstaller to wire up controller listeners.
     */
    @Bean
    public org.sokybot.network.IPacketPublisher packetPublisher(IProxyConnection connection) {
        if (connection == null) return null;
        return connection.getPacketPublisher();
    }

    @Bean
    @Order(4)
    ApplicationRunner initializeUserConfig() {
        return args -> {

            Settings config = this.ctx.getBean(Settings.class);

            if (args.containsOption(AppConstants.MACHINE_AUTO_LOGIN)) {
                config.setAutoLogin(true);
            }

            args.getNonOptionArgs()
                    .stream()
                    .map((l) -> l.split("="))
                    .forEach((pair) -> {
                        if (pair.length == 2) {
                            switch (pair[0]) {

                            case AppConstants.MACHINE_TARGET_GATEWAY:
                                config.setTargetGateway(pair[1]);
                                break;
                            case AppConstants.MACHINE_USER_NAME:
                                config.setUsername(pair[1]);
                                break;
                            case AppConstants.MACHINE_PASSWORD:
                                config.setPassword(pair[1]);
                                break;
                            case AppConstants.MACHINE_PASSCODE:
                                config.setPasscode(pair[1]);
                                break;
                            case AppConstants.MACHINE_TARGET_AGENT:
                                config.setTargetAgent(pair[1]);
                                break;
                            }
                        }
                    });

             boolean isAcceptable = machine.sendEvent(UserAction.CONFIG_COMMIT);
             log.info("CONFIG_COMMIT event is acceptable {} ", isAcceptable);
        };
    }
}
