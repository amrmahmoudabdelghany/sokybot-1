import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder
import org.sokybot.network.packet.ClientOpcode
import org.sokybot.network.packet.Encoding
import org.sokybot.network.packet.MutablePacket
import org.sokybot.network.NetworkPeer
import org.sokybot.settings.api.ISettingsRegistry
import org.sokybot.settings.api.ISettingsProvider
import org.sokybot.actuator.login.LoginSettings
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Groovy implementation of ConnectorActuator.
 */
class Connector implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(Connector.class)

    @Override
    String getName() { "connector" }

    @Override
    void initialize(IActuatorContext context) {
        log.info("Initializing GROOVY connector actuator for machine: {}", context.getMachineId())

        try {
            // Lookup ISettingsRegistry service using the new context method
            def settingsRegistry = context.getService(ISettingsRegistry.class)
            
            if (settingsRegistry == null) {
                log.error("Failed to acquire ISettingsRegistry service!")
                return
            }

            // Get settings provider
            def settingsProvider = settingsRegistry.getProvider(
                    context.getGroupName(),
                    context.getMachineName(),
                    "login",
                    LoginSettings.class)

            // Register connector cycle
            def cycle = new CycleDefinitionBuilder()
                    .name("connector-cycle")
                    .priority(100)
                    .entryState("CHECK_CONNECTION")
                    .entryGuard({ ctx ->
                        def settings = settingsProvider.get()
                        return !ctx.getDispatcher().isConnected() &&
                                settings.getTargetGateway() != null &&
                                !settings.getTargetGateway().isEmpty()
                    })
                    .state("CHECK_CONNECTION", { builder -> builder
                            .guard({ ctx -> !ctx.getDispatcher().isConnected() })
                            .action({ ctx -> 
                                log.info("Connection check: not connected, attempting to connect") 
                            })
                            .nextState("CONNECT_TO_SERVER")
                            .targetState(null) // Exit if connected
                    })
                    .state("CONNECT_TO_SERVER", { builder -> builder
                            .guard({ ctx ->
                                def settings = settingsProvider.get()
                                return settings.getTargetGateway() != null && !settings.getTargetGateway().isEmpty()
                            })
                            .action({ ctx ->
                                def settings = settingsProvider.get()
                                String gateway = settings.getTargetGateway()
                                log.info("Connecting to gateway: {}", gateway)
                                
                                String[] parts = gateway.split(":")
                                if (parts.length == 2) {
                                    String host = parts[0]
                                    int port = Integer.parseInt(parts[1])
                                    ctx.getDispatcher().connect(host, port)
                                } else {
                                    log.warn("Invalid gateway format: {}", gateway)
                                }
                            })
                            .nextState("WAIT_FOR_CONNECTION")
                            .targetState("CHECK_CONNECTION")
                    })
                    .state("WAIT_FOR_CONNECTION", { builder -> builder
                            .guard({ ctx -> ctx.getDispatcher().isConnected() })
                            .action({ ctx ->
                                log.info("Connected successfully")
                                sendAgentRequest(ctx)
                            })
                            .nextState(null)
                            .targetState("CHECK_CONNECTION")
                    })
                    .build()

            context.getWorkflowRegistry().registerCycle(cycle)
            log.info("Connector cycle registered successfully (Groovy)")

        } catch (Exception e) {
            log.error("Failed to initialize connector actuator: {}", e.getMessage(), e)
        }
    }

    private void sendAgentRequest(def context) {
        try {
            def agentRequest = MutablePacket.getBuilder(0, ClientOpcode.AGENT_REQUEST)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .build()

            context.getDispatcher().sendToServer(agentRequest)
            log.debug("Sent agent request packet")
        } catch (Exception e) {
            log.error("Failed to send agent request: {}", e.getMessage(), e)
        }
    }

    @Override
    void shutdown(IActuatorContext context) {
        log.info("Shutting down Groovy connector actuator")
    }
}

// Return instance
new Connector()
