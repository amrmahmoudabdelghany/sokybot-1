package org.sokybot.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineConfig;
import org.sokybot.machine.MachineState;
import org.sokybot.settings.ISettingsManager;
import org.sokybot.settings.Settings;
import org.sokybot.machine.model.UserAction;
import org.sokybot.proxy.IProxyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

/**
 * Factory implementation for creating engine instances.
 * 
 * Creates Spring contexts with MachineConfig for each machine.
 * Each engine instance wraps a Spring application context.
 */
@Component(service = IEngineFactory.class)
public class EngineFactory implements IEngineFactory {
    
    private static final Logger log = LoggerFactory.getLogger(EngineFactory.class);
    
    private final Map<String, EngineImpl> engines = new ConcurrentHashMap<>();

    @Reference
    private ISettingsManager settingsManager;

    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public IEngine createEngine(String machineId, IProxyConnection proxyConnection, 
                                String groupName, String machineName) {
        
        synchronized (engines) {
            if (engines.containsKey(machineId)) {
                throw new IllegalStateException("Engine already exists for machine: " + machineId);
            }
            
            log.info("Creating engine for machine: {}", machineId);
            
            try {
                // ... (Existing Spring Context creation code) ...
                ConfigurableApplicationContext springContext = new SpringApplicationBuilder(MachineConfig.class)
                        .properties(Map.of(
                                AppConstants.MACHINE_NAME, machineName,
                                AppConstants.GROUP_NAME, groupName,
                                "spring.config.location", "classpath:machine.properties"))
                        .initializers((ctx) -> {
                            ctx.getBeanFactory().registerSingleton("machineId", machineId);
                            ctx.getBeanFactory().registerSingleton("groupName", groupName);
                            ctx.getBeanFactory().registerSingleton("machineName", machineName);
                            ctx.getBeanFactory().registerSingleton("proxyConnection", proxyConnection);
                        })
                        .run();
                
                // ...Wrapper connections...
                 try {
                    org.sokybot.proxy.IConnectionListener connectionHandler = 
                        springContext.getBean(org.sokybot.proxy.IConnectionListener.class);
                    if (connectionHandler != null && proxyConnection != null) {
                        proxyConnection.setConnectionListener(connectionHandler);
                    }
                } catch (Exception e) {
                    log.warn("Could not wire ConnectionHandler: {}", e.getMessage());
                }

                // Wrap Spring context as IEngine
                EngineImpl engine = new EngineImpl(machineId, groupName, machineName, springContext, eventAdmin, settingsManager, bundleContext);
                engines.put(machineId, engine);
                
                log.info("Engine created successfully for machine: {}", machineId);
                return engine;
                
            } catch (Exception e) {
                log.error("Failed to create engine for machine: {}", machineId, e);
                throw new RuntimeException("Failed to create engine for machine: " + machineId, e);
            }
        }
    }
    
    // ... (destroyEngine and getEngine remain same) ...
    @Override
    public void destroyEngine(String machineId) {
        synchronized (engines) {
            EngineImpl engine = engines.remove(machineId);
            if (engine != null) {
                log.info("Destroying engine for machine: {}", machineId);
                engine.stop();
            } else {
                log.warn("Engine not found for machine: {}", machineId);
            }
        }
    }
    
    @Override
    public IEngine getEngine(String machineId) {
        return engines.get(machineId);
    }

    /**
     * Internal implementation of IEngine that wraps a Spring context and manages Settings.
     */
    private static class EngineImpl implements IEngine, EventHandler {
        
        private final String machineId;
        private final String groupName;
        private final String machineName;
        private final ConfigurableApplicationContext springContext;
        private final EventAdmin eventAdmin;
        private final ISettingsManager settingsManager;
        private final BundleContext bundleContext;
        
        private volatile boolean running = false;
        private volatile Settings settings;
        private ServiceRegistration<EventHandler> eventRegistration;
        
