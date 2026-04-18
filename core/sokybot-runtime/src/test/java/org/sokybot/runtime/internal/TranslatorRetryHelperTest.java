package org.sokybot.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IPacketTranslator;

class TranslatorRetryHelperTest {

    @Test
    void resolveForFactoryReturnsNonEmptyWhenAvailableOnFirstAttempt() {
        ITranslatorRefreshable g = mock(ITranslatorRefreshable.class);
        IPacketTranslator t = mock(IPacketTranslator.class);
        Map<Integer, List<IPacketTranslator>> map = Collections.singletonMap(1, Collections.singletonList(t));
        when(g.getTranslators()).thenReturn(map);

        Map<Integer, List<IPacketTranslator>> out = TranslatorRetryHelper.resolveForFactory(g);

        assertFalse(out.isEmpty());
    }
}
