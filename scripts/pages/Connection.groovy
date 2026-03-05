import org.osgi.service.event.Event
import org.osgi.service.event.EventHandler
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.sokybot.settings.api.ISettingsRegistry
import org.sokybot.settings.api.IProfileManager
import org.sokybot.settings.security.ICredentialEncryptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class ConnectionPage implements IScriptedPage, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPage.class)

    private IMachineContext machineContext
    private def loginSettingsProvider
    private IProfileManager profileManager
    private ICredentialEncryptor credentialEncryptor
    
    private final Sinks.Many<Map<String, Object>> stateSink = Sinks.many().multicast().onBackpressureBuffer(100)
    
    private String profileName = ""

     
    String getTitle() { "Connection" }

     
    String getIcon() { "Link" }

     
     @Override void init(IMachineContext context) {
        this.machineContext = context
        
        def settingsRegistry = context.getSokybotContext().getService(ISettingsRegistry.class)
        this.profileManager = context.getSokybotContext().getService(IProfileManager.class)
        this.credentialEncryptor = context.getSokybotContext().getService(ICredentialEncryptor.class)

        // Access LoginSettings from the registry. 
        // We use Object.class because the actual class is defined in Login.groovy
        this.loginSettingsProvider = settingsRegistry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                "login",
                Object.class)

        this.loginSettingsProvider.subscribe { settings -> emitStateUpdate() }
        
        log.info("Groovy ConnectionPage initialized for {}", context.fullName())
    }

     
    Map<String, Object> getSchema() {
        return [:] // Loaded from Connection.json by ScriptPageLoader
    }

     
    Map<String, Object> getInitialState() {
        return getState()
    }

     
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        log.debug("Handling action: {} with data: {}", action, data)
        
        try {
            switch (action) {
                case "refresh":
                    break
                case "save":
                    loginSettingsProvider.save()
                    break
                case "update":
                    updateSettings(data)
                    break
                case "unlock":
                    String passphrase = (String) data.get("passphrase")
                    credentialEncryptor.unlock(passphrase)
                    loginSettingsProvider.reload()
                    break
                case "lock":
                    credentialEncryptor.lock()
                    break
                case "loadProfile":
                    String name = (String) data.get("profileName") ?: this.profileName
                    if (name) {
                        profileManager.loadProfile(machineContext.getGroupName(), machineContext.getMachineName(), name)
                        loginSettingsProvider.reload()
                    }
                    break
                case "saveProfile":
                    String name = (String) data.get("profileName") ?: this.profileName
                    if (name) {
                        profileManager.saveProfile(machineContext.getGroupName(), name)
                    }
                    break
                case "deleteProfile":
                    String name = (String) data.get("profileName") ?: this.profileName
                    if (name) {
                        profileManager.deleteProfile(machineContext.getGroupName(), name)
                    }
                    break
            }
            
            def newState = getState()
            newState.put("success", true)
            return newState
            
        } catch (Exception e) {
            log.error("Error handling action ${action}", e)
            return [success: false, error: e.message]
        }
    }

     
    Flux<Object> streamData(String streamName, Map<String, Object> params) {
        return Flux.concat(
                Flux.just(getState()),
                stateSink.asFlux()
        )
    }

     
    void handleEvent(Event event) {
        // MachinePagesActivator filters by machine, so we just check topics
        String topic = event.getTopic()
        if (topic.contains("Connected") || topic.contains("Disconnected")) {
            emitStateUpdate()
        }
    }

     
    void shutdown() {
        stateSink.tryEmitComplete()
    }

    private void updateSettings(Map<String, Object> data) {
        if (data.containsKey("profileName")) {
            this.profileName = data.get("profileName")
            emitStateUpdate()
        }
        
        loginSettingsProvider.update { settings ->
            if (data.containsKey("username")) settings.username = data.get("username")
            if (data.containsKey("password")) settings.password = data.get("password")
            if (data.containsKey("targetGateway")) settings.targetGateway = data.get("targetGateway")
            if (data.containsKey("autoLogin")) settings.autoLogin = data.get("autoLogin")
            if (data.containsKey("targetAgent")) settings.targetAgent = data.get("targetAgent")
            if (data.containsKey("passcode")) settings.passcode = data.get("passcode")
        }
    }

    private Map<String, Object> getState() {
        def settings = loginSettingsProvider.get()
        boolean connected = machineContext.getNetworkController().getProxyConnection().isConnected()
        
        return [
            settings: settings,
            isDirty: loginSettingsProvider.isDirty(),
            isUnlocked: credentialEncryptor.isUnlocked(),
            connected: connected,
            profileName: this.profileName,
            profiles: profileManager.listProfiles(machineContext.getGroupName())
        ]
    }

    private void emitStateUpdate() {
        stateSink.tryEmitNext(getState())
    }
}

new ConnectionPage()
