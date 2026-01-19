package org.sokybot.packetsniffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.webview.api.IWebviewConfigurator;
import org.sokybot.webview.api.util.SchemaLoader;
import org.sokybot.packetsniffer.storage.JsonPacketStorage;

/**
 * Packet Sniffer Bundle Activator
 * Uses EventAdmin to listen for machine context lifecycle events
 */
@Component(
    immediate = true,
    property = {
        "event.topics=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED,
        "event.topics=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED
    }
)
public class PacketSnifferActivator implements EventHandler {
    
    private IWebviewConfigurator webviewConfigurator;
    
    @Reference
    public void setWebviewConfigurator(IWebviewConfigurator webviewConfigurator) {
        this.webviewConfigurator = webviewConfigurator;
    }

    private final Map<String, PacketSnifferService> services = new HashMap<>();
    private final Map<String, IPacketSubscription> subscriptions = new HashMap<>();
    
    private static final JsonPacketStorage storage = new JsonPacketStorage("./packet-data.json");
    
    // Cache loaded UI schemas
    private Map<String, Object> mainSchema;
    private Map<String, Object> trafficMonitorSchema;
    private Map<String, Object> packetTracerSchema;
    private Map<String, Object> packetAnalyzerSchema;

    @Activate
    public void activate() {
        System.out.println("Starting PacketSniffer Bundle (Webview)");
        
        // Load UI schemas from resources
        loadUISchemas();
    }

