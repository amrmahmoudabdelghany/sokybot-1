package org.sokybot.session.api;

import java.util.Optional;

/**
 * SPI for captcha/passcode solving strategies.
 * <p>
 * Implementations are registered as OSGi services with {@code service.ranking}.
 * The login coordinator picks the highest-ranked solver where
 * {@link #appliesTo(CaptchaPrompt)} returns true.
 * <p>
 * Built-in solvers:
 * <ul>
 *   <li>{@code PasscodeBlindSolver} (ranking 0) — blind-submits the saved passcode</li>
 *   <li>{@code ManualUserAlertSolver} (ranking 10) — blocks for operator input on image captchas</li>
 * </ul>
 * Third-party solvers (e.g. OCR, TwoCaptcha) can register with higher rankings.
 */
public interface ICaptchaSolver {

    /**
     * Unique solver identifier (e.g. "blind-passcode", "manual-alert", "twocaptcha").
     */
    String getId();

    /**
     * OSGi {@code service.ranking} convention. Higher ranking wins.
     */
    int getRanking();

    /**
     * Returns true if this solver can handle the given prompt type.
     *
     * @param prompt the captcha challenge to evaluate
     * @return true if this solver should be used for this prompt
     */
    boolean appliesTo(CaptchaPrompt prompt);

    /**
     * Attempt to solve the captcha. May block until solved or the deadline expires.
     *
     * @param prompt  the captcha challenge to solve
     * @param context runtime context including machine ID, settings, and deadline
     * @return the answer string, or empty if unsolvable / timed out
     */
    Optional<String> solve(CaptchaPrompt prompt, ICaptchaContext context);
}
