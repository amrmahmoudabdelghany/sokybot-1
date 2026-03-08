package org.sokybot.engine.api.extension;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.IPacketBuilder;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;

/**
 * Base class for Groovy actuator scripts.
 * Provides Logger, settings helpers (with idempotent registration),
 * and a packet construction DSL.
 *
 * <pre>
 * class Login extends BaseActuator {
 *     Login() { super("login") }
 *
 *     void setup() {
 *         registerSettings("login", LoginSettings, { new LoginSettings() })
 *         def provider = settingsProvider("login", LoginSettings)
 *         // register cycles...
 *     }
 * }
 * new Login()
 * </pre>
 */
public abstract class BaseActuator implements IActuator {

    protected final Logger log;
    protected final String name;
    protected IActuatorContext context;

    protected BaseActuator(String name) {
        this.name = name;
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final void initialize(IActuatorContext context) {
        this.context = context;
        log.info("Initializing {} actuator for machine: {}", name, context.getMachineId());
        try {
            setup();
        } catch (Exception e) {
            log.error("Failed to initialize {} actuator: {}", name, e.getMessage(), e);
        }
    }

    /**
     * Override to perform initialization: register settings, define cycles, etc.
     * Called after context is available.
     */
    protected abstract void setup();

    @Override
    public void shutdown(IActuatorContext context) {
        log.info("Shutting down {} actuator", name);
    }

    // ---- Settings helpers ----

    /**
     * Idempotent settings registration. Checks getRegisteredScopes() first,
     * fixing the static-boolean-per-class-per-eval bug.
     */
    protected <T> void registerSettings(String scope, Class<T> type, Supplier<T> factory) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("ISettingsRegistry not available for {} actuator", name);
            return;
        }
        if (!registry.getRegisteredScopes().contains(scope)) {
            registry.register(scope, type, factory);
            log.info("Registered settings scope '{}'", scope);
        }
    }

    /**
     * Get a settings provider for the given scope and type, scoped to this machine.
     */
    protected <T> ISettingsProvider<T> settingsProvider(String scope, Class<T> type) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("ISettingsRegistry not available for {} actuator", name);
            return null;
        }
        return registry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                scope, type);
    }

    // ---- Packet DSL ----

    /**
     * Build an encrypted bot packet with the standard encoding/source defaults.
     *
     * <pre>
     * def pkt = buildPacket(7, CHAR_ACTION) { it.put(action).putInt(targetId) }
     * </pre>
     */
    protected MutablePacket buildPacket(int capacity, int opcode, Consumer<IPacketBuilder> config) {
        IPacketBuilder builder = MutablePacket.getBuilder(capacity, opcode)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT);
        config.accept(builder);
        return builder.build();
    }

    /**
     * Build and immediately send a packet to the server.
     *
     * <pre>
     * sendToServer(7, CHAR_ACTION) { it.put(action).put((byte)0x01).putInt(targetId) }
     * </pre>
     */
    protected void sendToServer(int capacity, int opcode, Consumer<IPacketBuilder> config) {
        context.getDispatcher().sendToServer(buildPacket(capacity, opcode, config));
    }

    /**
     * Build and immediately send a packet to the client.
     */
    protected void sendToClient(int capacity, int opcode, Consumer<IPacketBuilder> config) {
        context.getDispatcher().sendToClient(buildPacket(capacity, opcode, config));
    }

    /**
     * Shorthand for getting the dispatcher.
     */
    protected IDispatcher dispatcher() {
        return context.getDispatcher();
    }
}
