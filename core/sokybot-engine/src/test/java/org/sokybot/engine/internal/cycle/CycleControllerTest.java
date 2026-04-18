package org.sokybot.engine.internal.cycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;

class CycleControllerTest {

    @Test
    void reconcileAfterAuthenticationEnablesTrainingWhenDesired() {
        IWorkflowRegistry registry = mock(IWorkflowRegistry.class);
        CycleController c = new CycleController(registry);
        c.setEngineState(EngineState.IDLE);
        c.setDesiredModeTraining();

        c.reconcileAfterAuthentication();

        verify(registry).setCycleEnabled("training-cycle", true);
        assertEquals(EngineState.ACTIVE, c.getEngineState());
    }

    @Test
    void getActiveActivitiesReflectsRegistry() {
        IWorkflowRegistry registry = mock(IWorkflowRegistry.class);
        when(registry.isCycleEnabled("training-cycle")).thenReturn(false);
        when(registry.isCycleEnabled("login-cycle")).thenReturn(true);
        CycleController c = new CycleController(registry);

        assertTrue(c.getActiveActivities().contains("LOGIN"));
        assertFalse(c.getActiveActivities().contains("TRAINING"));
    }
}
