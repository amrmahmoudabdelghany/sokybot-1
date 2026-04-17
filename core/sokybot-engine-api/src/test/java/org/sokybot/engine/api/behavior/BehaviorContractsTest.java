package org.sokybot.engine.api.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BehaviorContractsTest {

    @Test
    void behaviorDefaultsShouldBeStable() {
        IBehavior<String> behavior = new IBehavior<String>() {
            @Override
            public String id() {
                return "sample";
            }

            @Override
            public Class<String> settingsType() {
                return String.class;
            }

            @Override
            public boolean applies(org.sokybot.engine.api.workflow.IWorkflowContext context, String settings) {
                return true;
            }

            @Override
            public BehaviorStatus execute(org.sokybot.engine.api.workflow.IWorkflowContext context, String settings) {
                return BehaviorStatus.EXECUTED;
            }
        };

        assertEquals("sample", behavior.id());
        assertEquals(0, behavior.order());
        assertTrue(behavior.appliesTo("any"));
        assertEquals(0L, behavior.postDelayMs());
        assertFalse(behavior.canInterrupt());
        assertEquals(0, behavior.interruptionPriority());
    }

    @Test
    void specBuilderShouldEnforceMandatoryFields() {
        assertThrows(NullPointerException.class, () -> BehaviorCycleSpec.builder(null));

        assertThrows(IllegalArgumentException.class, () -> BehaviorCycleSpec.builder(String.class)
                .settingsSupplier(() -> "ok")
                .build());

        assertThrows(NullPointerException.class, () -> BehaviorCycleSpec.builder(String.class)
                .cycleId("training-cycle")
                .build());

        assertThrows(IllegalArgumentException.class, () -> BehaviorCycleSpec.builder(String.class)
                .cycleId("training-cycle")
                .settingsSupplier(() -> "ok")
                .defaultDelayMs(-1)
                .build());
    }

    @Test
    void specBuilderShouldCreateImmutableSpec() {
        BehaviorCycleSpec<String> spec = BehaviorCycleSpec.builder(String.class)
                .cycleId("training-cycle")
                .priority(300)
                .settingsSupplier(() -> "settings")
                .interruptible(true)
                .defaultDelayMs(250L)
                .build();

        assertNotNull(spec);
        assertEquals("training-cycle", spec.getCycleId());
        assertEquals(300, spec.getPriority());
        assertEquals(String.class, spec.getSettingsType());
        assertEquals("settings", spec.getSettingsSupplier().get());
        assertEquals(250L, spec.getDefaultDelayMs());
        assertTrue(spec.isInterruptible());
    }
}
