package org.sokybot.login.security.builtin;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.login.IGatewayProtocolEmitter;
import org.sokybot.engine.api.login.ILoginInteractiveCoordinator;
import org.sokybot.engine.api.login.InteractiveOutcome;
import org.sokybot.engine.api.login.LoginSettingsSnapshot;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.LoginState;

@Component(service = ILoginInteractiveCoordinator.class, immediate = true)
public final class LoginInteractiveCoordinatorImpl implements ILoginInteractiveCoordinator {

    @Reference
    private IGatewayProtocolEmitter gatewayProtocolEmitter;

    private final InteractiveDeadlineKeeper deadlineKeeper = new InteractiveDeadlineKeeper();
    private final InteractiveStateInspector stateInspector = new InteractiveStateInspector();
    private final LoginSafetyDisconnect safetyDisconnect = new LoginSafetyDisconnect();

    @Override
    public InteractiveOutcome handle(IWorkflowContext context, LoginSettingsSnapshot settingsSnapshot) {
        if (context == null || context.getGameModel() == null) {
            return InteractiveOutcome.NONE;
        }
        LoginState loginState = context.getGameModel().getLoginState();
        if (loginState == null) {
            return InteractiveOutcome.NONE;
        }

        if (stateInspector.isMissingPrereqState(loginState)) {
            deadlineKeeper.markUserResumeRequired(context);
            deadlineKeeper.clear(context);
            return InteractiveOutcome.WAITING_FOR_USER_RESUME;
        }

        if (stateInspector.isQueueState(loginState)) {
            if (!safetyDisconnect.isServerConnected(context)) {
                loginState.setFailureReason("Queue connection dropped");
                return InteractiveOutcome.TIMED_OUT_DISCONNECT;
            }
            return InteractiveOutcome.IN_QUEUE;
        }

        if (!stateInspector.isPasscodeOrCaptchaRequired(loginState)) {
            deadlineKeeper.clear(context);
            return InteractiveOutcome.NONE;
        }

        if (!safetyDisconnect.isServerConnected(context)) {
            loginState.setFailureReason("Connection dropped while waiting for passcode");
            deadlineKeeper.clear(context);
            return InteractiveOutcome.TIMED_OUT_DISCONNECT;
        }

        if (deadlineKeeper.isTimedOut(context)) {
            loginState.setFailureReason("Passcode/Captcha input timeout");
            deadlineKeeper.clear(context);
            safetyDisconnect.safeDisconnect(context);
            return InteractiveOutcome.TIMED_OUT_DISCONNECT;
        }

        if (loginState.getPhase() == LoginState.Phase.WAIT_FOR_CAPTCHA) {
            String answer = settingsSnapshot != null ? safe(settingsSnapshot.getPasscode()) : "";
            gatewayProtocolEmitter.sendGatewayImageCodeAnswer(context, answer);
            loginState.setPhase(LoginState.Phase.PASSCODE_SUBMITTED);
            loginState.setFailureReason(null);
            deadlineKeeper.ensure(context, timeout(settingsSnapshot));
            return InteractiveOutcome.WAITING_FOR_CAPTCHA;
        }

        deadlineKeeper.ensure(context, timeout(settingsSnapshot));
        return InteractiveOutcome.WAITING_FOR_PASSCODE;
    }

    private static long timeout(LoginSettingsSnapshot settingsSnapshot) {
        if (settingsSnapshot == null) {
            return 60000L;
        }
        return Math.max(5000L, settingsSnapshot.getPasscodeUserInputTimeoutMs());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
