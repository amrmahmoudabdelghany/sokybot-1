import org.sokybot.settings.security.Encrypted

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
                .entryState("CHECK_LOGGED_IN")
                .entryGuard({ ctx ->
                    def settings = settingsProvider?.get()
                    return settings != null && ctx.getDispatcher().isConnected() &&
                            settings.isAutoLogin() &&
                            !isLoggedIn(ctx)
                })
                .state("CHECK_LOGGED_IN", { builder -> builder
                        .guard({ ctx -> !isLoggedIn(ctx) })
                        .action({ ctx -> log.info("Not logged in, starting login sequence") })
                        .nextState("SEND_LOGIN_REQUEST")
                        .targetState(null)
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
                        .nextState("WAIT_FOR_LOGIN_RESPONSE")
                        .targetState("CHECK_LOGGED_IN")
                })
                .state("WAIT_FOR_LOGIN_RESPONSE", { builder -> builder
                        .guard({ ctx -> isLoggedIn(ctx) })
                        .action({ ctx -> log.info("Login successful") })
                        .nextState(null)
                        .targetState("CHECK_LOGGED_IN")
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
