package org.sokybot.runtime.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sokybot.commons.lifecycle.ISubscriptionScope;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gamemodel.spi.IGameModelMutator;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;

class TranslatorBridgeWirerTest {

    @Test
    void subscribesOncePerTranslator() {
        ChunkedPacketManager chunkManager = new ChunkedPacketManager();
        IGameModelMutator mutator = mock(IGameModelMutator.class);
        ISubscriptionScope scope = mock(ISubscriptionScope.class);
        IPacketPublisher publisher = mock(IPacketPublisher.class);
        IPacketTranslator translator = mock(IPacketTranslator.class);
        when(translator.getOpcode()).thenReturn(0x01);
        IPacketSubscription sub = mock(IPacketSubscription.class);
        when(publisher.subscribe(any(), eq(0x01))).thenReturn(sub);

        TranslatorBridgeWirer wirer = new TranslatorBridgeWirer(chunkManager, mutator, scope);
        Map<Integer, java.util.List<IPacketTranslator>> map = new HashMap<>();
        map.put(Integer.valueOf(1), Collections.singletonList(translator));

        wirer.wireTranslatorChains(publisher, map, "g.m", null, null);

        verify(publisher).subscribe(any(), eq(0x01));
        verify(scope).register(sub);
    }
}
