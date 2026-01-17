package org.sokybot.actuator.connector;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.NetworkPeer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actuator for auto-connect functionality.
 * Manages connection to game server and agent discovery.
 */
@Component(service = IActuator.class, property = {"actuator.name=connector"})
public class ConnectorActuator implements IActuator {
    
    private static final Logger log = LoggerFactory.getLogger(ConnectorActuator.class);
    
    @Override
    public String getName() {
        return "connector";
    }
    
    @Override
    public void initialize(IActuatorContext context) throws BundleException {
        log.info("Initializing connector actuator for machine: {}", context.getMachineId());
        
        try {
            // Register connector cycle
            ICycleDefinition cycle = new CycleDefinitionBuilder()
                .name("connector-cycle")
                .priority(100) // High priority - connection is fundamental
                .entryState("CHECK_CONNECTION")
                .entryGuard(ctx -> {
                    // Only enter if not connected and settings are available
                    return !ctx.getDispatcher().isConnected() 
                        && ctx.getSettings().getTargetGateway() != null
                        && !ctx.getSettings().getTargetGateway().isEmpty();
                })
                .state("CHECK_CONNECTION", builder -> builder
                    .guard(ctx -> {
                        // Check if not connected
                        return !ctx.getDispatcher().isConnected();
                    })
                    .action(ctx -> {
                        log.info("Connection check: not connected, attempting to connect");
                    })
                    .nextState("CONNECT_TO_SERVER")
                    .targetState(null)) // If already connected, exit cycle
                .state("CONNECT_TO_SERVER", builder -> builder
                    .guard(ctx -> {
                        // Check if settings have connection info
                        String gateway = ctx.getSettings().getTargetGateway();
                        return gateway != null && !gateway.isEmpty();
                    })
                    .action(ctx -> {
                        String gateway = ctx.getSettings().getTargetGateway();
                        log.info("Connecting to gateway: {}", gateway);
                        // Parse gateway (format: host:port)
                        String[] parts = gateway.split(":");
                        if (parts.length == 2) {
                            String host = parts[0];
                            int port = Integer.parseInt(parts[1]);
                            
                            // Connection is handled by proxy connection
                            // This actuator just triggers the discovery process
                            // Send agent request packet to discover available agents
                            sendAgentRequest(ctx);
                        } else {
                            log.warn("Invalid gateway format: {}", gateway);
                        }
                    })
                    .nextState("WAIT_FOR_CONNECTION")
                    .targetState("CHECK_CONNECTION")) // If guard fails, retry
                .state("WAIT_FOR_CONNECTION", builder -> builder
                    .guard(ctx -> {
                        // Check if connection established
                        return ctx.getDispatcher().isConnected();
                    })
                    .action(ctx -> {
                        log.info("Connected successfully");
                    })
                    .nextState(null) // Exit cycle when connected
                    .targetState("CHECK_CONNECTION")) // If guard fails, retry
                .build();
            
            context.getWorkflowRegistry().registerCycle(cycle);
            log.info("Connector cycle registered successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize connector actuator: {}", e.getMessage(), e);
            throw new BundleException("Failed to initialize connector actuator: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sends agent request packet to discover available agents.
     */
    private void sendAgentRequest(IWorkflowContext context) {
        try {
            MutablePacket agentRequest = MutablePacket.getBuilder(0, ClientOpcode.AGENT_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
            
            context.getDispatcher().sendToServer(agentRequest);
            log.debug("Sent agent request packet");
        } catch (Exception e) {
            log.error("Failed to send agent request: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void shutdown(IActuatorContext context) {
        log.info("Shutting down connector actuator for machine: {}", context.getMachineId());
    }
}
