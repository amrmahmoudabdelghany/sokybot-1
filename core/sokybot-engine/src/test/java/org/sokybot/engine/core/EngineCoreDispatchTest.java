package org.sokybot.engine.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.EngineEvent;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.event.Connect;
import org.sokybot.engine.api.event.PartyIntent;
import org.sokybot.engine.api.event.Wake;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.test.EngineTestBase;
import org.sokybot.engine.test.util.WorkflowTestBuilders;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EngineCore Dispatch Tests")
class EngineCoreDispatchTest extends EngineTestBase {

    private EngineCore engine;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        engine = createTestEngine();
    }

    @Test
    @DisplayName("dispatch(Connect) should mimic legacy sendEvent(CONNECT)")
    @SuppressWarnings("deprecation")
    void dispatchConnectShouldMimicLegacySendEvent() {
        registerLoginCycle();
        engine.start();

        assertDoesNotThrow(() -> engine.dispatch(Connect.INSTANCE));
        assertTrue(engine.getWorkflowRegistry().isCycleEnabled("login-cycle"));

        // legacy path still delegates and should keep the same behavior
        assertDoesNotThrow(() -> engine.sendEvent(EngineEvent.CONNECT));
        assertTrue(engine.getWorkflowRegistry().isCycleEnabled("login-cycle"));
    }

    @Test
    @DisplayName("dispatch(Wake.INSTANCE) should mimic legacy wakeWorkflow()")
    void dispatchWakeShouldMimicLegacyWakeWorkflow() {
        registerLoginCycle();
        engine.start();

        assertDoesNotThrow(() -> engine.dispatch(Wake.INSTANCE));
        assertDoesNotThrow(() -> engine.wakeWorkflow());
        assertEquals(EngineState.IDLE, engine.getEngineState());
    }

    @Test
    @DisplayName("dispatch(PartyIntent) should trigger mediator callback")
    void dispatchPartyIntentShouldTriggerMediator() {
        registerLoginCycle();
        engine.start();
        AtomicReference<PartyIntent> captured = new AtomicReference<>();
        engine.setPartyIntentMediator(captured::set);

        PartyIntent intent = new PartyIntent("alpha", PartyIntent.IntentType.INVITE, "m2");
        engine.dispatch(intent);

        assertSame(intent, captured.get());
    }

    private void registerLoginCycle() {
        ICycleDefinition loginCycle = WorkflowTestBuilders.cycle("login-cycle")
                .priority(10)
                .entryState("S1")
                .state("S1",
                        WorkflowTestBuilders.guard().returns(true).build(),
                        WorkflowTestBuilders.action().build(),
                        null)
                .build();
        engine.getWorkflowRegistry().registerCycle(loginCycle);
    }
}
