package org.sokybot.machine.network;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.IMachineContext;
import org.sokybot.gameevents.GameEventPublisher;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.packet.ImmutablePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Bridges Spring (engine) and OSGi (game-events) by forwarding packets
 * from the machine's packet publisher to the GameEventPublisher service.
 * 
 * This component:
 * - Lives in the machine's Spring context (one per machine)
 * - Looks up OSGi GameEventPublisher service
 * - Subscribes to IPacketPublisher
 * - Forwards packets to game-events for translation
 * 
 * NOTE: Currently disabled until sokybot-game-events module is implemented.
 */
@Component
@Scope("prototype")  // One instance per machine
public class PacketDispatcher implements IPacketObserver {
    
    private static final Logger log = LoggerFactory.getLogger(PacketDispatcher.class);
    
    @Autowired
    private IMachineContext machineContext;
    
    @Autowired
    private BundleContext bundleContext;
    
    private GameEventPublisher eventPublisher;
    private ServiceReference<GameEventPublisher> serviceRef;
    
    @PostConstruct
    public void init() {
        try {
            // Get OSGi service reference to GameEventPublisher
            serviceRef = bundleContext.getServiceReference(GameEventPublisher.class);
            
            if (serviceRef != null) {
                eventPublisher = bundleContext.getService(serviceRef);
                
                // Subscribe to machine's packet publisher
                machineContext.packetPublisher().subscribe(this);
                
                log.info("PacketDispatcher initialized for machine: {}", 
                    machineContext.fullName());
            } else {
                log.warn("GameEventPublisher service not found! Events will not be published. This is normal during startup if sokybot-game-events bundle hasn't started yet.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize PacketDispatcher: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            // Unsubscribe from packet publisher
            if (machineContext.packetPublisher() != null) {
                machineContext.packetPublisher().unsubscribe(this);
            }
            
            // Release OSGi service reference
            if (serviceRef != null) {
                bundleContext.ungetService(serviceRef);
            }
            
            log.info("PacketDispatcher cleaned up for machine: {}", 
                machineContext.fullName());
        } catch (Exception e) {
            log.error("Error during PacketDispatcher cleanup: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onNext(int opcode, ImmutablePacket packet) {
        if (eventPublisher != null) {
            // Forward packet to game-events module for translation
            eventPublisher.onPacketReceived(machineContext.fullName(), packet);
        } else {
            // GameEventPublisher not available yet - this is normal during startup
            // The service might not be registered yet if sokybot-game-events bundle hasn't started
            log.debug("GameEventPublisher not available, dropping packet opcode 0x{}", 
                Integer.toHexString(opcode).toUpperCase());
        }
    }
}
