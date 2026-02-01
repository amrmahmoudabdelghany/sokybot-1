import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor
import org.slf4j.LoggerFactory

/**
 * A sample Groovy actuator that demonstrates hot-reloading.
 */
class HelloActuator implements IActuator {
    private static final log = LoggerFactory.getLogger("HelloActuator")

    @Override
    String getName() {
        return "hello-script"
    }

    @Override
    void initialize(IActuatorContext context) {
        log.info("Hello Actuator Initialized!")
    }

    @Override
    void shutdown(IActuatorContext context) {
        log.info("Hello Actuator Shutdown!")
    }

    @Override
    ActuatorDescriptor getDescriptor() {
        return ActuatorDescriptor.builder(getName())
            .displayName("Groovy Hello Actuator")
            .description("A sample actuator written in Groovy")
            .version("1.0.0")
            .author("Sokybot")
            .build()
    }
}

// Return the instance
new HelloActuator()
