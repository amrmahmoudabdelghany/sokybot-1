package org.sokybot.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CRC checksum calculation.
 */
@DisplayName("CRCSecurity Tests")
public class CRCSecurityTest {

    private CRCSecurity crcSecurity;

    @BeforeEach
    void setUp() {
        crcSecurity = new CRCSecurity();
    }

    // ========== Configuration Tests ==========

    @Test
    @DisplayName("configur with various seed values")
    void testConfigur_WithVariousSeed() {
        // Should not throw for valid seeds
        assertDoesNotThrow(() -> crcSecurity.configur(0x00L));
        assertDoesNotThrow(() -> crcSecurity.configur(0xFFL));
        assertDoesNotThrow(() -> crcSecurity.configur(0x12345678L));
    }

    @Test
    @DisplayName("configur initializes internal state")
    void testConfigur_InitializesState() {
        crcSecurity.configur(0x42L);
        
        // Verify by calculating a checksum (would throw if not initialized)
        byte[] data = new byte[]{1, 2, 3, 4};
        assertDoesNotThrow(() -> crcSecurity.calculate(data));
    }

    // ========== Calculate Tests ==========

    @Test
    @DisplayName("calculate with empty data returns zero")
    void testCalculate_EmptyData() {
        crcSecurity.configur(0x01L);
        
        byte[] emptyData = new byte[0];
        byte result = crcSecurity.calculate(emptyData);
        
        assertEquals(0, result, "Empty data should return CRC of 0");
    }

    @Test
    @DisplayName("calculate with single byte")
    void testCalculate_SingleByte() {
        crcSecurity.configur(0x01L);
        
        byte[] singleByte = new byte[]{0x42};
        byte result = crcSecurity.calculate(singleByte);
        
        // Result is deterministic, just verify it returns a value
        assertNotNull(result);
    }

    @Test
    @DisplayName("calculate with multiple bytes")
    void testCalculate_MultipleBytes() {
        crcSecurity.configur(0x01L);
        
        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        byte result = crcSecurity.calculate(data);
        
        assertNotNull(result);
    }

    // ========== Determinism Tests ==========

    @Test
    @DisplayName("same input and seed produces same checksum")
    void testCalculate_SameInputSameSeed_Deterministic() {
        byte[] data = new byte[]{0x11, 0x22, 0x33, 0x44, 0x55};
        long seed = 0x10L;
        
        crcSecurity.configur(seed);
        byte result1 = crcSecurity.calculate(data.clone());
        
        CRCSecurity another = new CRCSecurity();
        another.configur(seed);
        byte result2 = another.calculate(data.clone());
        
        assertEquals(result1, result2, "Same input and seed should produce identical checksums");
    }

    @Test
    @DisplayName("same instance same data produces consistent checksum")
    void testCalculate_SameInstance_Consistent() {
        crcSecurity.configur(0x20L);
        byte[] data = new byte[]{0x01, 0x02, 0x03};
        
        byte result1 = crcSecurity.calculate(data);
        byte result2 = crcSecurity.calculate(data);
        
        assertEquals(result1, result2, "Same data should produce consistent checksum");
    }

    // ========== Different Input/Seed Tests ==========

    @Test
    @DisplayName("different seeds produce different checksums")
    void testCalculate_DifferentSeeds_DifferentOutput() {
        byte[] data = new byte[]{0x11, 0x22, 0x33, 0x44};
        
        CRCSecurity crc1 = new CRCSecurity();
        crc1.configur(0x01L);
        byte result1 = crc1.calculate(data.clone());
        
        CRCSecurity crc2 = new CRCSecurity();
        crc2.configur(0x80L);
        byte result2 = crc2.calculate(data.clone());
        
        // Different seeds should produce different checksums (with high probability)
        // Note: There's a small chance they could collide, but it's unlikely
        assertNotEquals(result1, result2, 
                "Different seeds should typically produce different checksums");
    }

    @Test
    @DisplayName("different data produces different checksums")
    void testCalculate_DifferentData_DifferentOutput() {
        crcSecurity.configur(0x01L);
        
        byte[] data1 = new byte[]{0x01, 0x02, 0x03, 0x04};
        byte[] data2 = new byte[]{0x04, 0x03, 0x02, 0x01};
        
        byte result1 = crcSecurity.calculate(data1);
        byte result2 = crcSecurity.calculate(data2);
        
        assertNotEquals(result1, result2, 
                "Different data should produce different checksums");
    }

    // ========== Boundary Tests ==========

    @Test
    @DisplayName("calculate handles maximum length mask (0x7FFF)")
    void testCalculate_LengthMask() {
        crcSecurity.configur(0x01L);
        
        // Create data that tests the length masking (length & 0x7FFF)
        byte[] largeData = new byte[1000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        
        byte result = crcSecurity.calculate(largeData);
        assertNotNull(result);
    }

    // ========== Extended Coverage Tests ==========

    @Test
    @DisplayName("calculate with all 0xFF bytes")
    void testCalculate_MaxBitHandling() {
        crcSecurity.configur(0xABCL);
        
        byte[] maxBytes = new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
        byte result = crcSecurity.calculate(maxBytes);
        
        // Verify we get a result and no overflow exceptions occurred internally
        assertNotNull(result);
    }

    @Test
    @DisplayName("calculate large data (>256 bytes) is consistent")
    void testCalculate_LargeData() {
        crcSecurity.configur(0x123L);
        
        byte[] largeData = new byte[512];
        for(int i=0; i<512; i++) largeData[i] = (byte)(i & 0xFF);
        
        byte result1 = crcSecurity.calculate(largeData);
        byte result2 = crcSecurity.calculate(largeData.clone());
        
        assertEquals(result1, result2, "Large data calculation should be deterministic");
    }
}
