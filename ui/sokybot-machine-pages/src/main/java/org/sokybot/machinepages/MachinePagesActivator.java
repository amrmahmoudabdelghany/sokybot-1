package org.sokybot.machinepages;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.webview.api.IWebviewConfigurator;
import org.sokybot.webview.api.util.SchemaLoader;
import org.sokybot.machinepages.service.EnvironmentService;
import org.sokybot.machinepages.service.InventoryService;
import org.sokybot.machinepages.service.LogService;
import org.sokybot.machinepages.service.SkillService;
import org.sokybot.machinepages.service.TrainingService;
import org.sokybot.machinepages.service.ConnectionService;
import org.sokybot.machinepages.service.NavigationService;
import org.sokybot.machinepages.service.HealingService;
import org.sokybot.settings.api.IProfileManager;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.security.ICredentialEncryptor;

/**
 * Activator for Machine Pages bundle.
 * Registers declarative UI pages for Inventory, Skills, Training, Environment,
 * and Log.
 */
@Component(immediate = true, property = {
        "event.topics=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED,
        "event.topics=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED
})
public class MachinePagesActivator implements EventHandler {

    private ISokybotContext appCtx;
    private IWebviewConfigurator webviewConfigurator;
    private ISettingsRegistry settingsRegistry;
    private IProfileManager profileManager;
    private ICredentialEncryptor credentialEncryptor;

    @Reference
    public void setAppCtx(ISokybotContext appCtx) {
        this.appCtx = appCtx;
    }

    @Reference
    public void setWebviewConfigurator(IWebviewConfigurator webviewConfigurator) {
        this.webviewConfigurator = webviewConfigurator;
    }

