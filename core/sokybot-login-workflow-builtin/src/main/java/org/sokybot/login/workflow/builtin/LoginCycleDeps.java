package org.sokybot.login.workflow.builtin;

import java.util.Objects;

import org.sokybot.engine.api.login.IGatewayProtocolEmitter;
import org.sokybot.engine.api.login.ILoginFailureClassifier;
import org.sokybot.engine.api.login.ILoginInteractiveCoordinator;
import org.sokybot.engine.api.login.ILoginSettingsSnapshotter;
import org.sokybot.settings.api.ISettingsProvider;

/**
 * Injected collaborators and per-machine settings provider passed into login workflow logic.
 */
public final class LoginCycleDeps {

    private final IGatewayProtocolEmitter gatewayProtocolEmitter;
    private final ILoginSettingsSnapshotter loginSettingsSnapshotter;
    private final ILoginFailureClassifier loginFailureClassifier;
    private final ILoginInteractiveCoordinator loginInteractiveCoordinator;
    private final ISettingsProvider<LoginSettings> loginSettingsProvider;
    private final org.sokybot.engine.api.IEngineControl engineControl;

    public LoginCycleDeps(
            IGatewayProtocolEmitter gatewayProtocolEmitter,
            ILoginSettingsSnapshotter loginSettingsSnapshotter,
            ILoginFailureClassifier loginFailureClassifier,
            ILoginInteractiveCoordinator loginInteractiveCoordinator,
            ISettingsProvider<LoginSettings> loginSettingsProvider,
            org.sokybot.engine.api.IEngineControl engineControl
    ) {
        this.gatewayProtocolEmitter = Objects.requireNonNull(gatewayProtocolEmitter, "gatewayProtocolEmitter");
        this.loginSettingsSnapshotter = Objects.requireNonNull(loginSettingsSnapshotter, "loginSettingsSnapshotter");
        this.loginFailureClassifier = Objects.requireNonNull(loginFailureClassifier, "loginFailureClassifier");
        this.loginInteractiveCoordinator = Objects.requireNonNull(loginInteractiveCoordinator, "loginInteractiveCoordinator");
        this.loginSettingsProvider = Objects.requireNonNull(loginSettingsProvider, "loginSettingsProvider");
        this.engineControl = engineControl;
    }

    public org.sokybot.engine.api.IEngineControl engineControl() {
        return engineControl;
    }

    public IGatewayProtocolEmitter gatewayProtocolEmitter() {
        return gatewayProtocolEmitter;
    }

    public ILoginSettingsSnapshotter loginSettingsSnapshotter() {
        return loginSettingsSnapshotter;
    }

    public ILoginFailureClassifier loginFailureClassifier() {
        return loginFailureClassifier;
    }

    public ILoginInteractiveCoordinator loginInteractiveCoordinator() {
        return loginInteractiveCoordinator;
    }

    public ISettingsProvider<LoginSettings> loginSettingsProvider() {
        return loginSettingsProvider;
    }
}