    @Deactivate
    public void deactivate() {
        System.out.println("Stopping PacketSniffer Bundle");
        
        // Unsubscribe all subscriptions
        for (IPacketSubscription subscription : subscriptions.values()) {
            subscription.unsubscribe();
        }
        subscriptions.clear();
        
        // Shutdown all services
        for (PacketSnifferService service : services.values()) {
            service.shutdown();
        }
        services.clear();
    }

    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        
        if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED.equals(topic)) {
            IMachineContext machineContext = (IMachineContext) event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
            if (machineContext != null) {
                installPacketSniffer(machineContext);
            }
        } else if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED.equals(topic)) {
            String fullName = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
            if (fullName != null) {
                uninstallPacketSniffer(fullName);
            }
        }
    }

    private void loadUISchemas() {
        try {
            // Load main schema
            mainSchema = SchemaLoader.loadSchema("/ui/packet-sniffer.json", getClass());
            if (mainSchema == null) {
                System.err.println("Failed to load packet-sniffer.json");
                mainSchema = createFallbackSchema();
            }
            
            // Load traffic monitor schema
            trafficMonitorSchema = SchemaLoader.loadSchema("/ui/traffic-monitor.json", getClass());
            if (trafficMonitorSchema == null) {
                System.err.println("Failed to load traffic-monitor.json");
            }
            
            // Load packet tracer schema
            packetTracerSchema = SchemaLoader.loadSchema("/ui/packet-tracer.json", getClass());
            if (packetTracerSchema == null) {
                System.err.println("Failed to load packet-tracer.json");
            }
            
            System.out.println("Packet Sniffer: UI schemas loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load UI schemas: " + e.getMessage());
            e.printStackTrace();
            mainSchema = createFallbackSchema();
        }
    }
    
    private Map<String, Object> createFallbackSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "div");
        schema.put("className", "p-6");
        schema.put("props", Map.of(
            "children", "Packet Sniffer - UI schema failed to load"
        ));
        return schema;
    }

    private void installPacketSniffer(IMachineContext ctx) {
        String machineName = ctx.fullName();
        System.out.println("Install Packet Sniffer On Machine: " + machineName);
        
        // Get proxy connection and packet publisher
        IProxyConnection proxyConnection = ctx.getProxyConnection();
        if (proxyConnection == null) {
            System.err.println("No proxy connection available for " + machineName);
            return;
        }
        
        IPacketPublisher packetPublisher = proxyConnection.getPacketPublisher();
        if (packetPublisher == null) {
            System.err.println("No packet publisher available for " + machineName);
            return;
        }
        
        // Create service for this machine
        PacketSnifferService service = new PacketSnifferService(storage, machineName);
        services.put(machineName, service);
        
        // Subscribe to all packets
        PacketSnifferObserver observer = new PacketSnifferObserver(service, machineName);
        IPacketSubscription subscription = packetPublisher.subscribeAll(observer);
        subscriptions.put(machineName, subscription);
        
        // Register webview page with declarative schema
        registerPacketSnifferPage(machineName, service);
    }

    private void uninstallPacketSniffer(String machineName) {
        IPacketSubscription subscription = subscriptions.remove(machineName);
        if (subscription != null) {
            subscription.unsubscribe();
        }
        
        PacketSnifferService service = services.remove(machineName);
        if (service != null) {
            service.shutdown();
        }
        
        // Remove page from webview
        if (webviewConfigurator != null) {
            webviewConfigurator.removePage("packetSniffer_" + machineName);
        }
    }

    private void registerPacketSnifferPage(String machineName, PacketSnifferService service) {
        if (webviewConfigurator == null) {
            System.err.println("Webview configurator not available");
            return;
        }
        
        // Create a copy of the schema for this machine instance
        Map<String, Object> machineSchema = deepCopySchema(mainSchema);
        
        // Register the page
        webviewConfigurator.addDeclarativePage(
            "packetSniffer_" + machineName,
            "Packet Sniffer",
            "icons/network.svg",
            machineSchema
        );
        
        // Register schema handler for dynamic data
        webviewConfigurator.registerSchemaHandler(
            "packetSniffer_" + machineName,
            (request) -> service.handleSchemaRequest(request)
        );
        
        // Register action handlers
        webviewConfigurator.registerActionHandler(
            "packetSniffer_" + machineName,
            (action, data) -> service.handleAction(action, data)
        );
        
        // Register stream handlers
        webviewConfigurator.registerStreamHandler(
            "packetSniffer_" + machineName,
            "packets",
            service::streamPackets,
            "trafficPackets"
        );
        
        webviewConfigurator.registerStreamHandler(
            "packetSniffer_" + machineName,
            "statistics",
            service::streamStatistics,
            "statistics"
        );
        
        // Register state change listener to update UI
        service.addStateChangeListener((state) -> {
            webviewConfigurator.sendEvent(
                "packetSniffer_" + machineName + ".stateChanged",
                state
            );
        });
        
        // Register analyzer page
        if (packetAnalyzerSchema != null) {
            Map<String, Object> analyzerSchema = deepCopySchema(packetAnalyzerSchema);
            webviewConfigurator.addDeclarativePage(
                "packetAnalyzer_" + machineName,
                "Packet Analyzer",
                "icons/search.svg",
                analyzerSchema
            );
            
            // Register analyzer action handlers
            webviewConfigurator.registerActionHandler(
                "packetAnalyzer_" + machineName,
                (action, data) -> {
                    // Delegate to service's analyzer action handler
                    return service.handleAction("analyzerAction", Map.of(
                        "action", action,
                        "data", data != null ? data : new HashMap<>()
                    ));
                }
            );
            
            // Register analyzer schema handler
            webviewConfigurator.registerSchemaHandler(
                "packetAnalyzer_" + machineName,
                (request) -> {
                    if (service.getAnalyzerService() != null) {
                        return service.getAnalyzerService().getInitialState();
                    }
                    return Map.of("packets", new ArrayList<>(), "variables", new ArrayList<>());
                }
            );
        }
        
        System.out.println("Packet Sniffer: Page registered for " + machineName);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopySchema(Map<String, Object> original) {
        if (original == null) return new HashMap<>();
        try {
            // Simple deep copy using serialization
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(original);
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            System.err.println("Failed to deep copy schema: " + e.getMessage());
            return new HashMap<>(original);
        }
    }
}
