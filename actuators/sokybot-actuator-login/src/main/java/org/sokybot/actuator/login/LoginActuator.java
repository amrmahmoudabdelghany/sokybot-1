package org.sokybot.actuator.login;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.NetworkPeer;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.api.ISettingsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Actuator for auto-login functionality.
 * Manages login sequence after connection is established.
 */
@Component(service = IActuator.class, property = { "actuator.name=login" })
public class LoginActuator implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(LoginActuator.class);

    private ISettingsRegistry settingsRegistry;

    @Reference
    public void setSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public void initialize(IActuatorContext context) throws BundleException {
        log.info("Initializing login actuator for machine: {}", context.getMachineId());

        try {
            // Settings scope is now registered globally by LoginSettingsRegistrar

            // Get settings provider
            ISettingsProvider<LoginSettings> settingsProvider = settingsRegistry.getProvider(
                    context.getGroupName(),
                    context.getMachineName(),
                    "login",
                    LoginSettings.class);

            // Register login cycle
            ICycleDefinition cycle = new CycleDefinitionBuilder()
                    .name("login-cycle")
                    .priority(200) // Higher priority than connector - login after connection
                    .entryState("CHECK_LOGGED_IN")
                    .entryGuard(ctx -> {
                        // Only enter if connected and not logged in
                        LoginSettings settings = settingsProvider.get();
                        return ctx.getDispatcher().isConnected()
                                && settings.isAutoLogin()
                                && !isLoggedIn(ctx);
                    })
                    .state("CHECK_LOGGED_IN", builder -> builder
                            .guard(ctx -> !isLoggedIn(ctx))
                            .action(ctx -> {
                                log.info("Not logged in, starting login sequence");
                            })
                            .nextState("SEND_LOGIN_REQUEST")
                            .targetState(null)) // If already logged in, exit cycle
                    .state("SEND_LOGIN_REQUEST", builder -> builder
                            .guard(ctx -> {
                                // Check if credentials are available
                                LoginSettings settings = settingsProvider.get();
                                return settings.getUsername() != null
                                        && !settings.getUsername().isEmpty()
                                        && settings.getPassword() != null
                                        && !settings.getPassword().isEmpty();
                            })
                            .action(ctx -> {
                                LoginSettings settings = settingsProvider.get();
                                String username = settings.getUsername();
                                String password = settings.getPassword();
                                log.info("Sending login request for user: {}", username);

                                log.info("Sending login request for user: {}", username);

                                // Get locale from settings
                                byte locale = (byte) settings.getLocale();

                                // Parse agent ID from settings (if available)
                                short agentId = parseAgentId(settings.getTargetAgent());

                                // Create login packet
                                sendLoginPacket(ctx, username, password, locale, agentId);
                            })
                            .nextState("WAIT_FOR_LOGIN_RESPONSE")
                            .targetState("CHECK_LOGGED_IN")) // If guard fails, retry
                    .state("WAIT_FOR_LOGIN_RESPONSE", builder -> builder
                            .guard(ctx -> {
                                // Check if login successful (check game model for trainer)
                                return isLoggedIn(ctx);
                            })
                            .action(ctx -> {
                                log.info("Login successful");
                            })
                            .nextState(null) // Exit cycle when logged in
                            .targetState("CHECK_LOGGED_IN")) // If guard fails, retry
                    .build();

            context.getWorkflowRegistry().registerCycle(cycle);
            log.info("Login cycle registered successfully");

        } catch (Exception e) {
            log.error("Failed to initialize login actuator: {}", e.getMessage(), e);
            throw new BundleException("Failed to initialize login actuator: " + e.getMessage(), e);
        }
    }

    /**
     * Sends login request packet.
     */
    private void sendLoginPacket(IWorkflowContext context, String username, String password, byte locale,
            short agentId) {
        try {
            int packetLen = 7 + username.length() + password.length();

            MutablePacket loginPacket = MutablePacket.getBuilder(packetLen, ClientOpcode.LOGIN_REQUEST)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .put(locale)
                    .putShort((short) username.length())
                    .putBytes(username.getBytes())
                    .putShort((short) password.length())
                    .putBytes(password.getBytes())
                    .putShort(agentId)
                    .build();

            context.getDispatcher().sendToServer(loginPacket);
            log.debug("Sent login request packet for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to send login packet: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends authentication request packet (for agent server).
     */
    private void sendAuthPacket(IWorkflowContext context, String username, String password, int loginId, byte locale) {
        try {
            int packetLen = 15 + username.length() + password.length();

            MutablePacket authPacket = MutablePacket.getBuilder(packetLen, ClientOpcode.AUTH_REQUEST)
                    .packetEncoding(Encoding.ENCRYPTED)
                    .dataEncoding(Encoding.PLAIN)
                    .packetSource(NetworkPeer.BOT)
                    .putInt(loginId)
                    .putShort((short) username.length())
                    .putBytes(username.toLowerCase().getBytes())
                    .putShort((short) password.length())
                    .putBytes(password.getBytes())
                    .put(locale)
                    .putBytes(new byte[6]) // MAC address placeholder
                    .build();

            context.getDispatcher().sendToServer(authPacket);
            log.debug("Sent authentication packet for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to send auth packet: {}", e.getMessage(), e);
        }
    }

    /**
     * Parses agent ID from target agent string.
     */
    private short parseAgentId(String targetAgent) {
        if (targetAgent == null || targetAgent.isEmpty()) {
            return 0; // Default agent
        }
        try {
            return Short.parseShort(targetAgent);
        } catch (NumberFormatException e) {
            log.warn("Invalid agent ID format: {}", targetAgent);
            return 0;
        }
    }

    /**
     * Checks if the player is logged in.
     */
    private boolean isLoggedIn(IWorkflowContext context) {
        try {
            ITrainer trainer = context.getGameModel().getTrainer();
            // Trainer exists means logged in
            return trainer != null && trainer.getUniqueId() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void shutdown(IActuatorContext context) {
        log.info("Shutting down login actuator for machine: {}", context.getMachineId());
    }
}
