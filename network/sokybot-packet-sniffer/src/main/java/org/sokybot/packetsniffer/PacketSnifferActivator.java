package org.sokybot.packetsniffer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.packetsniffer.api.IPacketSnifferRegistry;
import org.sokybot.packetsniffer.storage.JsonPacketStorage;
import org.sokybot.packetsniffer.storage.StructDefinitionStorage;
import org.sokybot.proxy.recording.IPacketRecorder;

/**
 * Packet Sniffer Bundle Activator.
 * Listens for machine lifecycle events, creates PacketSnifferService per machine,
 * subscribes to packets, and registers IPacketSnifferRegistry so scripted pages can obtain the sniffer.
 * Does not register any webview pages; those are registered by MachinePagesActivator from scripts/pages.
 */
@Component(immediate = true, service = EventHandler.class, property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED,
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED
})
public class PacketSnifferActivator implements EventHandler {

    private static final JsonPacketStorage storage = new JsonPacketStorage("./packet-data.json");
    private static final StructDefinitionStorage structStorage = new StructDefinitionStorage("./struct-definitions.json");

    private ISokybotContext sokybotContext;
    private final PacketSnifferRegistryImpl registry = new PacketSnifferRegistryImpl();
    private final java.util.Map<String, PacketSnifferService> services = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, IPacketSubscription> subscriptions = new java.util.concurrent.ConcurrentHashMap<>();

    private BundleContext bundleContext;
    private ServiceRegistration<IPacketSnifferRegistry> registryRegistration;
    private volatile IPacketRecorder packetRecorder;

    @Reference
    public void setSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
    }

    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL, policy = org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC)
    public void setPacketRecorder(IPacketRecorder packetRecorder) {
        this.packetRecorder = packetRecorder;
    }

    public void unsetPacketRecorder(IPacketRecorder packetRecorder) {
        if (this.packetRecorder == packetRecorder) {
            this.packetRecorder = null;
        }
    }

    @Activate
    public void activate(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
        registryRegistration = bundleContext.registerService(IPacketSnifferRegistry.class, registry, null);
        System.out.println("Starting PacketSniffer Bundle (declarative pages)");

        if (sokybotContext != null) {
            for (IGroupContext group : sokybotContext.getGroups()) {
                for (IMachineContext machine : group.getMachines()) {
                    try {
                        installPacketSniffer(machine);
                    } catch (Exception e) {
                        System.err.println("Failed to install packet sniffer on existing machine " + machine.fullName()
                                + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Deactivate
    public void deactivate() {
        System.out.println("Stopping PacketSniffer Bundle");

        if (registryRegistration != null) {
            registryRegistration.unregister();
            registryRegistration = null;
        }

        for (IPacketSubscription subscription : subscriptions.values()) {
            subscription.unsubscribe();
        }
        subscriptions.clear();

        for (PacketSnifferService service : services.values()) {
            service.shutdown();
        }
        services.clear();
    }

    @Override
    public void handleEvent(Event event) {
        try {
            String topic = event.getTopic();

            if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED.equals(topic)) {
                IMachineContext ctx = (IMachineContext) event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
                if (ctx != null) {
                    installPacketSniffer(ctx);
                }
            } else if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED.equals(topic)) {
                String fullName = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
                if (fullName != null) {
                    uninstallPacketSniffer(fullName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling Packet Sniffer event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void installPacketSniffer(IMachineContext ctx) {
        String machineName = ctx.fullName();
        System.out.println("Install Packet Sniffer On Machine: " + machineName);

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

        PacketSnifferService service = new PacketSnifferService(storage, machineName, proxyConnection, packetRecorder,
                structStorage);
        services.put(machineName, service);
        registry.put(machineName, service);

        PacketSnifferObserver observer = new PacketSnifferObserver(service, machineName, packetRecorder);
        IPacketSubscription subscription = packetPublisher.subscribeAll(observer);
        subscriptions.put(machineName, subscription);

        System.out.println("Packet Sniffer: service registered for " + machineName);
    }

    private void uninstallPacketSniffer(String machineName) {
        IPacketSubscription subscription = subscriptions.remove(machineName);
        if (subscription != null) {
            subscription.unsubscribe();
        }

        registry.remove(machineName);
        PacketSnifferService service = services.remove(machineName);
        if (service != null) {
            service.shutdown();
        }
    }
}
