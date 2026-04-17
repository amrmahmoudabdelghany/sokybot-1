package org.sokybot.translators.builtin.gateway;

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.events.core.ITranslatorProvider;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Registers {@link GatewayAgentListTranslator} with fallback priority for opcode 0xA101.
 */
@Component(service = ITranslatorProvider.class, immediate = true, property = "type=built-in-gateway")
public class BuiltinAgentListTranslatorProvider implements ITranslatorProvider {

    private static final int OPCODE = 0xA101;

    @Override
    public boolean supports(int opcode, IGameDataLookup lookup) {
        return opcode == OPCODE;
    }

    @Override
    public Set<Integer> getSupportedOpcodes(IGameDataLookup lookup) {
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
        return 25;
    }
}
