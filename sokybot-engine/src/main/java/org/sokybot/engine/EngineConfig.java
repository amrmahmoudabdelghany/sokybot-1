package org.sokybot.engine;

import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.model.UserAction;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.statemachine.StateMachine;

@Configuration
public class EngineConfig {

    @Autowired
    ApplicationContext ctx;

    @Autowired
    Logger log;

    @Autowired
    StateMachine<MachineState, IMachineEvent> machine;

    @Bean
    @Order(4)
    ApplicationRunner initializeUserConfig() {
        return args -> {

            Settings config = this.ctx.getBean(Settings.class);

            if (args.containsOption(AppConstants.MACHINE_AUTO_LOGIN)) {
                config.setAutoLogin(true);
            }

            args.getNonOptionArgs()
                    .stream()
                    .map((l) -> l.split("="))
                    .forEach((pair) -> {
                        if (pair.length == 2) {
                            switch (pair[0]) {

                            case AppConstants.MACHINE_TARGET_GATEWAY:
                                config.setTargetGateway(pair[1]);
                                break;
                            case AppConstants.MACHINE_USER_NAME:
                                config.setUsername(pair[1]);
                                break;
                            case AppConstants.MACHINE_PASSWORD:
                                config.setPassword(pair[1]);
                                break;
                            case AppConstants.MACHINE_PASSCODE:
                                config.setPasscode(pair[1]);
                                break;
                            case AppConstants.MACHINE_TARGET_AGENT:
                                config.setTargetAgent(pair[1]);
                                break;
                            }
                        }
                    });

             boolean isAcceptable = machine.sendEvent(UserAction.CONFIG_COMMIT);
             log.info("CONFIG_COMMIT event is acceptable {} ", isAcceptable);
        };
    }
}
