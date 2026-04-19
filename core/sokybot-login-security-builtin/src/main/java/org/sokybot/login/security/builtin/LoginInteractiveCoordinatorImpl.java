package org.sokybot.login.security.builtin;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.engine.api.login.IGatewayProtocolEmitter;
import org.sokybot.engine.api.login.ILoginInteractiveCoordinator;
import org.sokybot.engine.api.login.InteractiveOutcome;
import org.sokybot.engine.api.login.LoginSettingsSnapshot;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.LoginState;
import org.sokybot.session.api.CaptchaPrompt;
import org.sokybot.session.api.ICaptchaContext;
import org.sokybot.session.api.ICaptchaSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ILoginInteractiveCoordinator.class, immediate = true)
public final class LoginInteractiveCoordinatorImpl implements ILoginInteractiveCoordinator {

    private static final Logger log = LoggerFactory.getLogger(LoginInteractiveCoordinatorImpl.class);

    @Reference
    private IGatewayProtocolEmitter gatewayProtocolEmitter;

    private final List<ICaptchaSolver> solvers = new CopyOnWriteArrayList<>();

    @Reference(service = ICaptchaSolver.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void bindCaptchaSolver(ICaptchaSolver solver) {
        solvers.add(solver);
    }

    void unbindCaptchaSolver(ICaptchaSolver solver) {
        solvers.remove(solver);
    }

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
            String answer = null;
            byte[] captchaData = context.getPersistentData().containsKey("captchaImage") 
                    ? (byte[]) context.getPersistentData().get("captchaImage") 
                    : new byte[0];
            String hint = captchaData.length > 0 ? "IMAGE" : "PASSCODE";
            CaptchaPrompt prompt = new CaptchaPrompt(0, captchaData, System.currentTimeMillis(), hint);
            ICaptchaContext captchaCtx = new ICaptchaContext() {
                @Override public String getMachineId() { return context.getMachineId(); }
                @Override public LoginSettingsSnapshot getLoginSettingsSnapshot() { return settingsSnapshot; }
                @Override public long getDeadlineEpochMs() { return System.currentTimeMillis() + timeout(settingsSnapshot); }
            };

            List<ICaptchaSolver> activeSolvers = new java.util.ArrayList<>(this.solvers);
            activeSolvers.sort(Comparator.comparingInt(ICaptchaSolver::getRanking).reversed());

            for (ICaptchaSolver solver : activeSolvers) {
                if (solver.appliesTo(prompt)) {
                    Optional<String> result = solver.solve(prompt, captchaCtx);
                    if (result.isPresent()) {
                        answer = safe(result.get());
                        log.info("Captcha solved by {}: {}", solver.getClass().getSimpleName(), answer);
                        break;
                    }
                }
            }

            if (answer == null) {
                if (activeSolvers.isEmpty()) {
                    answer = settingsSnapshot != null ? safe(settingsSnapshot.getPasscode()) : "";
                    log.warn("No captcha solvers available; using blind fallback passcode");
                } else if (settingsSnapshot != null && settingsSnapshot.isStopBotOnCaptchaUnsolved()) {
                    loginState.setFailureReason("No solver could resolve captcha, and stopBotOnCaptchaUnsolved is true");
                    deadlineKeeper.clear(context);
                    safetyDisconnect.safeDisconnect(context);
                    return InteractiveOutcome.TIMED_OUT_DISCONNECT;
                } else {
                    log.warn("No solver could resolve captcha; returning empty answer to prompt retry");
                    answer = "";
                }
            }

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
        return Math.max(5000L, settingsSnapshot.getCaptchaSolveTimeoutMs() > 0 ? settingsSnapshot.getCaptchaSolveTimeoutMs() : settingsSnapshot.getPasscodeUserInputTimeoutMs());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
