package org.sokybot.gameevents.internal.gateway;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.sokybot.gameevents.GatewayAgentListTranslator;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.events.core.ITranslatorProvider;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Registers {@link GatewayAgentListTranslator} with high priority for opcode 0xA101.
 */
@Component(service = ITranslatorProvider.class, immediate = true, property = "type=built-in-gateway")
public class BuiltinAgentListTranslatorProvider implements ITranslatorProvider {

    private static final int OPCODE = 0xA101;

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
        return GatewayAgentListTranslator.INSTANCE;
    }

    @Override
    public int getPriority() {
        return 200;
    }
}
