package org.sokybot.gamemodel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
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
        IN_QUEUE,
        AUTH_SENT,
        AUTHENTICATED,
        LOADING_ENVIRONMENT,
        IN_GAME,
        MANUAL_VERIFICATION_REQUIRED,
        WAITING_FOR_PASSCODE,
        WAIT_FOR_CAPTCHA,
        PASSCODE_SUBMITTED,
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
    private volatile Integer queuePosition;
    private volatile boolean worldReady;
    private volatile boolean spawnSyncActive;
    private final AtomicReference<Integer> gatewayResultCode = new AtomicReference<>();
    private final AtomicReference<Integer> agentAuthResultCode = new AtomicReference<>();

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
        this.queuePosition = null;
        this.worldReady = false;
        this.spawnSyncActive = false;
        this.gatewayResultCode.set(null);
        this.agentAuthResultCode.set(null);
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

    public Integer getGatewayResultCode() {
        return gatewayResultCode.get();
    }

    public void setGatewayResultCode(Integer code) {
        gatewayResultCode.set(code);
    }

    public Integer getAgentAuthResultCode() {
        return agentAuthResultCode.get();
    }

    public void setAgentAuthResultCode(Integer code) {
        agentAuthResultCode.set(code);
    }

    public void clearLoginResultCodes() {
        gatewayResultCode.set(null);
        agentAuthResultCode.set(null);
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(Integer queuePosition) {
        this.queuePosition = queuePosition;
    }

    public boolean isWorldReady() {
        return worldReady;
    }

    public void setWorldReady(boolean worldReady) {
        this.worldReady = worldReady;
    }

    public boolean isSpawnSyncActive() {
        return spawnSyncActive;
    }

    public void setSpawnSyncActive(boolean spawnSyncActive) {
        this.spawnSyncActive = spawnSyncActive;
    }
}
