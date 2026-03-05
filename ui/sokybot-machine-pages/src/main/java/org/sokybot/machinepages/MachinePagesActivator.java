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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.machinepages.api.IScriptedPage;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.webview.api.IWebviewConfigurator;
import org.sokybot.logging.api.ILogStreamService;
import org.sokybot.packetsniffer.api.IPacketSnifferPage;
import org.sokybot.packetsniffer.api.IPacketSnifferRegistry;

/**
 * Activator for Machine Pages bundle.
 * Dynamically loads and registers UI pages from Groovy scripts.
 */
@Component(service = { EventHandler.class, MachinePagesActivator.class }, property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED,
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED
}, immediate = true)
public class MachinePagesActivator implements EventHandler {

    private static final String LOG_STREAM_ID = "Log";

    private ISokybotContext appCtx;
    private IWebviewConfigurator webviewConfigurator;
    private ScriptPageLoader pageLoader;
    private ILogStreamService logStreamService;
    private IPacketSnifferRegistry packetSnifferRegistry;

    @Reference
    public void setAppCtx(ISokybotContext appCtx) {
        this.appCtx = appCtx;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setPacketSnifferRegistry(IPacketSnifferRegistry packetSnifferRegistry) {
        this.packetSnifferRegistry = packetSnifferRegistry;
    }

    public void unsetPacketSnifferRegistry(IPacketSnifferRegistry packetSnifferRegistry) {
        if (this.packetSnifferRegistry == packetSnifferRegistry) {
            this.packetSnifferRegistry = null;
        }
    }

    @Reference
    public void setWebviewConfigurator(IWebviewConfigurator webviewConfigurator) {
        this.webviewConfigurator = webviewConfigurator;
    }

    @Reference
    public void setPageLoader(ScriptPageLoader pageLoader) {
        this.pageLoader = pageLoader;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setLogStreamService(ILogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    public void unsetLogStreamService(ILogStreamService logStreamService) {
        if (this.logStreamService == logStreamService) {
            this.logStreamService = null;
        }
    }

    private BundleContext bundleContext;

    // Service instances per machine
    private final Map<String, MachineServices> machineServices = new HashMap<>();

    @Activate
    public void activate(ComponentContext componentContext) {
        System.out.println("Starting Machine Pages Bundle");
        this.bundleContext = componentContext.getBundleContext();

        // Listen for script changes to reload pages
        this.pageLoader.addPageListener(this::reloadPageForAllMachines);

        // Install for existing machines
        if (appCtx != null) {
            installExistingMachines(appCtx);
        }
    }

    @Deactivate
    public void deactivate() {
        System.out.println("Stopping Machine Pages Bundle");
        if (appCtx != null) {
            uninstallAllMachines();
        }

        if (pageLoader != null) {
            pageLoader.removePageListener(this::reloadPageForAllMachines);
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
        IGroupContext[] groups = ctx.getGroups();
        System.out.println("Machine Pages: Found " + groups.length + " existing groups to scan");
        Stream.of(groups)
                .flatMap(g -> {
                    IMachineContext[] machines = g.getMachines();
                    System.out.println("Machine Pages: Group " + g.name() + " has " + machines.length + " machines");
                    return Stream.of(machines);
                })
                .forEach(this::installMachinePages);
    }

    private void uninstallAllMachines() {
        new ArrayList<>(machineServices.keySet()).forEach(this::uninstallMachinePages);
    }

    private void installMachinePages(IMachineContext ctx) {
        String machineFullName = ctx.fullName();
        if (machineServices.containsKey(machineFullName)) {
            return; // Already installed
        }

        System.out.println("Installing Machine Pages for: " + machineFullName);

        MachineServices services = new MachineServices(machineFullName, ctx);
        machineServices.put(machineFullName, services);

        // Load and register all available pages from script loader
        for (String pageName : pageLoader.getAvailablePages()) {
            registerScriptedPage(services, pageName);
        }
    }

    private void uninstallMachinePages(String machineFullName) {
        MachineServices services = machineServices.remove(machineFullName);
        if (services != null) {
            services.shutdown();
        }
    }

    private void reloadPageForAllMachines(String pageName) {
        System.out.println("Reloading scripted page: " + pageName);
        for (MachineServices services : machineServices.values()) {
            // Unregister old version if it exists
            services.unregisterPage(pageName);
            // Register new version
            registerScriptedPage(services, pageName);
        }
    }

    private void registerScriptedPage(MachineServices services, String pageName) {
        pageLoader.createPage(pageName, services.machineContext).ifPresent(page -> {
            String pageId = pageName + "_" + services.machineFullName;
            System.out.println("Registering scripted page " + pageId);

            services.addPage(pageName, page);

            // Register with Webview
            if (webviewConfigurator != null) {
                Map<String, Object> schema = pageLoader.loadSchema(pageName);
                if (schema.isEmpty()) {
                    schema = page.getSchema();
                }

                webviewConfigurator.addDeclarativePage(
                        pageId,
                        page.getTitle(),
                        page.getIcon(),
                        schema);

                // Schema handler: for the Log page with central logging available,
                // build initial state from ILogStreamService so that the declarative
                // schema (Log.json) can bind to Log.events / Log.level / Log.feature.
                if ("log".equalsIgnoreCase(pageName) && logStreamService != null) {
                    webviewConfigurator.registerSchemaHandler(pageId, (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", pageLoader.loadSchema(pageName));

                        String machineFullName = services.machineFullName;
                        String level = "ALL";
                        String feature = "ALL";
                        int maxEntries = 500;

                        java.util.List<java.util.Map<String, Object>> entries =
                                logStreamService.getRecentEntries(machineFullName, level, feature, maxEntries);

                        Map<String, Object> state = buildLogState(entries, maxEntries, level, feature);
                        result.put("state", state);
                        return result;
                    });
                } else {
                    webviewConfigurator.registerSchemaHandler(pageId, (request) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("schema", pageLoader.loadSchema(pageName));
                        result.put("state", page.getInitialState());
                        return result;
                    });
                }

                webviewConfigurator.registerActionHandler(pageId, page::handleAction);

                // Default stream for the page.
                // For the Log page, when the central logging service is
                // available, build state from ILogStreamService so we can
                // filter by level/feature and keep a bounded list.
                if ("log".equalsIgnoreCase(pageName) && logStreamService != null) {
                    webviewConfigurator.registerStreamHandler(pageId, LOG_STREAM_ID,
                            params -> {
                                String machineFullName = services.machineFullName;
                                String level = stringOrDefault(params.get("level"), "ALL");
                                String feature = stringOrDefault(params.get("feature"), "ALL");
                                int maxEntries = intOrDefault(params.get("maxEntries"), 500);

                                // Initial snapshot
                                java.util.List<java.util.Map<String, Object>> initial =
                                        logStreamService.getRecentEntries(machineFullName, level, feature, maxEntries);

                                java.util.Map<String, Object> initialState = buildLogState(initial, maxEntries, level,
                                        feature);

                                // Live updates: accumulate into a bounded list
                                return reactor.core.publisher.Flux
                                        .concat(
                                                reactor.core.publisher.Flux.just(initialState),
                                                logStreamService
                                                        .getStream(machineFullName, level, feature, maxEntries)
                                                        .scan(initialState, (state, entry) -> {
                                                            @SuppressWarnings("unchecked")
                                                            java.util.List<java.util.Map<String, Object>> events =
                                                                    (java.util.List<java.util.Map<String, Object>>) state
                                                                            .getOrDefault("events",
                                                                                    java.util.Collections.emptyList());
                                                            events = new java.util.ArrayList<>(events);
                                                            events.add(0, entry);
                                                            while (events.size() > maxEntries) {
                                                                events.remove(events.size() - 1);
                                                            }
                                                            return buildLogState(events, maxEntries, level, feature);
                                                        }))
                                        ;
                            },
                            LOG_STREAM_ID);
                } else {
                    webviewConfigurator.registerStreamHandler(pageId, pageName,
                            (params) -> page.streamData(pageName, params),
                            pageName);
                }

                // Packet Sniffer: register streams and state push from bundle
                if ("PacketSniffer".equalsIgnoreCase(pageName) && packetSnifferRegistry != null) {
                    IPacketSnifferPage sniffer = packetSnifferRegistry.getSniffer(services.machineFullName);
                    if (sniffer != null) {
                        webviewConfigurator.registerStreamHandler(pageId, "packets", sniffer::streamPackets, "trafficPackets");
                        webviewConfigurator.registerStreamHandler(pageId, "statistics", sniffer::streamStatistics, "statistics");
                        sniffer.addStateChangeListener(state -> webviewConfigurator.sendEvent(pageId + ".stateChanged", state));
                    }
                }
                if ("PacketAnalyzer".equalsIgnoreCase(pageName) && packetSnifferRegistry != null) {
                    IPacketSnifferPage sniffer = packetSnifferRegistry.getSniffer(services.machineFullName);
                    if (sniffer != null) {
                        sniffer.addStateChangeListener(state -> webviewConfigurator.sendEvent(pageId + ".stateChanged", state));
                    }
                }
            }

            // Register as EventHandler if needed
            if (bundleContext != null) {
                // Determine topics - for now, can we let the page define its topics?
                // Or just use a default? Connection and Training use specific topics.
                // Let's assume generic naming for now or add getTopics() to IScriptedPage
                Dictionary<String, Object> props = new Hashtable<>();
                if (pageName.equalsIgnoreCase("connection")) {
                    props.put(EventConstants.EVENT_TOPIC, new String[] {
                            "sokybot/network/" + services.machineFullName + "/Connected",
                            "sokybot/network/" + services.machineFullName + "/Disconnected"
                    });
                } else if (pageName.equalsIgnoreCase("training")) {
                    props.put(EventConstants.EVENT_TOPIC,
                            "sokybot/game/" + services.machineFullName + "/TrainerStuckEvent");
                }
                // Add more as needed or make it dynamic

                if (props.get(EventConstants.EVENT_TOPIC) != null) {
                    ServiceRegistration<EventHandler> reg = bundleContext.registerService(
                            EventHandler.class, (EventHandler) event -> {
                                // Since IScriptedPage doesn't implement EventHandler directly in the interface,
                                // we check if it can handle events. Or better, add handleEvent to
                                // IScriptedPage.
                                if (page instanceof EventHandler) {
                                    ((EventHandler) page).handleEvent(event);
                                }
                            }, props);
                    services.registerHandler(pageName, reg);
                }
            }
        });
    }

    /**
     * Container for all services for a single machine.
     */
    private class MachineServices {
        final String machineFullName;
        final IMachineContext machineContext;
        final Map<String, IScriptedPage> pages = new HashMap<>();
        final Map<String, ServiceRegistration<EventHandler>> handlerRegistrations = new HashMap<>();

        MachineServices(String machineFullName, IMachineContext machineContext) {
            this.machineFullName = machineFullName;
            this.machineContext = machineContext;
        }

        void addPage(String name, IScriptedPage page) {
            pages.put(name, page);
        }

        void registerHandler(String name, ServiceRegistration<EventHandler> reg) {
            handlerRegistrations.put(name, reg);
        }

        void unregisterPage(String name) {
            IScriptedPage page = pages.remove(name);
            if (page != null) {
                page.shutdown();
                if (webviewConfigurator != null) {
                    webviewConfigurator.removePage(name + "_" + machineFullName);
                }
            }
            ServiceRegistration<EventHandler> reg = handlerRegistrations.remove(name);
            if (reg != null) {
                reg.unregister();
            }
        }

        void shutdown() {
            new ArrayList<>(pages.keySet()).forEach(this::unregisterPage);
        }
    }

    private static Map<String, Object> buildLogState(java.util.List<java.util.Map<String, Object>> events,
                                                     int maxEntries,
                                                     String level,
                                                     String feature) {
        Map<String, Object> state = new HashMap<>();
        state.put("events", events);
        state.put("maxEvents", maxEntries);

        Map<String, Object> logState = new HashMap<>();
        logState.put("events", events);
        logState.put("maxEvents", maxEntries);
        logState.put("level", level);
        logState.put("feature", feature);
        state.put("Log", logState);

        return state;
    }

    private static String stringOrDefault(Object value, String def) {
        if (value instanceof String) {
            String s = (String) value;
            if (!s.isEmpty()) {
                return s;
            }
        }
        return def;
    }

    private static int intOrDefault(Object value, int def) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return def;
    }
}