    @Reference
    public void setSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }

    @Reference
    public void setProfileManager(IProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Reference
    public void setCredentialEncryptor(ICredentialEncryptor credentialEncryptor) {
        this.credentialEncryptor = credentialEncryptor;
    }

    private BundleContext bundleContext;

    // Service instances per machine
    private final Map<String, MachineServices> machineServices = new HashMap<>();

    // Loaded UI schemas
    private Map<String, Object> inventorySchema;
    private Map<String, Object> skillsSchema;
    private Map<String, Object> trainingSchema;
    private Map<String, Object> environmentSchema;
    private Map<String, Object> logSchema;
    private Map<String, Object> connectionSchema;
    private Map<String, Object> navigationSchema;
    private Map<String, Object> healingSchema;

    @Activate
    public void activate(ComponentContext componentContext) {
        System.out.println("Starting Machine Pages Bundle");
        this.bundleContext = componentContext.getBundleContext();

        // Load UI schemas
        loadUISchemas();

        // Install for existing machines
        if (appCtx != null && appCtx.isRunning()) {
            installExistingMachines(appCtx);
        }
    }

    @Deactivate
    public void deactivate() {
        System.out.println("Stopping Machine Pages Bundle");
        if (appCtx != null) {
            uninstallAllMachines();
        }

        machineServices.clear();
    }

    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();

        if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED.equals(topic)) {
            IMachineContext machineContext = (IMachineContext) event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
            if (machineContext != null) {
                installMachinePages(machineContext);
            }
        } else if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED.equals(topic)) {
            String fullName = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
            if (fullName != null) {
                uninstallMachinePages(fullName);
            }
        }
    }

    private void installExistingMachines(ISokybotContext ctx) {
        Stream.of(ctx.getGroups())
                .filter(IGroupContext::isRunning)
                .flatMap(g -> Stream.of(g.getMachines()))
                .filter(IMachineContext::isRunning)
                .forEach(this::installMachinePages);
    }

    private void uninstallAllMachines() {
        // Create a copy of keys to avoid concurrent modification if uninstall removes
        // from map
        new ArrayList<>(machineServices.keySet()).forEach(this::uninstallMachinePages);
    }

    private void loadUISchemas() {
        try {
            inventorySchema = SchemaLoader.loadSchema("/ui/inventory.json", getClass());
            skillsSchema = SchemaLoader.loadSchema("/ui/skills.json", getClass());
            trainingSchema = SchemaLoader.loadSchema("/ui/training.json", getClass());
            environmentSchema = SchemaLoader.loadSchema("/ui/environment.json", getClass());
            logSchema = SchemaLoader.loadSchema("/ui/log.json", getClass());
            connectionSchema = SchemaLoader.loadSchema("/ui/connection.json", getClass());
            navigationSchema = SchemaLoader.loadSchema("/ui/navigation.json", getClass());
            healingSchema = SchemaLoader.loadSchema("/ui/healing.json", getClass());

            System.out.println("Machine Pages: UI schemas loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load UI schemas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void installMachinePages(IMachineContext ctx) {
        String machineFullName = ctx.fullName();
        if (machineServices.containsKey(machineFullName)) {
            return; // Already installed
        }

        System.out.println("Installing Machine Pages for: " + machineFullName);

        // Create service instances
        MachineServices services = new MachineServices();
        services.inventoryService = new InventoryService(machineFullName,
                ctx.getGameModel() != null ? ctx.getGameModel().getTrainer() : null);
        services.skillService = new SkillService(machineFullName,
                ctx.getGameModel() != null ? ctx.getGameModel().getTrainer() : null);

        services.trainingService = new TrainingService(machineFullName, ctx, settingsRegistry, profileManager);

        services.connectionService = new ConnectionService(machineFullName, ctx, ctx,
                settingsRegistry, profileManager, credentialEncryptor);

        services.navigationService = new NavigationService(machineFullName, ctx, settingsRegistry);
        services.healingService = new HealingService(machineFullName, ctx, settingsRegistry);

        services.environmentService = new EnvironmentService(machineFullName);
        services.logService = new LogService(machineFullName);

        machineServices.put(machineFullName, services);

        // Register services as EventHandlers
        registerEventHandlers(machineFullName, services);

        // Register pages with webview
        registerPages(machineFullName, services);
    }

    private void uninstallMachinePages(String machineFullName) {
        MachineServices services = machineServices.remove(machineFullName);
        if (services != null) {
            // Unregister EventHandlers
            if (services.inventoryHandlerRegistration != null)
                services.inventoryHandlerRegistration.unregister();
            if (services.skillHandlerRegistration != null)
                services.skillHandlerRegistration.unregister();
            if (services.trainingHandlerRegistration != null)
                services.trainingHandlerRegistration.unregister();
            if (services.environmentHandlerRegistration != null)
                services.environmentHandlerRegistration.unregister();
            if (services.logHandlerRegistration != null)
                services.logHandlerRegistration.unregister();
            if (services.connectionHandlerRegistration != null)
                services.connectionHandlerRegistration.unregister();
            if (services.navigationHandlerRegistration != null)
                services.navigationHandlerRegistration.unregister();
            if (services.healingHandlerRegistration != null)
                services.healingHandlerRegistration.unregister();

            // Shutdown services
            services.shutdown();

            // Remove pages
            if (webviewConfigurator != null) {
                webviewConfigurator.removePage("inventory_" + machineFullName);
                webviewConfigurator.removePage("skills_" + machineFullName);
                webviewConfigurator.removePage("training_" + machineFullName);
                webviewConfigurator.removePage("environment_" + machineFullName);
                webviewConfigurator.removePage("log_" + machineFullName);
                webviewConfigurator.removePage("connection_" + machineFullName);
                webviewConfigurator.removePage("navigation_" + machineFullName);
                webviewConfigurator.removePage("healing_" + machineFullName);
            }
        }
    }

    private void registerEventHandlers(String machineFullName, MachineServices services) {
        if (bundleContext == null) {
            System.err.println("BundleContext not available - cannot register EventHandlers");
            return;
        }

        try {
            // Register InventoryService EventHandler
            Dictionary<String, Object> inventoryProps = new Hashtable<>();
            inventoryProps.put(EventConstants.EVENT_TOPIC, new String[] {
                    "sokybot/game/" + machineFullName + "/InventoryItemUpdateEvent",
                    "sokybot/game/" + machineFullName + "/InventoryOperationEvent",
                    "sokybot/game/" + machineFullName + "/InventorySizeUpdateEvent",
                    "sokybot/game/" + machineFullName + "/ItemObtainedEvent"
            });
            services.inventoryHandlerRegistration = bundleContext.registerService(
                    EventHandler.class, services.inventoryService, inventoryProps);

            // Register SkillService EventHandler
            Dictionary<String, Object> skillProps = new Hashtable<>();
            skillProps.put(EventConstants.EVENT_TOPIC, new String[] {
                    "sokybot/game/" + machineFullName + "/SkillLevelUpEvent",
                    "sokybot/game/" + machineFullName + "/SkillPointsUpdateEvent",
                    "sokybot/game/" + machineFullName + "/CharacterSkillLoadedEvent"
            });
            services.skillHandlerRegistration = bundleContext.registerService(
                    EventHandler.class, services.skillService, skillProps);

            // Register TrainingService EventHandler
            Dictionary<String, Object> trainingProps = new Hashtable<>();
            trainingProps.put(EventConstants.EVENT_TOPIC,
                    "sokybot/game/" + machineFullName + "/TrainerStuckEvent");
            services.trainingHandlerRegistration = bundleContext.registerService(
                    EventHandler.class, services.trainingService, trainingProps);

            // Register ConnectionService EventHandler
            Dictionary<String, Object> connectionProps = new Hashtable<>();
            connectionProps.put(EventConstants.EVENT_TOPIC, new String[] {
                    "sokybot/network/" + machineFullName + "/Connected",
                    "sokybot/network/" + machineFullName + "/Disconnected"
            });
            services.connectionHandlerRegistration = bundleContext.registerService(
                    EventHandler.class, services.connectionService, connectionProps);

            // Register NavigationService EventHandler
            Dictionary<String, Object> navProps = new Hashtable<>();
            navProps.put(EventConstants.EVENT_TOPIC, new String[] {
                    "sokybot/game/" + machineFullName + "/PositionUpdateEvent",
                    "sokybot/game/" + machineFullName + "/MapChangedEvent"
            });
            services.navigationHandlerRegistration = bundleContext.registerService(
                    EventHandler.class, services.navigationService, navProps);

            // Register HealingService EventHandler
            Dictionary<String, Object> healingProps = new Hashtable<>();
            healingProps.put(EventConstants.EVENT_TOPIC, new String[] {
                    "sokybot/game/" + machineFullName + "/UpdateHPEvent",
                    "sokybot/game/" + machineFullName + "/UpdateMPEvent"
            });
            services.healingHandlerRegistration = bundleContext.registerService(
                    EventHandler.class, services.healingService, healingProps);

            // Register EnvironmentService EventHandler
            Dictionary<String, Object> envProps = new Hashtable<>();
            envProps.put(EventConstants.EVENT_TOPIC, new String[] {
                    "sokybot/game/" + machineFullName + "/MonsterSpawnEvent",
                    "sokybot/game/" + machineFullName + "/ItemSpawnEvent",
                    "sokybot/game/" + machineFullName + "/EntityDespawnEvent"
            });
            services.environmentHandlerRegistration = bundleContext.registerService(
                    EventHandler.class, services.environmentService, envProps);

            // Register LogService EventHandler (all events for this machine)
            Dictionary<String, Object> logProps = new Hashtable<>();
            logProps.put(EventConstants.EVENT_TOPIC,
                    "sokybot/game/" + machineFullName + "/*");
            services.logHandlerRegistration = bundleContext.registerService(
                    EventHandler.class, services.logService, logProps);

            System.out.println("Machine Pages: EventHandlers registered for " + machineFullName);
        } catch (Exception e) {
            System.err.println("Failed to register EventHandlers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerPages(String machineFullName, MachineServices services) {
        if (webviewConfigurator == null)
            return;

        // Register Inventory page
        if (inventorySchema != null) {
            webviewConfigurator.addDeclarativePage(
                    "inventory_" + machineFullName,
                    "Inventory",
                    "Package",
                    deepCopySchema(inventorySchema));
            webviewConfigurator.registerSchemaHandler(
                    "inventory_" + machineFullName,
                    (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", deepCopySchema(inventorySchema));
                        result.put("state", services.inventoryService.getInitialState());
                        return result;
                    });
            webviewConfigurator.registerActionHandler(
                    "inventory_" + machineFullName,
                    (action, data) -> services.inventoryService.handleAction(action, data));
            webviewConfigurator.registerStreamHandler(
                    "inventory_" + machineFullName,
                    "inventory",
                    (params) -> services.inventoryService.streamInventory(),
                    "items");
        }

        // Register Skills page
        if (skillsSchema != null) {
            webviewConfigurator.addDeclarativePage(
                    "skills_" + machineFullName,
                    "Skills",
                    "Zap",
                    deepCopySchema(skillsSchema));
            webviewConfigurator.registerSchemaHandler(
                    "skills_" + machineFullName,
                    (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", deepCopySchema(skillsSchema));
                        result.put("state", services.skillService.getInitialState());
                        return result;
                    });
            webviewConfigurator.registerActionHandler(
                    "skills_" + machineFullName,
                    (action, data) -> services.skillService.handleAction(action, data));
            webviewConfigurator.registerStreamHandler(
                    "skills_" + machineFullName,
                    "skills",
                    (params) -> services.skillService.streamSkills(),
                    "skills");
        }

        // Register Training page
        if (trainingSchema != null) {
            webviewConfigurator.addDeclarativePage(
                    "training_" + machineFullName,
                    "Training",
                    "Target",
                    deepCopySchema(trainingSchema));
            webviewConfigurator.registerSchemaHandler(
                    "training_" + machineFullName,
                    (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", deepCopySchema(trainingSchema));
                        result.put("state", services.trainingService.getInitialState());
                        return result;
                    });
            webviewConfigurator.registerActionHandler(
                    "training_" + machineFullName,
                    (action, data) -> services.trainingService.handleAction(action, data));
            webviewConfigurator.registerStreamHandler(
                    "training_" + machineFullName,
                    "training",
                    (params) -> services.trainingService.streamTraining(),
                    "training");
        }

        // Register Environment page
        if (environmentSchema != null) {
            webviewConfigurator.addDeclarativePage(
                    "environment_" + machineFullName,
                    "Environment",
                    "Map",
                    deepCopySchema(environmentSchema));
            webviewConfigurator.registerSchemaHandler(
                    "environment_" + machineFullName,
                    (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", deepCopySchema(environmentSchema));
                        result.put("state", services.environmentService.getInitialState());
                        return result;
                    });
            webviewConfigurator.registerActionHandler(
                    "environment_" + machineFullName,
                    (action, data) -> services.environmentService.handleAction(action, data));
            webviewConfigurator.registerStreamHandler(
                    "environment_" + machineFullName,
                    "environment",
                    (params) -> services.environmentService.streamEnvironment(),
                    "environment");
        }

        // Register Log page
        if (logSchema != null) {
            webviewConfigurator.addDeclarativePage(
                    "log_" + machineFullName,
                    "Log",
                    "FileText",
                    deepCopySchema(logSchema));
            webviewConfigurator.registerSchemaHandler(
                    "log_" + machineFullName,
                    (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", deepCopySchema(logSchema));
                        result.put("state", services.logService.getInitialState());
                        return result;
                    });
            webviewConfigurator.registerActionHandler(
                    "log_" + machineFullName,
                    (action, data) -> services.logService.handleAction(action, data));
            webviewConfigurator.registerStreamHandler(
                    "log_" + machineFullName,
                    "log",
                    (params) -> services.logService.streamLog(),
                    "events");
        }

        // Register Connection page
        if (connectionSchema != null) {
            webviewConfigurator.addDeclarativePage(
                    "connection_" + machineFullName,
                    "Connection",
                    "Link",
                    deepCopySchema(connectionSchema));
            webviewConfigurator.registerSchemaHandler(
                    "connection_" + machineFullName,
                    (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", deepCopySchema(connectionSchema));
                        result.put("state", services.connectionService.getInitialState());
                        return result;
                    });
            webviewConfigurator.registerActionHandler(
                    "connection_" + machineFullName,
                    (action, data) -> services.connectionService.handleAction(action, data));
            webviewConfigurator.registerStreamHandler(
                    "connection_" + machineFullName,
                    "connection",
                    (params) -> services.connectionService.streamState(),
                    "state");
        }

        // Register Navigation page
        if (navigationSchema != null) {
            webviewConfigurator.addDeclarativePage(
                    "navigation_" + machineFullName,
                    "Navigation",
                    "Compass",
                    deepCopySchema(navigationSchema));
            webviewConfigurator.registerSchemaHandler(
                    "navigation_" + machineFullName,
                    (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", deepCopySchema(navigationSchema));
                        result.put("state", services.navigationService.getInitialState());
                        return result;
                    });
            webviewConfigurator.registerActionHandler(
                    "navigation_" + machineFullName,
                    (action, data) -> services.navigationService.handleAction(action, data));
            webviewConfigurator.registerStreamHandler(
                    "navigation_" + machineFullName,
                    "navigation",
                    (params) -> services.navigationService.streamNavigation(),
                    "state");
        }

        // Register Healing page
        if (healingSchema != null) {
            webviewConfigurator.addDeclarativePage(
                    "healing_" + machineFullName,
                    "Healing",
                    "Heart",
                    deepCopySchema(healingSchema));
            webviewConfigurator.registerSchemaHandler(
                    "healing_" + machineFullName,
                    (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", deepCopySchema(healingSchema));
                        result.put("state", services.healingService.getInitialState());
                        return result;
                    });
            webviewConfigurator.registerActionHandler(
                    "healing_" + machineFullName,
                    (action, data) -> services.healingService.handleAction(action, data));
            webviewConfigurator.registerStreamHandler(
                    "healing_" + machineFullName,
                    "healing",
                    (params) -> services.healingService.streamHealing(),
                    "state");
        }

        System.out.println("Machine Pages: Pages registered for " + machineFullName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopySchema(Map<String, Object> original) {
        if (original == null)
            return new HashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(original);
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            System.err.println("Failed to deep copy schema: " + e.getMessage());
            return new HashMap<>(original);
        }
    }

    /**
     * Container for all services for a single machine.
     */
    private static class MachineServices {
        InventoryService inventoryService;
        SkillService skillService;
        TrainingService trainingService;
        EnvironmentService environmentService;
        LogService logService;
        ConnectionService connectionService;
        NavigationService navigationService;
        HealingService healingService;

        ServiceRegistration<EventHandler> inventoryHandlerRegistration;
        ServiceRegistration<EventHandler> skillHandlerRegistration;
        ServiceRegistration<EventHandler> trainingHandlerRegistration;
        ServiceRegistration<EventHandler> environmentHandlerRegistration;
        ServiceRegistration<EventHandler> logHandlerRegistration;
        ServiceRegistration<EventHandler> connectionHandlerRegistration;
        ServiceRegistration<EventHandler> navigationHandlerRegistration;
        ServiceRegistration<EventHandler> healingHandlerRegistration;

        void shutdown() {
            if (inventoryService != null)
                inventoryService.shutdown();
            if (skillService != null)
                skillService.shutdown();
            if (trainingService != null)
                trainingService.shutdown();
            if (environmentService != null)
                environmentService.shutdown();
            if (logService != null)
                logService.shutdown();
            if (connectionService != null)
                connectionService.shutdown();
            if (navigationService != null)
                navigationService.shutdown();
            if (healingService != null)
                healingService.shutdown();
        }
    }
}
