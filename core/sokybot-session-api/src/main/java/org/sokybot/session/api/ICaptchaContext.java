package org.sokybot.session.api;

import org.sokybot.engine.api.login.LoginSettingsSnapshot;

/**
 * Runtime context provided to {@link ICaptchaSolver} implementations during
 * captcha resolution.
 * <p>
 * Provides the machine identity, current login settings, and a hard deadline
 * after which the solver must return.
 */
public interface ICaptchaContext {

    /**
     * Gets the machine identifier for this captcha session.
     */
    String getMachineId();

    /**
     * Gets the current login settings snapshot.
     */
    LoginSettingsSnapshot getLoginSettingsSnapshot();

    /**
     * Gets the hard deadline (epoch millis) by which the solver must return.
     * Derived from the login settings timeout configuration.
     */
    long getDeadlineEpochMs();
}
