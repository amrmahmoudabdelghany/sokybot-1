package org.sokybot.gameevents.internal.gateway;

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.sokybot.gameevents.GatewayLoginResponseTranslator;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.events.core.ITranslatorProvider;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Registers {@link GatewayLoginResponseTranslator} with high priority for opcode 0xA102.
 */
@Component(service = ITranslatorProvider.class, immediate = true, property = "type=built-in-gateway")
public class BuiltinLoginResponseTranslatorProvider implements ITranslatorProvider {

    private static final int OPCODE = 0xA102;

    @Override
    public boolean supports(int opcode, IGameDataLookup lookup) {
        return opcode == OPCODE;
    }

    @Override
    public Set<Integer> getSupportedOpcodes() {
        return Set.of(OPCODE);
    }

    @Override
    public IPacketTranslator createTranslator(int opcode, IGameDataLookup lookup) {
        if (opcode != OPCODE) {
            return null;
        }
        return GatewayLoginResponseTranslator.INSTANCE;
    }

    @Override
    public int getPriority() {
        return 200;
    }
}
