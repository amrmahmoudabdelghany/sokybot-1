package org.sokybot.session.solvers.builtin.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.session.api.CaptchaPrompt;
import org.sokybot.session.api.ICaptchaContext;
import org.sokybot.session.api.ICaptchaSolver;

/**
 * Lowest-priority captcha solver that blind-submits the saved passcode.
 * <p>
 * This preserves today's behavior for static account passcodes: when the
 * server sends a passcode prompt (no image data), this solver immediately
 * returns the passcode from {@code LoginSettingsSnapshot}.
 * <p>
 * Ranking 0 means any higher-ranked solver will take priority when it
 * applies to the given prompt.
 */
@Component(
    service = ICaptchaSolver.class,
    property = "service.ranking:Integer=0"
)
public final class PasscodeBlindSolver implements ICaptchaSolver {

    private static final Logger log = LoggerFactory.getLogger(PasscodeBlindSolver.class);

    private static final String ID = "blind-passcode";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getRanking() {
        return 0;
    }

    @Override
    public boolean appliesTo(CaptchaPrompt prompt) {
        // Handles passcode-only prompts (no image data)
        return prompt.getImageData() == null || prompt.getImageData().length == 0;
    }

    @Override
    public Optional<String> solve(CaptchaPrompt prompt, ICaptchaContext context) {
        String passcode = context.getLoginSettingsSnapshot().getPasscode();
        String trimmed = passcode != null ? passcode.trim() : "";
        log.debug("[{}] Blind-submitting saved passcode (captchaId={})",
                context.getMachineId(), prompt.getCaptchaId());
        return Optional.of(trimmed);
    }
}
