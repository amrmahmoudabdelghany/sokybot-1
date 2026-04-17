package org.sokybot.engine.api.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CycleIdTest {

    @Test
    void shouldNormalizeAndCompareByCanonicalString() {
        CycleId left = CycleId.of("  training-cycle  ");
        CycleId right = CycleId.of("training-cycle");

        assertEquals("training-cycle", left.asString());
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    void shouldRejectNullOrBlankInputs() {
        assertThrows(NullPointerException.class, () -> CycleId.of(null));
        assertThrows(IllegalArgumentException.class, () -> CycleId.of("   "));
    }

    @Test
    void qualifyShouldCreateCycleQualifiedState() {
        CycleId cycleId = CycleId.of("login-cycle");
        StateId stateId = StateId.of("CONNECT");

        assertEquals(StateId.of("login-cycle.CONNECT"), cycleId.qualify(stateId));
    }
}