        public EngineImpl(String machineId, String groupName, String machineName, 
                          ConfigurableApplicationContext springContext, 
                          EventAdmin eventAdmin,
                          ISettingsManager settingsManager,
                          BundleContext bundleContext) {
            this.machineId = machineId;
            this.groupName = groupName; // Persist group logic
            this.machineName = machineName;
            this.springContext = springContext;
            this.eventAdmin = eventAdmin;
            this.settingsManager = settingsManager;
            this.bundleContext = bundleContext;
            
            // Initial load
            reloadSettings();
            
            // Register State Machine Listener
            registerStateListener();
            registerSettingsListener();
        }

        private void reloadSettings() {
            try {
                this.settings = settingsManager.loadSettings(groupName, machineName, "core", Settings.class);
            } catch (Exception e) {
                log.error("Failed to load settings for machine {}", machineId, e);
            }
        }

        private void registerSettingsListener() {
           if (bundleContext != null) {
               java.util.Dictionary<String, Object> props = new java.util.Hashtable<>();
               props.put(org.osgi.service.event.EventConstants.EVENT_TOPIC, "sokybot/settings/updated");
               // Filter events for this machine
               String filter = "(&(groupName=" + groupName + ")(machineName=" + machineName + "))";
               props.put(org.osgi.service.event.EventConstants.EVENT_FILTER, filter);
               
               eventRegistration = bundleContext.registerService(EventHandler.class, this, props);
           }
        }

        @Override
        public void handleEvent(Event event) {
            // Reload settings when notified
            log.debug("Received settings update event for machine {}", machineId);
            Settings newSettings = (Settings) event.getProperty("settings");
            if (newSettings != null) {
                this.settings = newSettings;
            } else {
                reloadSettings();
            }
        }

        @SuppressWarnings("unchecked")
        private void registerStateListener() {
            try {
                 StateMachine<MachineState, IMachineEvent> sm = springContext.getBean(StateMachine.class);
                 sm.addStateListener(new StateMachineListenerAdapter<>() {
                     @Override
                     public void stateChanged(State<MachineState, IMachineEvent> from, State<MachineState, IMachineEvent> to) {
                         if (to != null) {
                             postStateEvent(to.getId().name());
                         }
                     }
                 });
            } catch (Exception e) {
                log.warn("Could not register StateMachine listener: {}", e.getMessage());
            }
        }

        private void postStateEvent(String stateName) {
            if (eventAdmin != null) {
                Map<String, Object> props = new HashMap<>();
                props.put("machineId", machineId);
                props.put("state", stateName);
                String topic = "sokybot/machine/state";
                eventAdmin.postEvent(new Event(topic, props));
            }
        }

        @Override
        public void sendEvent(String eventName) {
            try {
                UserAction action = UserAction.valueOf(eventName);
                @SuppressWarnings("unchecked")
                StateMachine<MachineState, IMachineEvent> sm = springContext.getBean(StateMachine.class);
                sm.sendEvent(action);
                log.debug("Sent event {} to machine {}", eventName, machineId);
            } catch (IllegalArgumentException e) {
                 log.error("Invalid event name: {}", eventName);
                 throw new IllegalArgumentException("Unknown event: " + eventName);
            } catch (Exception e) {
                 log.error("Failed to send event {} to machine {}", eventName, machineId, e);
                 throw new RuntimeException("Failed to send event", e);
            }
        }
        
        @Override
        public String getMachineId() {
            return machineId;
        }
        
        @Override
        public void start() {
            if (!running) {
                synchronized (this) {
                    if (!running) {
                        if (!springContext.isRunning()) {
                            springContext.start();
                        }
                        running = true;
                        log.debug("Engine started for machine: {}", machineId);
                    }
                }
            }
        }
        
        @Override
        public void stop() {
            if (running) {
                synchronized (this) {
                    if (running) {
                        if (eventRegistration != null) {
                            eventRegistration.unregister();
                        }
                        try {
                            springContext.stop();
                            springContext.close();
                            running = false;
                            log.debug("Engine stopped for machine: {}", machineId);
                        } catch (Exception e) {
                            log.error("Error stopping engine for machine: {}", machineId, e);
                            running = false;
                        }
                    }
                }
            }
        }
        
        @Override
        public Settings getSettings() {
             return this.settings;
        }

        @Override
        public boolean isRunning() {
            return running && springContext.isRunning();
        }
    }
}
}
