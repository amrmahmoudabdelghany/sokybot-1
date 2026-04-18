package org.sokybot.engine.api.login;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Produces immutable runtime login settings snapshots from the active machine context.
 */
public interface ILoginSettingsSnapshotter {

    LoginSettingsSnapshot snapshot(IWorkflowContext context, boolean forceRefresh);
}
