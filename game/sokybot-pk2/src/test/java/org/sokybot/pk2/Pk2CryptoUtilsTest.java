package org.sokybot.pk2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Pk2CryptoUtilsTest {

    @Test
    public void testIsEncrypted_ValidEncryptedFooter() {
        byte[] buffer = new byte[30];
        // Set encrypted flags
        buffer[0] = (byte) 0xE2;
        buffer[1] = (byte) 0xB0;
        
        // Copy footer to end of buffer
        byte[] footer = Pk2CryptoUtils.ENCRYPTED_TEXT_FOOTER;
        System.arraycopy(footer, 0, buffer, buffer.length - footer.length, footer.length);
        
        assertTrue(Pk2CryptoUtils.isEncrepted(buffer), "Should detect valid encrypted buffer");
    }

    @Test
    public void testIsEncrypted_InvalidFlag() {
        byte[] buffer = new byte[30];
        buffer[0] = 0x00;
        buffer[1] = 0x00;
        // Correct footer but wrong flag
        byte[] footer = Pk2CryptoUtils.ENCRYPTED_TEXT_FOOTER;
        System.arraycopy(footer, 0, buffer, buffer.length - footer.length, footer.length);
        
        assertFalse(Pk2CryptoUtils.isEncrepted(buffer), "Should fail with invalid flag");
    }

    @Test
    public void testIsEncrypted_ShortBuffer() {
        byte[] buffer = new byte[10];
        assertFalse(Pk2CryptoUtils.isEncrepted(buffer), "Should fail with short buffer");
    }
    
    @Test
    public void testDecrypt_Buffer() {
         byte[] buffer = new byte[5];
         // Simple check that decrypt runs without error and modifies buffer or returns it
         byte[] decrypted = Pk2CryptoUtils.decrypt(buffer, Pk2CryptoUtils.INITIAL_BLOWFISH_KEY);
         assertNotNull(decrypted);
         assertEquals(5, decrypted.length);
    }

    @Test
    public void testIsEncrypted_NoFooter() {
        byte[] buffer = new byte[30];
        buffer[0] = (byte) 0xE2;
        buffer[1] = (byte) 0xB0;
        // No footer present (buffer is all zeros except first 2 bytes)
        assertFalse(Pk2CryptoUtils.isEncrepted(buffer), "Should fail when footer is missing");
    }

    @Test
    public void testIsEncrypted_FooterWrongFlag() {
        byte[] buffer = new byte[30];
        // Wrong flag
        buffer[0] = 0x00;
        buffer[1] = 0x00;
        
        // Add valid footer
        byte[] footer = Pk2CryptoUtils.ENCRYPTED_TEXT_FOOTER;
        System.arraycopy(footer, 0, buffer, buffer.length - footer.length, footer.length);
        
        assertFalse(Pk2CryptoUtils.isEncrepted(buffer), "Should fail when flag is wrong even if footer is present");
    }

    @Test
    public void testIsEncrypted_FooterOnly() {
        // Buffer exactly size of footer (missing 2 byte flag space effectively, or just too short)
        // Code checks > 24 bytes. Footer is 22 bytes.
        byte[] buffer = Pk2CryptoUtils.ENCRYPTED_TEXT_FOOTER;
        assertFalse(Pk2CryptoUtils.isEncrepted(buffer), "Should fail for buffer equal to footer size (too short)");
    }

    @Test
    public void testDecrypt_NonEncrypted() {
        byte[] buffer = new byte[30];
        // No flag, no footer
        buffer[0] = 0x00; 
        
        byte[] original = buffer.clone();
        byte[] result = Pk2CryptoUtils.decrypt(buffer);
        
        assertArrayEquals(original, result, "Should not modify non-encrypted buffer");
    }

    @Test
    public void testDecrypt_Encrypted() {
        byte[] buffer = new byte[30];
        // Set flag
        buffer[0] = (byte) 0xE2;
        buffer[1] = (byte) 0xB0;
        
        byte[] original = buffer.clone();
        byte[] result = Pk2CryptoUtils.decrypt(buffer);
        
        assertFalse(java.util.Arrays.equals(original, result), "Should modify encrypted buffer");
    }
}
