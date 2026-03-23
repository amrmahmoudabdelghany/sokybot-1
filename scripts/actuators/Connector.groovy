class Connector extends BaseActuator {

    Connector() { super("connector") }

    @Override
    void setup() {
        String testHost = "192.168.100.112"
        int testPort = 15779

        def cycle = new CycleDefinitionBuilder()
                .name("connector-cycle")
                .priority(1000)
                .entryState("CHECK_CONNECTION")
                .entryGuard({ ctx -> !ctx.getDispatcher().isServerConnected() })
                .state("CHECK_CONNECTION", { builder -> builder
                        .guard({ ctx -> !ctx.getDispatcher().isServerConnected() })
                        .action({ ctx ->
                            log.info("Connection check: not connected to server, attempting to connect")
                        })
                        .nextState("CONNECT_TO_SERVER")
                })
                .state("CONNECT_TO_SERVER", { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx ->
                            log.info("Connecting to gateway {}:{}", testHost, testPort)
                            ctx.getDispatcher().setClientlessMode(true)
                            ctx.getDispatcher().connect(testHost, testPort)
                        })
                        .nextState("WAIT_FOR_CONNECTION")
                        .targetState("CHECK_CONNECTION")
                })
                .state("WAIT_FOR_CONNECTION", { builder -> builder
                        .guard({ ctx -> ctx.getDispatcher().isServerConnected() })
                        .action({ ctx ->
                            log.info("Connected successfully to server")
                            sendToServer(0, ClientOpcode.AGENT_REQUEST) { it }
                            log.info("Sent agent request packet")
                        })
                        .nextState(null)
                        .targetState("RETRY_DELAY")
                })
                .state("RETRY_DELAY", { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx ->
                            // Optional: log or handle retry logic here
                        })
                        .delay(5000)
                        .nextState(null)
                })
                .build()

        context.getWorkflowRegistry().registerCycle(cycle)
        log.info("Connector cycle registered successfully")
    }
}

new Connector()
