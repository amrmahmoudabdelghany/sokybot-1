package org.sokybot.login.security.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.workflow.IWorkflowContext;

final class LoginSafetyDisconnect {

    private static final Logger log = LoggerFactory.getLogger(LoginSafetyDisconnect.class);

    void safeDisconnect(IWorkflowContext context) {
        try {
            if (context.getDispatcher() != null) {
                context.getDispatcher().disconnect();
                return;
            }
        } catch (Exception e) {
            log.debug("Dispatcher disconnect failed: {}", e.getMessage());
        }
        try {
            if (context.getProxyConnection() != null) {
                context.getProxyConnection().disconnect();
            }
        } catch (Exception e) {
            log.debug("Proxy disconnect failed: {}", e.getMessage());
        }
    }

    boolean isServerConnected(IWorkflowContext context) {
        try {
            return context.getDispatcher() != null && context.getDispatcher().isServerConnected();
        } catch (Exception ignored) {
            return false;
        }
    }
}
