import org.sokybot.machinepages.api.BasePage
import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.settings.api.IProfileManager
import org.sokybot.settings.security.ICredentialEncryptor

class ConnectionPage extends BasePage implements EventHandler {

    def loginSettings
    private IProfileManager profileManager
    private ICredentialEncryptor credentialEncryptor
    private String profileName = ""

    ConnectionPage() { super("Connection", "Link") }

    @Override
    String[] getEventTopics(String machineFullName) {
        def mid = osgiEventTopicSegment(machineFullName)
        ["sokybot/network/${mid}/Connected",
         "sokybot/network/${mid}/Disconnected",
         "sokybot/game/${mid}/AgentListEvent",
         "sokybot/game/${mid}/LoginResponseEvent",
         "sokybot/game/${mid}/AuthResponseEvent"] as String[]
    }

    @Override
    void setup() {
        profileManager = service(IProfileManager)
        credentialEncryptor = service(ICredentialEncryptor)
        loginSettings = settingsProvider("login", Object)
        loginSettings?.subscribe { emitStateUpdate() }
    }

    @Override
    Map<String, Object> getInitialState() {
        def settings = loginSettings?.get()
        boolean connected = false
        def loginState = null
        try {
            connected = machineContext.getProxyConnection()?.isServerConnected()
            loginState = machineContext.getGameModel()?.getLoginState()
        } catch (Exception ignore) {}

        def agents = loginState?.getAgentList() ?: []
        def agentOptions = agents.collect { a ->
            [
                    value: String.valueOf(a.getId()),
                    label: "${a.getName()} (${a.getOnlineCount()}/${a.getCapacity()})"
            ]
        }

        return [
            settings: settings ?: [:],
            isDirty: loginSettings?.isDirty() ?: false,
            isUnlocked: credentialEncryptor?.isUnlocked() ?: false,
            connected: connected,
            loginPhase: loginState?.getPhase()?.name() ?: "DISCONNECTED",
            agentList: agents,
            agentOptions: agentOptions,
            profileName: this.profileName,
            profiles: profileManager?.listProfiles(machineContext.getGroupName()) ?: []
        ]
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refresh": break
                case "save":
                    boolean running = false
                    try {
                        running = machineContext?.getEngine()?.isRunning() ?: false
                    } catch (Exception ignored) {}
                    loginSettings?.save()
                    return withSuccess(getInitialState() + [
                            applyOnRestart: running,
                            saveMessage: running
                                    ? "Connection settings saved. Changes apply next bot start."
                                    : "Connection settings saved."
                    ])
                case "connect":
                case "disconnect":
                    return withError("Connection controls moved to Machine Status. Use Start/Stop Bot.")
                case "update":
                    if (data.containsKey("profileName")) {
                        this.profileName = data.get("profileName")
                        emitStateUpdate()
                    }
                    applyFrom(loginSettings, data, [
                            "username", "password", "targetGateway", "autoLogin", "targetAgent", "passcode", "selectedCharacter",
                            "autoReconnect", "retryBaseDelayMs", "retryMaxDelayMs", "maxRetryAttempts", "infiniteRetryMode"
                    ])
                    break
                case "unlock":
                    credentialEncryptor?.unlock((String) data.get("passphrase"))
                    loginSettings?.reload()
                    break
                case "lock":
                    credentialEncryptor?.lock()
                    break
                case "loadProfile":
                    String name = (String) data.get("profileName") ?: this.profileName
                    if (name && profileManager) {
                        profileManager.loadProfile(machineContext.getGroupName(), machineContext.getMachineName(), name)
                        loginSettings?.reload()
                    }
                    break
                case "saveProfile":
                    String name = (String) data.get("profileName") ?: this.profileName
                    if (name && profileManager) {
                        profileManager.saveProfile(machineContext.getGroupName(), name)
                    }
                    break
                case "deleteProfile":
                    String name = (String) data.get("profileName") ?: this.profileName
                    if (name && profileManager) {
                        profileManager.deleteProfile(machineContext.getGroupName(), name)
                    }
                    break
            }
            return withSuccess(getInitialState())
        } catch (Exception e) {
            log.error("Error handling action ${action}", e)
            return withError(e.message)
        }
    }

    @Override
    void handleEvent(Event event) {
        if (event.getTopic().contains("Connected") || event.getTopic().contains("Disconnected")) {
            emitStateUpdate()
        }
    }
}

new ConnectionPage()
