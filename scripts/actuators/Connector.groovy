class Connector extends BaseActuator {
    Connector() { super("connector") }

    @Override
    void setup() {
        // Connector cycle is retired. Login actuator owns connect + login flow.
        log.info("Connector actuator is inactive; login-cycle handles connection flow")
    }

    @Override
    void shutdown(IActuatorContext ctx) {
        // No-op: connector cycle retired.
        super.shutdown(ctx)
    }
}

new Connector()
