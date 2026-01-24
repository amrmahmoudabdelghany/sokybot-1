package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.stat.ExpUpdateEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for ExpUpdateTranslator.
 * Uses hex dump files for packet data.
 */
class ExpUpdateTranslatorTest extends TranslatorTestBase {

    private static final int EXP_SP_UPDATE_OPCODE = 0x3056;
    private ExpUpdateTranslator translator;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new ExpUpdateTranslator(createNullLookup());
    }

    @Test
    @DisplayName("Should parse experience update from hex dump")
    void testExpUpdate() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/stat/exp_update_0x3056.hex");

        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);

        ExpUpdateEvent event = assertSingleEvent(events, ExpUpdateEvent.class);
        assertEquals(10000L, event.getExpGained()); // 50000 - 40000
        assertEquals(50000, event.getTotalExp());
        assertEquals(TEST_MACHINE_NAME, event.getFullName());
        // Level up detection is simplified - defaults to false
        assertFalse(event.isLevelUp());
    }

    @Test
    @DisplayName("Should calculate exp gained correctly")
    void testExpGainedCalculation() {
        int[][] testCases = {
                { 50000, 40000, 10000 }, // Normal gain
                { 100000, 100000, 0 }, // No gain
                { 75000, 50000, 25000 }, // Large gain
                { 1000, 5000, -4000 } // Negative (shouldn't happen but test edge case)
        };

        for (int[] testCase : testCases) {
            int currentExp = testCase[0];
            int previousExp = testCase[1];
            long expectedGained = testCase[2];

            var packet = packetBuilder()
                    .putInt(12345)
                    .putInt(currentExp)
                    .putInt(previousExp)
                    .putInt(1000)
                    .putInt(800)
                    .build();

            List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);

            ExpUpdateEvent event = assertSingleEvent(events, ExpUpdateEvent.class);
            assertEquals(expectedGained, event.getExpGained());
            assertEquals(currentExp, event.getTotalExp());
        }
    }

    @Test
    @DisplayName("Should handle zero experience gain")
    void testZeroExpGain() {
        var packet = packetBuilder()
                .putInt(12345)
                .putInt(50000) // currentExp
                .putInt(50000) // previousExp (same, no gain)
                .putInt(1000)
                .putInt(800)
                .build();

        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);

        ExpUpdateEvent event = assertSingleEvent(events, ExpUpdateEvent.class);
        assertEquals(0, event.getExpGained());
        assertEquals(50000, event.getTotalExp());
    }

    @Test
    @DisplayName("Should return empty list on malformed packet")
    void testMalformedPacket() {
        var packet = createPacket(new byte[] { 0x01, 0x02, 0x03 }); // Too short

        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);

        assertNoEvents(events);
    }

    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(EXP_SP_UPDATE_OPCODE, translator.getOpcode());
    }
}
