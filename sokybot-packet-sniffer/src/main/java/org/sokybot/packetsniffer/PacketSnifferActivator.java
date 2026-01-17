package org.sokybot.packetsniffer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.IGroupListener;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.IMachineListener;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.webview.api.IWebviewConfigurator;
import org.sokybot.webview.api.util.SchemaLoader;
import org.sokybot.packetsniffer.storage.JsonPacketStorage;

/**
 * Packet Sniffer Bundle Activator
 * Refactored to use declarative UI system
 */
@Component(immediate = true)
public class PacketSnifferActivator implements IGroupListener, IMachineListener {

    @Reference
    private ISokybotContext appCtx;
    
    @Reference
    private IWebviewConfigurator webviewConfigurator;

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
        
        if (appCtx != null && appCtx.isRunning()) {
            installPacketSniffer(appCtx);
        }
    }

    @Deactivate
    public void deactivate() {
        System.out.println("Stopping PacketSniffer Bundle");
        if (appCtx != null) {
            uninstallPacketSniffer(appCtx);
        }
        
        // Shutdown services
        for (PacketSnifferService service : services.values()) {
            service.shutdown();
        }
        services.clear();
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

    @Override
    public void onGroupInstalled(IGroupContext groupCtx) {
        installPacketSniffer(groupCtx);
    }

    @Override
    public void onGroupUninstalled(IGroupContext groupCtx) {
        uninstallPacketSniffer(groupCtx);
    }

    @Override
    public void onMachineInstalled(IMachineContext machineCtx) {
        installPacketSniffer(machineCtx);
    }

    @Override
    public void onMachineUninstalled(IMachineContext machineCtx) {
        uninstallPacketSniffer(machineCtx);
    }

    private void installPacketSniffer(ISokybotContext ctx) {
        if (ctx.isRunning()) {
            Stream.of(ctx.getGroups())
                .filter(IGroupContext::isRunning)
                .forEach(this::installPacketSniffer);
            ctx.addGroupListener(this);
        }
    }

    private void uninstallPacketSniffer(ISokybotContext ctx) {
        Stream.of(ctx.getGroups())
            .filter(IGroupContext::isRunning)
            .forEach(this::uninstallPacketSniffer);
        ctx.removeGroupListener(this);
    }

    private void installPacketSniffer(IGroupContext ctx) {
        Stream.of(ctx.getMachines())
            .filter(IMachineContext::isRunning)
            .forEach(this::installPacketSniffer);
        ctx.addMachineListener(this);
    }

    private void uninstallPacketSniffer(IGroupContext ctx) {
        Stream.of(ctx.getMachines())
            .filter(IMachineContext::isRunning)
            .forEach(this::uninstallPacketSniffer);
        ctx.removeMachineListener(this);
    }

    private void installPacketSniffer(IMachineContext ctx) {
        String machineName = ctx.fullName();
        System.out.println("Install Packet Sniffer On Machine: " + machineName);
        
        // Create service for this machine
        PacketSnifferService service = new PacketSnifferService(storage, machineName);
        services.put(machineName, service);
        
        // Subscribe to packets
        PacketSnifferObserver observer = new PacketSnifferObserver(service, machineName);
        IPacketSubscription subscription = ctx.packetPublisher()
            .subscribe(observer, IPacketPublisher.ANY);
        subscriptions.put(machineName, subscription);
        
        // Register webview page with declarative schema
        registerPacketSnifferPage(machineName, service);
    }

    private void uninstallPacketSniffer(IMachineContext ctx) {
        String machineName = ctx.fullName();
        
        IPacketSubscription subscription = subscriptions.remove(machineName);
        if (subscription != null) {
            subscription.cancel();
        }
        
        PacketSnifferService service = services.remove(machineName);
        if (service != null) {
            service.shutdown();
        }
        
        // Remove page from webview
        webviewConfigurator.removePage("packetSniffer_" + machineName);
    }

    private void registerPacketSnifferPage(String machineName, PacketSnifferService service) {
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
