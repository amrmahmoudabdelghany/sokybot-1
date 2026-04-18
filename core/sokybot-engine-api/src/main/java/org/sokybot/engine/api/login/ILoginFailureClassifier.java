package org.sokybot.engine.api.login;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Classifies current login failure state into a typed verdict.
 */
public interface ILoginFailureClassifier {

    LoginFailureVerdict classify(IWorkflowContext context, String fallbackReason);
}
