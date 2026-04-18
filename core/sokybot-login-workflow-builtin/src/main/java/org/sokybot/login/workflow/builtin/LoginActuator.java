package org.sokybot.login.workflow.builtin;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.login.IGatewayProtocolEmitter;
import org.sokybot.engine.api.login.ILoginFailureClassifier;
import org.sokybot.engine.api.login.ILoginInteractiveCoordinator;
import org.sokybot.engine.api.login.ILoginSettingsSnapshotter;
import org.sokybot.engine.api.login.LoginSettingsSnapshot;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the compiled {@code login-cycle} on each engine (replaces the former {@code Login.groovy} script).
 */
@Component(service = IActuator.class, immediate = true)
public final class LoginActuator implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(LoginActuator.class);

    @Reference
    private IGatewayProtocolEmitter gatewayProtocolEmitter;
    @Reference
    private ILoginSettingsSnapshotter loginSettingsSnapshotter;
    @Reference
    private ILoginFailureClassifier loginFailureClassifier;
    @Reference
    private ILoginInteractiveCoordinator loginInteractiveCoordinator;

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("Login: ISettingsRegistry not available; cycle will not be registered");
            return;
        }
        if (!registry.getRegisteredScopes().contains("login")) {
            registry.register("login", LoginSettings.class, LoginSettings::new);
        }
        ISettingsProvider<LoginSettings> settingsProvider = registry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                "login",
                LoginSettings.class);
        if (settingsProvider == null) {
            log.warn("Login: settings provider not available; cycle will not be registered");
            return;
        }

        LoginCycleDeps deps = new LoginCycleDeps(
                gatewayProtocolEmitter,
                loginSettingsSnapshotter,
                loginFailureClassifier,
                loginInteractiveCoordinator,
                settingsProvider);
        LoginWorkflowSupport support = new LoginWorkflowSupport(deps);
        ICycleDefinition cycle = new LoginCycleBuilder(support).build();
        context.getWorkflowRegistry().registerCycle(cycle);
        log.info("Login cycle registered successfully");
    }

    @Override
    public void shutdown(IActuatorContext context) {
        try {
            if (gatewayProtocolEmitter != null) {
                ActuatorShutdownBridge bridge = new ActuatorShutdownBridge(context);
                long timeout = LoginCycleKeys.LOGOUT_ACK_TIMEOUT_MS;
                try {
                    if (loginSettingsSnapshotter != null) {
                        LoginSettingsSnapshot s = loginSettingsSnapshotter.snapshot(bridge, false);
                        if (s != null) {
                            timeout = s.getLogoutAckTimeoutMs();
                        }
                    }
                } catch (Exception ignored) {
                }
                gatewayProtocolEmitter.gracefulShutdown(bridge, timeout);
            }
            Map<String, Object> sd = context.getSessionData();
            if (sd != null) {
                sd.remove(LoginCycleKeys.KEY_RETRY_UNTIL_MS);
                sd.remove(LoginCycleKeys.KEY_RETRY_ATTEMPT_ID);
                sd.remove(LoginCycleKeys.KEY_ATTEMPT_LOGIN_SETTINGS);
                sd.remove(LoginCycleKeys.KEY_INTERACTIVE_WAIT_UNTIL_MS);
                sd.remove(LoginCycleKeys.KEY_USER_RESUME_REQUIRED);
            }
        } catch (Exception ignored) {
        }
        context.getWorkflowRegistry().unregisterCycle(LoginCycleKeys.CYCLE_NAME);
        log.info("Shutting down login actuator");
    }
}
