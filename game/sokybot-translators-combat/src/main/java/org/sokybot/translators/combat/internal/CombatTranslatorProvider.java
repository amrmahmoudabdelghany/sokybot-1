package org.sokybot.translators.combat.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.events.core.ITranslatorProvider;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Placeholder provider for future combat-only opcodes; core/builtin translators continue to publish world events.
 */
@Component(service = ITranslatorProvider.class, immediate = true, property = {
        "type=combat",
        "service.ranking:Integer=50"
})
public final class CombatTranslatorProvider implements ITranslatorProvider {

    @Override
    public boolean supports(int opcode, IGameDataLookup lookup) {
        return false;
    }

    @Override
    public IPacketTranslator createTranslator(int opcode, IGameDataLookup lookup) {
        return null;
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
