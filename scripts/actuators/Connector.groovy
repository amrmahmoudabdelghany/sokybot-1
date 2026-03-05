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
        
        // FOR TESTING: Trigger immediate connection
        log.info("TESTING: Triggering immediate connection for machine {}", context.getMachineId())
        try {
             context.getDispatcher().setClientlessMode(true)
             context.getDispatcher().connect("skrillax-gateway", 15779)
        } catch (Exception e) {
             log.error("Failed to trigger immediate connection: {}", e.getMessage())
        }

        try {
            // Lookup ISettingsRegistry service using the new context method
            def settingsRegistry = context.getService(ISettingsRegistry.class)
            
            if (settingsRegistry == null) {
                log.warn("ISettingsRegistry service not available yet, proceeding with defaults.")
            }

            // Get settings provider
            def settingsProvider = settingsRegistry.getProvider(
                    context.getGroupName(),
                    context.getMachineName(),
                    "login",
                    Map.class)

            // HARDCODED gateway for testing packet sniffer
            String testHost = "skrillax-gateway"
            int testPort = 15779

            // Register connector cycle
            def cycle = new CycleDefinitionBuilder()
                    .name("connector-cycle")
                    .priority(1000) // Must be higher than training-cycle (500) to avoid interruption
                    .entryState("CHECK_CONNECTION")
                    .entryGuard({ ctx ->
                        return !ctx.getDispatcher().isServerConnected()
                    })
                    .state("CHECK_CONNECTION", { builder -> builder
                            .guard({ ctx -> !ctx.getDispatcher().isServerConnected() })
                            .action({ ctx -> 
                                log.info("Connection check: not connected to server, attempting to connect") 
                            })
                            .nextState("CONNECT_TO_SERVER")
                    })
                    .state("CONNECT_TO_SERVER", { builder -> builder
                            .guard({ ctx -> true })
                            .action({ ctx ->
                                log.info("Connecting to hardcoded gateway {}:{}", testHost, testPort)
                                ctx.getDispatcher().setClientlessMode(true)
                                ctx.getDispatcher().connect(testHost, testPort)
                            })
                            .nextState("WAIT_FOR_CONNECTION")
                            .targetState("CHECK_CONNECTION")
                    })
                    .state("WAIT_FOR_CONNECTION", { builder -> builder
                            .guard({ ctx -> ctx.getDispatcher().isServerConnected() })
                            .action({ ctx ->
                                log.info("Connected successfully to server")
                                sendAgentRequest(ctx)
                            })
                            .nextState(null)
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
            log.info("Sent agent request packet")
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
