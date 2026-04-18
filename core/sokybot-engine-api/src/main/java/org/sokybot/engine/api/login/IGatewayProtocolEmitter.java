package org.sokybot.engine.api.login;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Emits low-level gateway protocol packets with resilient dispatcher/proxy fallback semantics.
 */
public interface IGatewayProtocolEmitter {

    boolean requestAgentList(IWorkflowContext context, boolean allowColdStartBypass, long minIntervalMs);

    void sendLoginRequest(
            IWorkflowContext context,
            GatewayCredentials credentials,
            byte locale,
            int agentId,
            String charsetName,
            boolean awaitGatewayPause
    );

    void sendGatewayImageCodeAnswer(IWorkflowContext context, String answer);

    void sendLogoutRequest(IWorkflowContext context);

    void gracefulShutdown(IWorkflowContext context, long logoutAckTimeoutMs);
}
