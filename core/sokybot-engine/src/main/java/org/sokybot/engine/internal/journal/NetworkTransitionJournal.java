package org.sokybot.engine.internal.journal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sokybot.engine.core.workflow.WorkflowContextImpl;

/**
 * Ring-buffer of network transition events stored under a fixed key in persistent
 * workflow data.
 */
public final class NetworkTransitionJournal implements INetworkTransitionJournal {

    private static final String KEY_NETWORK_TRANSITIONS = "networkTransitions";
    private static final int NETWORK_TRANSITIONS_MAX = 5;

    private final WorkflowContextImpl workflowContext;

    public NetworkTransitionJournal(WorkflowContextImpl workflowContext) {
        this.workflowContext = workflowContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void append(String transition, String loginPhase, String reason, String endpoint) {
        try {
            Map<String, Object> data = workflowContext.getPersistentData();
            synchronized (data) {
                Object existing = data.get(KEY_NETWORK_TRANSITIONS);
                List<Map<String, Object>> list;
                if (existing instanceof List) {
                    list = (List<Map<String, Object>>) existing;
                } else {
                    list = new ArrayList<>();
                }
                Map<String, Object> ev = new HashMap<>();
                ev.put("transition", transition);
                if (loginPhase != null) {
                    ev.put("loginPhase", loginPhase);
                }
                if (reason != null) {
                    ev.put("reason", reason);
                }
                if (endpoint != null) {
                    ev.put("endpoint", endpoint);
                }
                ev.put("timestamp", System.currentTimeMillis());
                list.add(ev);
                while (list.size() > NETWORK_TRANSITIONS_MAX) {
                    list.remove(0);
                }
                data.put(KEY_NETWORK_TRANSITIONS, list);
            }
        } catch (Exception ignored) {
            // Preserve legacy behavior: never fail connection callbacks on journal issues
        }
    }
}
