package org.sokybot.gamemodel;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.dto.AgentInfo;

public class LoginState {

    public enum Phase {
        DISCONNECTED,
        GATEWAY_CONNECTED,
        AGENTS_RECEIVED,
        LOGIN_SENT,
        LOGIN_SUCCESS,
        REDIRECTING,
        AGENT_CONNECTED,
        AUTH_SENT,
        AUTHENTICATED,
        FAILED
    }

    private volatile Phase phase = Phase.DISCONNECTED;
    private volatile int loginId;
    private volatile String agentHost;
    private volatile int agentPort;
    private volatile boolean authSuccess;
    private volatile List<AgentInfo> agentList = Collections.emptyList();
    private volatile String failureReason;

    public synchronized void reset() {
        this.phase = Phase.DISCONNECTED;
        this.loginId = 0;
        this.agentHost = null;
        this.agentPort = 0;
        this.authSuccess = false;
        this.agentList = Collections.emptyList();
        this.failureReason = null;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public int getLoginId() {
        return loginId;
    }

    public void setLoginId(int loginId) {
        this.loginId = loginId;
    }

    public String getAgentHost() {
        return agentHost;
    }

    public void setAgentHost(String agentHost) {
        this.agentHost = agentHost;
    }

    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }

    public boolean isAuthSuccess() {
        return authSuccess;
    }

    public void setAuthSuccess(boolean authSuccess) {
        this.authSuccess = authSuccess;
    }

    public List<AgentInfo> getAgentList() {
        return agentList;
    }

    public void setAgentList(List<AgentInfo> agentList) {
        this.agentList = agentList == null ? Collections.emptyList() : Collections.unmodifiableList(agentList);
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
