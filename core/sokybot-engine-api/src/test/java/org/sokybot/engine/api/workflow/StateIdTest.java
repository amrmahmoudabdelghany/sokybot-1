package org.sokybot.engine.api.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StateIdTest {

    @Test
    void shouldNormalizeAndCompareByCanonicalString() {
        StateId left = StateId.of("  LOGIN.STATE  ");
        StateId right = StateId.of("LOGIN.STATE");

        assertEquals("LOGIN.STATE", left.asString());
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    void shouldRejectNullOrBlankInputs() {
        assertThrows(NullPointerException.class, () -> StateId.of(null));
        assertThrows(IllegalArgumentException.class, () -> StateId.of("   "));
    }

    @Test
    void ofNullableShouldReturnNullForBlank() {
        assertNull(StateId.ofNullable(null));
        assertNull(StateId.ofNullable("   "));
        assertEquals(StateId.of("A"), StateId.ofNullable(" A "));
    }
}
