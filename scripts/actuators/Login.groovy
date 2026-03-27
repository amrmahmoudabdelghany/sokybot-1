import org.sokybot.settings.security.Encrypted
import org.sokybot.gamemodel.LoginState

class Login extends BaseActuator {

    Login() { super("login") }

    @Override
    void setup() {
        registerSettings("login", LoginSettings, { new LoginSettings() })

        def settingsProvider = settingsProvider("login", LoginSettings)
        if (settingsProvider == null) {
            log.warn("Login: settings provider not available; cycle will not be registered")
            return
        }

        def cycle = new CycleDefinitionBuilder()
                .name("login-cycle")
                .priority(200)
                .entryState("CHECK_CONNECTION")
                .entryGuard({ ctx ->
                    def settings = settingsProvider?.get()
                    def loginState = ctx.getGameModel()?.getLoginState()
                    return settings != null &&
                            settings.isAutoLogin() &&
                            !isAuthenticated(loginState) &&
                            !isLoggedIn(ctx)
                })
                .state("CHECK_CONNECTION", { builder -> builder
                        .guard({ ctx -> !ctx.getDispatcher().isServerConnected() })
                        .action({ ctx ->
                            def settings = settingsProvider?.get()
                            def gateway = parseGateway(settings?.getTargetGateway())
                            if (gateway != null) {
                                ctx.getPersistentData().put("gatewayHost", gateway.host)
                                ctx.getPersistentData().put("gatewayPort", gateway.port)
                            }
                        })
                        .nextState("CONNECT_TO_GATEWAY")
                        .targetState("RETRY_DELAY")
                })
                .state("CONNECT_TO_GATEWAY", { builder -> builder
                        .guard({ ctx -> ctx.getPersistentData().get("gatewayHost") != null })
                        .action({ ctx ->
                            String host = (String) ctx.getPersistentData().get("gatewayHost")
                            int port = (int) ctx.getPersistentData().get("gatewayPort")
                            log.info("Connecting to gateway {}:{}", host, port)
                            ctx.getDispatcher().setClientlessMode(true)
                            ctx.getDispatcher().connect(host, port)
                        })
                        .nextState("WAIT_FOR_CONNECTION")
                        .targetState("RETRY_DELAY")
                })
                .state("WAIT_FOR_CONNECTION", { builder -> builder
                        .guard({ ctx -> ctx.getDispatcher().isServerConnected() })
                        .action({ ctx ->
                            log.info("Gateway connected, requesting agent list")
                            sendToServer(0, ClientOpcode.AGENT_REQUEST) { it }
                        })
                        .nextState("WAIT_FOR_AGENTS")
                        .targetState("RETRY_DELAY")
                })
                .state("WAIT_FOR_AGENTS", { builder -> builder
                        .guard({ ctx -> hasAgents(ctx.getGameModel()?.getLoginState()) })
                        .action({ ctx -> log.info("Agent list received, sending login request") })
                        .nextState("SEND_LOGIN_REQUEST")
                        .targetState("RETRY_DELAY")
                })
                .state("SEND_LOGIN_REQUEST", { builder -> builder
                        .guard({ ctx ->
                            def settings = settingsProvider?.get()
                            return settings != null && settings.getUsername() && settings.getPassword()
                        })
                        .action({ ctx ->
                            def settings = settingsProvider?.get()
                            if (settings == null) return
                            String username = settings.getUsername()
                            String password = settings.getPassword()
                            log.info("Sending login request for user: {}", username)
                            ctx.getGameModel()?.getLoginState()?.setPhase(LoginState.Phase.LOGIN_SENT)

                            byte locale = (byte) settings.getLocale()
                            short agentId = parseAgentId(settings.getTargetAgent())

                            int packetLen = 7 + username.length() + password.length()
                            sendToServer(packetLen, ClientOpcode.LOGIN_REQUEST) { it
                                .put(locale)
                                .putShort((short) username.length())
                                .putBytes(username.getBytes())
                                .putShort((short) password.length())
                                .putBytes(password.getBytes())
                                .putShort(agentId)
                            }
                        })
                        .nextState("WAIT_FOR_AUTH")
                        .targetState("RETRY_DELAY")
                })
                .state("WAIT_FOR_AUTH", { builder -> builder
                        .guard({ ctx -> isAuthenticated(ctx.getGameModel()?.getLoginState()) || isLoggedIn(ctx) })
                        .action({ ctx -> log.info("Login/authentication successful") })
                        .nextState(null)
                        .targetState("RETRY_DELAY")
                })
                .state("RETRY_DELAY", { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx -> })
                        .delay(5000)
                        .nextState(null)
                })
                .build()

        context.getWorkflowRegistry().registerCycle(cycle)
        log.info("Login cycle registered successfully")
    }

    private boolean isLoggedIn(def ctx) {
        try {
            def trainer = ctx.getGameModel().getTrainer()
            return trainer != null && trainer.getUniqueId() > 0
        } catch (Exception e) { return false }
    }

    private short parseAgentId(String targetAgent) {
        if (!targetAgent) return (short) 0
        try { return Short.parseShort(targetAgent) }
        catch (NumberFormatException e) { log.warn("Invalid agent ID format: {}", targetAgent); return (short) 0 }
    }

    private Map parseGateway(String raw) {
        if (!raw) {
            log.warn("No target gateway configured")
            return null
        }
        def value = raw.trim()
        if (value.isEmpty()) {
            log.warn("Empty target gateway configured")
            return null
        }

        if (value.contains(":")) {
            def parts = value.split(":", 2)
            if (parts[0]?.trim()) {
                try {
                    return [host: parts[0].trim(), port: Integer.parseInt(parts[1].trim())]
                } catch (Exception e) {
                    log.warn("Invalid target gateway format '{}', expected host:port", raw)
                    return null
                }
            }
        }

        return [host: value, port: 15779]
    }

    private boolean hasAgents(def loginState) {
        try {
            return loginState != null && loginState.getAgentList() != null && !loginState.getAgentList().isEmpty()
        } catch (Exception e) {
            return false
        }
    }

    private boolean isAuthenticated(def loginState) {
        try {
            return loginState != null && loginState.getPhase() == LoginState.Phase.AUTHENTICATED
        } catch (Exception e) {
            return false
        }
    }
}

class LoginSettings {
    String targetGateway = ""
    @Encrypted String username = ""
    @Encrypted String password = ""
    @Encrypted String passcode = ""
    String targetAgent = ""
    int locale = 22
    boolean autoLogin = false
}

new Login()
