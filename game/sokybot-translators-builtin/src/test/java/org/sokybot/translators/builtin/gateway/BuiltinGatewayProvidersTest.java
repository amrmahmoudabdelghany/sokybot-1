package org.sokybot.translators.builtin.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IPacketTranslator;

class BuiltinGatewayProvidersTest {

    @Test
    void shouldExposeAgentListTranslatorAsFallback() {
        BuiltinAgentListTranslatorProvider provider = new BuiltinAgentListTranslatorProvider();

        assertEquals(25, provider.getPriority());
        assertEquals(java.util.Set.of(0xA101), provider.getSupportedOpcodes(null));
        IPacketTranslator translator = provider.createTranslator(0xA101, null);
        assertSame(GatewayAgentListTranslator.INSTANCE, translator);
        assertNull(provider.createTranslator(0x9999, null));
    }

    @Test
    void shouldExposeLoginResponseTranslatorAsFallback() {
        BuiltinLoginResponseTranslatorProvider provider = new BuiltinLoginResponseTranslatorProvider();

        assertEquals(25, provider.getPriority());
        assertEquals(java.util.Set.of(0xA102), provider.getSupportedOpcodes(null));
        IPacketTranslator translator = provider.createTranslator(0xA102, null);
        assertSame(GatewayLoginResponseTranslator.INSTANCE, translator);
        assertNull(provider.createTranslator(0x9999, null));
    }
}
