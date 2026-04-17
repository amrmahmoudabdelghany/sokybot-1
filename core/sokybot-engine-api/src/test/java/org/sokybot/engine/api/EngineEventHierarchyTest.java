package org.sokybot.engine.api;

import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.event.Connect;
import org.sokybot.engine.api.event.Disconnect;
import org.sokybot.engine.api.event.PartyIntent;
import org.sokybot.engine.api.event.StartTraining;
import org.sokybot.engine.api.event.StopTraining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineEventHierarchyTest {

    @Test
    void shouldExposeExpectedTypeForEachConcreteEvent() {
        assertEquals("START_TRAINING", StartTraining.INSTANCE.type());
        assertEquals("STOP_TRAINING", StopTraining.INSTANCE.type());
        assertEquals("CONNECT", Connect.INSTANCE.type());
        assertEquals("DISCONNECT", Disconnect.INSTANCE.type());
        assertEquals("PARTY_INTENT", new PartyIntent("g1", PartyIntent.IntentType.INVITE, "m1").type());
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedSingletonsShouldPointToTypedSingletons() {
        assertEquals(StartTraining.class, EngineEvent.START_TRAINING.getClass());
        assertEquals(StopTraining.class, EngineEvent.STOP_TRAINING.getClass());
        assertEquals(Connect.class, EngineEvent.CONNECT.getClass());
        assertEquals(Disconnect.class, EngineEvent.DISCONNECT.getClass());
    }

    @Test
    void defaultPayloadShouldBeEmptyForTypedEvents() {
        assertTrue(StartTraining.INSTANCE.payload().isEmpty());
        assertTrue(Connect.INSTANCE.payload().isEmpty());
        assertTrue(new PartyIntent("g1", PartyIntent.IntentType.ACCEPT, null).payload().isEmpty());
    }
}
