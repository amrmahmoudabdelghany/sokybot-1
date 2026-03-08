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
        ["sokybot/network/${machineFullName}/Connected",
         "sokybot/network/${machineFullName}/Disconnected"] as String[]
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
        try { connected = machineContext.getProxyConnection()?.isConnected() } catch (Exception ignore) {}

        return [
            settings: settings ?: [:],
            isDirty: loginSettings?.isDirty() ?: false,
            isUnlocked: credentialEncryptor?.isUnlocked() ?: false,
            connected: connected,
            profileName: this.profileName,
            profiles: profileManager?.listProfiles(machineContext.getGroupName()) ?: []
        ]
    }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refresh": break
                case "save": loginSettings?.save(); break
                case "update":
                    if (data.containsKey("profileName")) {
                        this.profileName = data.get("profileName")
                        emitStateUpdate()
                    }
                    applyFrom(loginSettings, data, ["username", "password", "targetGateway", "autoLogin", "targetAgent", "passcode"])
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
