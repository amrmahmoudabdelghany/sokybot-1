package org.sokybot.session.solvers.builtin.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.session.CaptchaChallengeEvent;
import org.sokybot.session.api.CaptchaPrompt;
import org.sokybot.session.api.ICaptchaContext;
import org.sokybot.session.api.ICaptchaSolver;

/**
 * Higher-priority solver that handles image captchas by alerting the operator.
 * <p>
 * When the server sends a captcha with image data, this solver:
 * <ol>
 *   <li>Publishes a {@link CaptchaChallengeEvent} to the reactive event bus
 *       (the session projection will set phase to {@code CAPTCHA_PENDING}).</li>
 *   <li>Blocks on the {@link CaptchaInbox} waiting for the operator to submit
 *       an answer from the UI.</li>
 *   <li>Returns the answer or {@link Optional#empty()} on timeout.</li>
 * </ol>
 * Ranking 10 means this solver takes priority over {@link PasscodeBlindSolver}
 * when image data is present.
 */
@Component(
    service = ICaptchaSolver.class,
    property = "service.ranking:Integer=10"
)
public final class ManualUserAlertSolver implements ICaptchaSolver {

    private static final Logger log = LoggerFactory.getLogger(ManualUserAlertSolver.class);

    private static final String ID = "manual-alert";

    @Reference
    private volatile IReactiveEventBus eventBus;

    @Reference
    private volatile CaptchaInbox captchaInbox;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getRanking() {
        return 10;
    }

    @Override
    public boolean appliesTo(CaptchaPrompt prompt) {
        // Handles image captchas only
        return prompt.getImageData() != null && prompt.getImageData().length > 0;
    }

    @Override
    public Optional<String> solve(CaptchaPrompt prompt, ICaptchaContext context) {
        String machineId = context.getMachineId();
        int captchaId = prompt.getCaptchaId();

        log.info("[{}] Image captcha detected (captchaId={}, imageSize={}). "
                + "Alerting operator and waiting for manual input.",
                machineId, captchaId,
                prompt.getImageData() != null ? prompt.getImageData().length : 0);

        // 1. Publish event so the projection switches to CAPTCHA_PENDING
        IReactiveEventBus bus = this.eventBus;
        if (bus != null) {
            bus.publish(new CaptchaChallengeEvent(
                    machineId, captchaId, prompt.getImageData()));
        }

        // 2. Block on the inbox until the operator submits or deadline expires
        long deadlineMs = context.getDeadlineEpochMs();
        Optional<String> answer = captchaInbox.awaitAnswer(machineId, captchaId, deadlineMs);

        if (answer.isPresent()) {
            log.info("[{}] Operator submitted captcha answer (captchaId={})", machineId, captchaId);
        } else {
            log.warn("[{}] Captcha solve timed out (captchaId={})", machineId, captchaId);
        }

        return answer;
    }
}
