package org.sokybot.gamemodel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sokybot.gameevents.dto.AgentInfo;
import org.sokybot.gameevents.events.character.CharacterSelectionActionEvent;

public class LoginState {

    public enum Phase {
        DISCONNECTED,
        CONNECTING_GATEWAY,
        GATEWAY_CONNECTED,
        WAITING_FOR_AGENTS,
        WAITING_FOR_AGENTS_TIMEOUT,
        AGENTS_RECEIVED,
        LOGIN_SENT,
        LOGIN_SUCCESS,
        REDIRECTING,
        AGENT_CONNECTED,
        AUTH_SENT,
        AUTHENTICATED,
        MANUAL_VERIFICATION_REQUIRED,
        RETRY_DELAY,
        RETRY_DISABLED,
        RETRY_LIMIT_REACHED,
        MISSING_GATEWAY,
        MISSING_CREDENTIALS,
        MISSING_AGENT_SERVER,
        MISSING_CHARACTER_SELECTION,
        FAILED
    }

    private volatile Phase phase = Phase.DISCONNECTED;
    private volatile int loginId;
    private volatile String agentHost;
    private volatile int agentPort;
    private volatile boolean authSuccess;
    private volatile List<AgentInfo> agentList = Collections.emptyList();
    private volatile List<CharacterSelectionActionEvent.CharSelectionEntry> availableCharacters = Collections.emptyList();
    private volatile String selectedCharacterName;
    private volatile String failureReason;

    public synchronized void reset() {
        this.phase = Phase.DISCONNECTED;
        this.loginId = 0;
        this.agentHost = null;
        this.agentPort = 0;
        this.authSuccess = false;
        this.agentList = Collections.emptyList();
        this.availableCharacters = Collections.emptyList();
        this.selectedCharacterName = null;
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

    public List<CharacterSelectionActionEvent.CharSelectionEntry> getAvailableCharacters() {
        return availableCharacters;
    }

    public void setAvailableCharacters(List<CharacterSelectionActionEvent.CharSelectionEntry> availableCharacters) {
        this.availableCharacters = availableCharacters == null ? Collections.emptyList()
                : Collections.unmodifiableList(availableCharacters);
    }

    public List<String> getAvailableCharacterNames() {
        return availableCharacters.stream()
                .map(CharacterSelectionActionEvent.CharSelectionEntry::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String getSelectedCharacterName() {
        return selectedCharacterName;
    }

    public void setSelectedCharacterName(String selectedCharacterName) {
        this.selectedCharacterName = selectedCharacterName;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
