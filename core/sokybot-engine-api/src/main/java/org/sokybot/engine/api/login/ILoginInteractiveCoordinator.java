package org.sokybot.engine.api.login;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Handles passcode/captcha/manual-interaction waits as a single coordinated unit.
 */
public interface ILoginInteractiveCoordinator {

    InteractiveOutcome handle(IWorkflowContext context, LoginSettingsSnapshot settingsSnapshot);
}
