package org.sokybot.webview.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sokybot.proxy.IProxyConnection;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.gamemodel.LoginState;
import org.sokybot.gameevents.dto.AgentInfo;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;
import org.sokybot.webview.login.GatewayLoginMessages;
import org.sokybot.webview.util.MachineResolver;

import reactor.core.publisher.Mono;

/**
 * Handler for character state requests.
 * 
 * Method: character.state
 * Params:
 * - machineId (optional): Machine identifier. If not provided, uses first
 * available machine.
 */
@Component(service = IRSocketHandler.class, property = IRSocketHandler.METHOD_PROPERTY + "=character.state")
public class CharacterStateHandler implements IRSocketHandler {

    private volatile ISokybotContext sokybotContext;
    private volatile ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
    }

    protected void unsetSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }

    protected void unsetSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] { "character.state" };
    }

    @Override
    public String getDescription() {
        return "Get character state for a machine";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        if (sokybotContext == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Sokybot context not available"));
        }

        String machineId = request.getString("machineId");
        IMachineContext ctx = null;

        // If no machineId provided, use first available from first group
        if (machineId == null || machineId.isEmpty()) {
            for (IGroupContext group : sokybotContext.getGroups()) {
                IMachineContext[] machines = group.getMachines();
                if (machines.length > 0) {
                    ctx = machines[0];
                    break;
                }
            }
            if (ctx == null) {
                return Mono.just(RSocketResponse.notFound("No machines available"));
            }
        } else {
            // Try to find machine by full name (Group.Machine) or simple name
            String groupName = null;
            String machineName = machineId;

            if (machineId.contains(".")) {
                String[] parts = machineId.split("\\.", 2);
                groupName = parts[0];
                machineName = parts[1];
            }

            if (groupName != null) {
                ctx = MachineResolver.resolve(sokybotContext, null, machineId).orElse(null);
            } else {
                List<IMachineContext> byName = MachineResolver.findAllBySimpleName(sokybotContext, machineName);
                if (byName.isEmpty()) {
                    return Mono.just(RSocketResponse.notFound("Machine not found: " + machineId));
                }
                ctx = pickDisambiguatedMachine(byName);
            }

            if (ctx == null) {
                return Mono.just(RSocketResponse.notFound("Machine not found: " + machineId));
            }
        }

        Map<String, Object> state = new HashMap<>();
        state.put("isRunning", ctx.isRunning());

        ITrainer trainer = null;
        if (ctx.getGameModel() != null) {
            trainer = ctx.getGameModel().getTrainer();
        }
        if (trainer != null) {
            state.put("entityId", trainer.getUniqueId());
            state.put("characterName", trainer.getName());
            state.put("level", trainer.getLevel());
            state.put("currentHP", trainer.getCurrentHP());
            state.put("maxHP", trainer.getMaxHP());
            state.put("currentMP", trainer.getCurrentMP());
            state.put("maxMP", trainer.getMaxMP());
            state.put("gold", trainer.getGold());
            state.put("xSector", trainer.getRefId());
            state.put("x", trainer.getX());
            state.put("y", trainer.getY());
        }
        state.put("inGame", trainer != null && trainer.getUniqueId() > 0);

        boolean connected = false;
        String loginPhase = "DISCONNECTED";
        LoginState.Phase phaseEnum = LoginState.Phase.DISCONNECTED;
        int agentsDiscovered = 0;
        boolean authenticated = false;
        boolean signInComplete = false;
        boolean awaitingCharacterSelection = false;
        Integer gatewayResultCode = null;
        Integer agentAuthResultCode = null;
        String failureReasonSnap = null;
        String loginDetailMessage = null;
        Integer queuePosition = null;
        List<Map<String, Object>> agentOptions = java.util.Collections.emptyList();
        List<String> availableCharacters = java.util.Collections.emptyList();
        String selectedCharacter = null;
        try {
            boolean transportLive = false;
            boolean tcpConnected = false;
            IProxyConnection proxy = ctx.getProxyConnection();
            if (proxy != null) {
                transportLive = proxy.isGameServerChannelActive();
                tcpConnected = proxy.isServerConnected();
            }
            if (ctx.getGameModel() != null && ctx.getGameModel().getLoginState() != null) {
                LoginState ls = ctx.getGameModel().getLoginState();
                if (ls.getPhase() != null) {
                    phaseEnum = ls.getPhase();
                    loginPhase = phaseEnum.name();
                }
                if (ls.getAgentList() != null) {
                    agentsDiscovered = ls.getAgentList().size();
                    agentOptions = ls.getAgentList().stream()
                            .map(this::toAgentOption)
                            .collect(Collectors.toList());
                }
                // Matches Login.groovy isAuthenticated: only AUTHENTICATED phase (not IN_GAME / queue).
                authenticated = ls.getPhase() == LoginState.Phase.AUTHENTICATED;
                signInComplete = ls.isAuthSuccess();
                awaitingCharacterSelection = phaseEnum == LoginState.Phase.MISSING_CHARACTER_SELECTION;
                gatewayResultCode = ls.getGatewayResultCode();
                agentAuthResultCode = ls.getAgentAuthResultCode();
                failureReasonSnap = ls.getFailureReason();
                loginDetailMessage = GatewayLoginMessages.deriveLoginDetailMessage(
                        phaseEnum, gatewayResultCode, agentAuthResultCode, failureReasonSnap);
                queuePosition = ls.getQueuePosition();
                availableCharacters = ls.getAvailableCharacterNames();
                selectedCharacter = ls.getSelectedCharacterName();
            }
            connected = transportLive || tcpConnected || loginPhaseIndicatesActiveSession(phaseEnum);
            if (LoginState.Phase.DISCONNECTED.equals(phaseEnum) && transportLive) {
                loginPhase = LoginState.Phase.CONNECTING_GATEWAY.name();
            }
        } catch (Exception ignored) {
            // Keep safe defaults for UI.
        }
        try {
            if (settingsRegistry != null) {
                Map<String, Object> raw = settingsRegistry.readRawSettings(ctx.getGroupName(), ctx.getMachineName(),
                        "login");
                Map<String, Object> savedLogin = new HashMap<>();
                copyLoginField(raw, savedLogin, "targetGateway");
                copyLoginField(raw, savedLogin, "targetAgent");
                copyLoginField(raw, savedLogin, "username");
                copyLoginBool(raw, savedLogin, "autoLogin");
                copyLoginBool(raw, savedLogin, "autoReconnect");
                if (!savedLogin.isEmpty()) {
                    state.put("savedLogin", savedLogin);
                }
            }
        } catch (Exception ignored) {
            // optional snapshot
        }
        state.put("connected", connected);
        state.put("loginPhase", loginPhase);
        state.put("agentsDiscovered", agentsDiscovered);
        state.put("authenticated", authenticated);
        state.put("signInComplete", Boolean.valueOf(signInComplete));
        state.put("awaitingCharacterSelection", Boolean.valueOf(awaitingCharacterSelection));
        state.put("gatewayResultCode", gatewayResultCode);
        state.put("agentAuthResultCode", agentAuthResultCode);
        state.put("failureReason", failureReasonSnap);
        state.put("loginDetailMessage", loginDetailMessage);
        state.put("queuePosition", queuePosition);
        state.put("agentOptions", agentOptions);
        state.put("availableCharacters", availableCharacters);
        state.put("selectedCharacter", selectedCharacter);

        return Mono.just(RSocketResponse.success(state));
    }

    /**
     * When multiple groups contain the same machine name, prefer a unique "live" machine; if several are live or
     * none are, pick deterministically by {@link IMachineContext#fullName()} lexicographic order.
     */
    private IMachineContext pickDisambiguatedMachine(List<IMachineContext> matches) {
        List<IMachineContext> live = new ArrayList<>();
        for (IMachineContext m : matches) {
            if (isMachinePreferableForDisambiguation(m)) {
                live.add(m);
            }
        }
        List<IMachineContext> pickFrom = live.isEmpty() ? new ArrayList<>(matches) : live;
        pickFrom.sort(Comparator.comparing(IMachineContext::fullName));
        if (pickFrom.isEmpty()) {
            return null;
        }
        return pickFrom.get(0);
    }

    private static boolean isMachinePreferableForDisambiguation(IMachineContext ctx) {
        try {
            if (ctx.isRunning()) {
                return true;
            }
            IProxyConnection pc = ctx.getProxyConnection();
            if (pc != null && (pc.isGameServerChannelActive() || pc.isServerConnected())) {
                return true;
            }
            if (ctx.getGameModel() != null && ctx.getGameModel().getLoginState() != null) {
                LoginState.Phase p = ctx.getGameModel().getLoginState().getPhase();
                return loginPhaseIndicatesActiveSession(p);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return false;
    }

    private Map<String, Object> toAgentOption(AgentInfo agent) {
        Map<String, Object> option = new HashMap<>();
        option.put("value", String.valueOf(agent.getId()));
        option.put("label", String.format("%s (%d/%d)", agent.getName(), agent.getOnlineCount(), agent.getCapacity()));
        return option;
    }

    /** True when the login workflow has left an idle/disabled failure state (covers gateway redirects, etc.). */
    private static boolean loginPhaseIndicatesActiveSession(LoginState.Phase phase) {
        if (phase == null) {
            return false;
        }
        switch (phase) {
            case DISCONNECTED:
            case FAILED:
            case RETRY_DISABLED:
            case RETRY_LIMIT_REACHED:
                return false;
            default:
                return true;
        }
    }

    private static void copyLoginField(Map<String, Object> raw, Map<String, Object> dest, String key) {
        if (raw == null || !raw.containsKey(key)) {
            return;
        }
        Object v = raw.get(key);
        if (v == null) {
            return;
        }
        if ("username".equals(key)) {
            dest.put("usernameSet", Boolean.valueOf(String.valueOf(v).trim().length() > 0));
            return;
        }
        dest.put(key, String.valueOf(v));
    }

    private static void copyLoginBool(Map<String, Object> raw, Map<String, Object> dest, String key) {
        if (raw == null || !raw.containsKey(key)) {
            return;
        }
        Object v = raw.get(key);
        if (v instanceof Boolean) {
            dest.put(key, v);
        } else if (v != null) {
            dest.put(key, Boolean.parseBoolean(String.valueOf(v)));
        }
    }
}
