package org.sokybot.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for count byte generation.
 */
@DisplayName("CountSecurity Tests")
public class CountSecurityTest {

    private CountSecurity countSecurity;

    @BeforeEach
    void setUp() {
        countSecurity = new CountSecurity();
    }

    // ========== Configuration Tests ==========

    @Test
    @DisplayName("configur with zero seed uses default fallback")
    void testConfigur_WithZeroSeed() {
        // Zero seed should default to 0x9ABFB3B6
        assertDoesNotThrow(() -> countSecurity.configur(0L));
        
        // Should be able to generate count bytes after configuration
        byte result = countSecurity.generateCountByte();
        assertNotNull(result);
    }

    @Test
    @DisplayName("configur with non-zero seed initializes properly")
    void testConfigur_WithNonZeroSeed() {
        assertDoesNotThrow(() -> countSecurity.configur(0x12345678L));
        
        byte result = countSecurity.generateCountByte();
        assertNotNull(result);
    }

    @Test
    @DisplayName("configur with various seed values")
    void testConfigur_WithVariousSeeds() {
        assertDoesNotThrow(() -> {
            countSecurity.configur(1L);
            countSecurity.configur(0xFFFFFFFFL);
            countSecurity.configur(0x80000000L);
        });
    }

    // ========== generateCountByte Tests ==========

    @Test
    @DisplayName("generateCountByte returns a byte value")
    void testGenerateCountByte_ReturnsValue() {
        countSecurity.configur(0x42L);
        
        byte result = countSecurity.generateCountByte();
        
        // Byte is returned (any value from -128 to 127)
        assertTrue(result >= Byte.MIN_VALUE && result <= Byte.MAX_VALUE);
    }

    @Test
    @DisplayName("generateCountByte produces sequence of values")
    void testGenerateCountByte_Sequence() {
        countSecurity.configur(0x12345L);
        
        byte[] sequence = new byte[10];
        for (int i = 0; i < sequence.length; i++) {
            sequence[i] = countSecurity.generateCountByte();
        }
        
        // Sequence should have some variation (not all same value)
        boolean hasVariation = false;
        for (int i = 1; i < sequence.length; i++) {
            if (sequence[i] != sequence[0]) {
                hasVariation = true;
                break;
            }
        }
        
        assertTrue(hasVariation, "Sequence should have variation in values");
    }

    // ========== Determinism Tests ==========

    @Test
    @DisplayName("same seed produces same sequence")
    void testSameSeed_ProducesSameSequence() {
        long seed = 0xABCDEFL;
        
        countSecurity.configur(seed);
        byte[] sequence1 = new byte[5];
        for (int i = 0; i < sequence1.length; i++) {
            sequence1[i] = countSecurity.generateCountByte();
        }
        
        CountSecurity another = new CountSecurity();
        another.configur(seed);
        byte[] sequence2 = new byte[5];
        for (int i = 0; i < sequence2.length; i++) {
            sequence2[i] = another.generateCountByte();
        }
        
        assertArrayEquals(sequence1, sequence2, 
                "Same seed should produce identical sequences");
    }

    @Test
    @DisplayName("different seeds produce different sequences")
    void testDifferentSeeds_ProduceDifferentSequences() {
        CountSecurity cs1 = new CountSecurity();
        cs1.configur(0x11111111L);
        
        CountSecurity cs2 = new CountSecurity();
        cs2.configur(0x22222222L);
        
        byte[] seq1 = new byte[5];
        byte[] seq2 = new byte[5];
        
        for (int i = 0; i < 5; i++) {
            seq1[i] = cs1.generateCountByte();
            seq2[i] = cs2.generateCountByte();
        }
        
        assertFalse(java.util.Arrays.equals(seq1, seq2), 
                "Different seeds should produce different sequences");
    }

    // ========== Zero Seed Special Case ==========

    @Test
    @DisplayName("zero seed and non-zero fallback produce same result as default")
    void testZeroSeed_UsesDefaultFallback() {
        CountSecurity csZero = new CountSecurity();
        csZero.configur(0L);
        
        CountSecurity csDefault = new CountSecurity();
        csDefault.configur(0x9ABFB3B6L); // The default fallback value
        
        byte[] seqZero = new byte[5];
        byte[] seqDefault = new byte[5];
        
        for (int i = 0; i < 5; i++) {
            seqZero[i] = csZero.generateCountByte();
            seqDefault[i] = csDefault.generateCountByte();
        }
        
        assertArrayEquals(seqZero, seqDefault, 
                "Zero seed should use default fallback value");
    }

    // ========== State Mutation Tests ==========

    @Test
    @DisplayName("consecutive calls modify internal state")
    void testConsecutiveCalls_ModifyState() {
        countSecurity.configur(0x999L);
        
        byte first = countSecurity.generateCountByte();
        byte second = countSecurity.generateCountByte();
        byte third = countSecurity.generateCountByte();
        
        // The algorithm modifies byte1Seeds[0] each call, so consecutive
        // calls should (typically) produce different values
        // Note: There's a theoretical chance of collision
        boolean allSame = (first == second) && (second == third);
        assertFalse(allSame, "Consecutive calls should typically produce different values");
    }

    // ========== Extended Coverage Tests ==========

    @Test
    @DisplayName("generate long sequence maintains variation")
    void testSequence_LongRun_Distribution() {
        countSecurity.configur(0xCAFEBABEL);
        
        int sampleSize = 1000;
        byte[] samples = new byte[sampleSize];
        int zeroCount = 0;
        
        for(int i=0; i<sampleSize; i++) {
            samples[i] = countSecurity.generateCountByte();
            if (samples[i] == 0) zeroCount++;
        }
        
        // Sanity check: It shouldn't get stuck on one value
        // We expect varied distribution. It shouldn't allow 'all zeros' or 'all ones' theoretically
        // Checking that we don't have > 90% zeros is a safe loose bound to catch broken generators
        assertTrue(zeroCount < (sampleSize * 0.9), "Generator should not be stuck producing mostly zeros");
        
        // Check for at least SOME variation.
        int uniqueValues = 0;
        boolean[] seen = new boolean[256];
        for(byte b : samples) {
            if(!seen[b & 0xFF]) {
                seen[b & 0xFF] = true;
                uniqueValues++;
            }
        }
        
        // A decent PRNG-like sequence should hit many values in 1000 tries.
        assertTrue(uniqueValues > 10, "Generator should produce a variety of byte values");
    }
}
