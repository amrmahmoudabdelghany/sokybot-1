import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder
import org.sokybot.settings.api.ISettingsRegistry
import org.sokybot.actuator.login.LoginSettings
import org.sokybot.network.packet.ClientOpcode
import org.sokybot.network.packet.Encoding
import org.sokybot.network.packet.MutablePacket
import org.sokybot.network.NetworkPeer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Login implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(Login.class)

    @Override
    String getName() { "login" }

    @Override
    void initialize(IActuatorContext context) {
        log.info("Initializing GROOVY login actuator for machine: {}", context.getMachineId())

        try {
            def settingsRegistry = context.getService(ISettingsRegistry.class)
            
            if (settingsRegistry == null) {
                log.error("Failed to acquire ISettingsRegistry service for Login actuator!")
                return
            }

            def settingsProvider = settingsRegistry.getProvider(
                    context.getGroupName(),
                    context.getMachineName(),
                    "login",
                    LoginSettings.class)

            def cycle = new CycleDefinitionBuilder()
                    .name("login-cycle")
                    .priority(200)
                    .entryState("CHECK_LOGGED_IN")
                    .entryGuard({ ctx ->
                        def settings = settingsProvider.get()
                        return ctx.getDispatcher().isConnected() &&
                                settings.isAutoLogin() &&
                                !isLoggedIn(ctx)
                    })
                    .state("CHECK_LOGGED_IN", { builder -> builder
                            .guard({ ctx -> !isLoggedIn(ctx) })
                            .action({ ctx ->
                                log.info("Not logged in, starting login sequence")
                            })
                            .nextState("SEND_LOGIN_REQUEST")
                            .targetState(null)
                    })
                    .state("SEND_LOGIN_REQUEST", { builder -> builder
                            .guard({ ctx ->
                                def settings = settingsProvider.get()
                                return settings.getUsername() != null &&
                                        !settings.getUsername().isEmpty() &&
                                        settings.getPassword() != null &&
                                        !settings.getPassword().isEmpty()
                            })
                            .action({ ctx ->
                                def settings = settingsProvider.get()
                                String username = settings.getUsername()
                                String password = settings.getPassword()
                                
                                log.info("Sending login request for user: {}", username)

                                byte locale = (byte) settings.getLocale()
                                short agentId = parseAgentId(settings.getTargetAgent())

                                sendLoginPacket(ctx, username, password, locale, agentId)
                            })
                            .nextState("WAIT_FOR_LOGIN_RESPONSE")
                            .targetState("CHECK_LOGGED_IN")
                    })
                    .state("WAIT_FOR_LOGIN_RESPONSE", { builder -> builder
                            .guard({ ctx -> isLoggedIn(ctx) })
                            .action({ ctx ->
                                log.info("Login successful (Groovy)")
                            })
                            .nextState(null)
                            .targetState("CHECK_LOGGED_IN")
                    })
                    .build()

            context.getWorkflowRegistry().registerCycle(cycle)
            log.info("Login cycle registered successfully (Groovy)")

        } catch (Exception e) {
            log.error("Failed to initialize login actuator: {}", e.getMessage(), e)
        }
    }

    @Override
    void shutdown(IActuatorContext context) {
        log.info("Shutting down Groovy login actuator")
    }

    private boolean isLoggedIn(def context) {
        try {
            def trainer = context.getGameModel().getTrainer()
            return trainer != null && trainer.getUniqueId() > 0
        } catch (Exception e) {
            return false
        }
    }

    private short parseAgentId(String targetAgent) {
        if (targetAgent == null || targetAgent.isEmpty()) {
            return (short) 0
        }
        try {
            return Short.parseShort(targetAgent)
        } catch (NumberFormatException e) {
            log.warn("Invalid agent ID format: {}", targetAgent)
            return (short) 0
        }
    }

    private void sendLoginPacket(def context, String username, String password, byte locale, short agentId) {
        try {
            int packetLen = 7 + username.length() + password.length()

            def loginPacket = MutablePacket.getBuilder(packetLen, ClientOpcode.LOGIN_REQUEST)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .put(locale)
                    .putShort((short) username.length())
                    .putBytes(username.getBytes())
                    .putShort((short) password.length())
                    .putBytes(password.getBytes())
                    .putShort(agentId)
                    .build()

            context.getDispatcher().sendToServer(loginPacket)
            log.debug("Sent login request packet for user: {}", username)
        } catch (Exception e) {
            log.error("Failed to send login packet: {}", e.getMessage(), e)
        }
    }
}

new Login()
