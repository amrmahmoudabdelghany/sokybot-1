package org.sokybot.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the Blowfish encryption/decryption implementation.
 */
@DisplayName("Blowfish Tests")
public class BlowfishTest {

    private static final byte[] TEST_KEY = "TestKey123".getBytes();
    private static final byte[] ALTERNATE_KEY = "AltKey789".getBytes();
    
    private IBlowfish blowfish;

    @BeforeEach
    void setUp() {
        blowfish = Blowfish.newInstance(TEST_KEY);
    }

    // ========== Factory Method Tests ==========

    @Test
    @DisplayName("newInstance() returns non-null IBlowfish")
    void testNewInstance_ReturnsNonNull() {
        IBlowfish instance = Blowfish.newInstance();
        assertNotNull(instance, "Factory should return non-null instance");
    }

    @Test
    @DisplayName("newInstance(key) returns configured IBlowfish")
    void testNewInstanceWithKey_ReturnsConfigured() {
        IBlowfish instance = Blowfish.newInstance(TEST_KEY);
        assertNotNull(instance, "Factory with key should return non-null instance");
        
        // Verify it's properly configured by encoding some data
        byte[] testData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        byte[] encoded = instance.encode(0, testData.clone());
        assertNotNull(encoded, "Configured instance should be able to encode");
    }

    // ========== Encode/Decode Roundtrip Tests ==========

    @Test
    @DisplayName("encode then decode returns original data (8-byte aligned)")
    void testEncodeDecodeCycle_ReturnsOriginal() {
        byte[] original = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        byte[] data = original.clone();
        
        byte[] encoded = blowfish.encode(0, data);
        byte[] decoded = blowfish.decode(0, encoded);
        
        assertArrayEquals(original, decoded, "Roundtrip should preserve original data");
    }

    @Test
    @DisplayName("encode then decode returns original data (multiple blocks)")
    void testEncodeDecodeCycle_MultipleBlocks() {
        byte[] original = new byte[24]; // 3 blocks of 8 bytes
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (i + 1);
        }
        byte[] data = original.clone();
        
        byte[] encoded = blowfish.encode(0, data);
        byte[] decoded = blowfish.decode(0, encoded);
        
        assertArrayEquals(original, decoded, "Roundtrip should preserve multi-block data");
    }

    // ========== Encoding Behavior Tests ==========

    @Test
    @DisplayName("encode modifies the input data")
    void testEncode_ModifiesData() {
        byte[] original = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        byte[] data = original.clone();
        
        byte[] encoded = blowfish.encode(0, data);
        
        assertFalse(java.util.Arrays.equals(original, encoded), 
                "Encoded data should differ from original");
    }

    @Test
    @DisplayName("decode modifies encrypted data")
    void testDecode_ModifiesData() {
        byte[] original = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        byte[] encoded = blowfish.encode(0, original.clone());
        byte[] encodedCopy = encoded.clone();
        
        byte[] decoded = blowfish.decode(0, encoded);
        
        assertFalse(java.util.Arrays.equals(encodedCopy, decoded), 
                "Decoded data should differ from encrypted");
    }

    @Test
    @DisplayName("different keys produce different ciphertext")
    void testDifferentKeys_ProduceDifferentOutput() {
        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        
        IBlowfish bf1 = Blowfish.newInstance(TEST_KEY);
        IBlowfish bf2 = Blowfish.newInstance(ALTERNATE_KEY);
        
        byte[] encoded1 = bf1.encode(0, data.clone());
        byte[] encoded2 = bf2.encode(0, data.clone());
        
        assertFalse(java.util.Arrays.equals(encoded1, encoded2), 
                "Different keys should produce different ciphertext");
    }

    // ========== Partial Block / Padding Tests ==========

    @Test
    @DisplayName("partial block data is padded and handled")
    void testPartialBlock_IsPadded() {
        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05}; // 5 bytes, not aligned
        
        byte[] encoded = blowfish.encode(0, data.clone());
        
        assertNotNull(encoded, "Encoded partial block should not be null");
        assertEquals(0, encoded.length % 8, "Output should be 8-byte aligned");
    }

    @Test
    @DisplayName("multiple blocks are processed correctly")
    void testMultipleBlocks_ProcessedCorrectly() {
        byte[] data = new byte[32]; // 4 blocks
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        byte[] original = data.clone();
        
        byte[] encoded = blowfish.encode(0, data);
        
        assertNotNull(encoded, "Encoded multi-block data should not be null");
        assertFalse(java.util.Arrays.equals(original, encoded), 
                "All blocks should be encrypted");
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("empty data is handled gracefully")
    void testEmptyData_HandledGracefully() {
        byte[] data = new byte[0];
        
        byte[] encoded = blowfish.encode(0, data);
        
        assertNotNull(encoded, "Empty data encode should not return null");
    }

    @Test
    @DisplayName("encode with offset processes correctly")
    void testEncodeWithOffset() {
        byte[] data = new byte[16];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        byte[] original = data.clone();
        
        // Encode starting at offset 8
        byte[] encoded = blowfish.encode(8, data);
        
        assertNotNull(encoded, "Encoded data with offset should not be null");
        // First 8 bytes should remain unchanged
        for (int i = 0; i < 8; i++) {
            assertEquals(original[i], encoded[i], "Data before offset should be unchanged");
        }
    }

    // ========== Extended Coverage Tests ==========

    @Test
    @DisplayName("transformBlock throws IllegalStateException when uninitialized")
    void testUninitialized_ThrowsException() {
        Blowfish uninit = new Blowfish(); // Use raw class to access transformBlock if it were public, or use interface if exposed
        // Note: transformBlock is public in Blowfish implementation but not in IBlowfish interface.
        // We cast to access it for white-box testing of the state check.
        
        assertThrows(IllegalStateException.class, () -> {
            uninit.transformBlock(new byte[8], 0, new byte[8], 0);
        }, "Should throw IllegalStateException if init/configur never called");
    }

    @Test
    @DisplayName("verify output length padding calculation")
    void testOutputLength_Calculation() {
        // Blowfish pads to 8 byte blocks.
        // Logic: Length + (8 - length%8) if length%8 != 0
        
        // Case 1: 5 bytes -> should become 5 + (8 - 5) = 8 bytes
        byte[] in5 = new byte[5];
        byte[] out5 = blowfish.encode(0, in5);
        assertEquals(8, out5.length);

        // Case 2: 8 bytes -> should check implementation behavior. 
        // Existing code: if %8==0 return data (same length).
        byte[] in8 = new byte[8];
        byte[] out8 = blowfish.encode(0, in8);
        assertEquals(8, out8.length);
        
        // Case 3: 9 bytes -> should become 9 + (8 - 1) = 16 bytes
        byte[] in9 = new byte[9];
        byte[] out9 = blowfish.encode(0, in9);
        assertEquals(16, out9.length);
    }

    @Test
    @DisplayName("long key is handled correctly")
    void testLongKey_HandledCorrectly() {
        // Blowfish supports variable key lengths up to 448 bits (56 bytes)
        // Implementation might handle longer keys by cycling or just taking what's given.
        // Let's test a 64-byte key.
        byte[] longKey = new byte[64];
        for (int i=0; i<64; i++) longKey[i] = (byte)i;
        
        IBlowfish longKeyInstance = Blowfish.newInstance(longKey);
        byte[] data = new byte[]{1,2,3,4,5,6,7,8};
        
        byte[] original = data.clone();
        
        // Should not crash
        byte[] result = longKeyInstance.encode(0, data);
        assertNotNull(result);
        assertFalse(java.util.Arrays.equals(original, result));
    }
}
