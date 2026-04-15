package org.sokybot.pk2extractor.mediapk2.gameinfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2extractor.dto.gameinfo.DivisionInfoData;
import org.sokybot.pk2extractor.dto.gameinfo.DivisionData;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.security.Blowfish;
import org.sokybot.security.IBlowfish;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Unit tests for GameInfoExtractor.
 * 
 * These tests verify that the token-based parsing logic correctly handles
 * legacy null-terminated strings from divisioninfo.txt, protecting against
 * regressions.
 */
public class GameInfoExtractorTest {

    @Test
    public void testParseDivisionInfo_BinaryFormat() {
        // [Locale:1][DivCount:1][NameLen:4][Name][00:1][HostCount:1][HostLen:4][Host][00:1]
        // Locale: 0x01, DivCount: 0x01
        // NameLen: 4 ("DIV1" = 4)
        // HostCount: 0x01
        // HostLen: 9 ("127.0.0.1" = 9)

        ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0x01); // Locale
        buffer.put((byte) 0x01); // DivCount

        // Division 1
        buffer.putInt(4); // NameLen
        buffer.put("DIV1".getBytes(StandardCharsets.ISO_8859_1));
        buffer.put((byte) 0x00); // Null terminator

        buffer.put((byte) 0x01); // HostCount
        buffer.putInt(9); // HostLen
        buffer.put("127.0.0.1".getBytes(StandardCharsets.ISO_8859_1));
        buffer.put((byte) 0x00); // Null terminator

        byte[] data = new byte[buffer.position()];
        buffer.flip();
        buffer.get(data);

        GameInfoExtractor extractor = new GameInfoExtractor();
        DivisionInfoData result = extractor.parseDivisionInfo(data);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, (int) result.getLocal());
        Assertions.assertEquals(1, result.getDivisions().size());
        Assertions.assertEquals("DIV1", result.getDivisions().get(0).getName());
        Assertions.assertEquals("127.0.0.1", result.getDivisions().get(0).getHosts().get(0));
    }

    @Test
    public void testParseDivisionInfo_DNSHostWithPort() {
        // Text format with DNS and Port: "L\0DIV1\0\0gwgt1.joymax.com:15780\0"
        String simulatedContent = "L\u0000DIV1\u0000\u0000gwgt1.joymax.com:15780\u0000";
        byte[] data = simulatedContent.getBytes(StandardCharsets.ISO_8859_1);

        GameInfoExtractor extractor = new GameInfoExtractor();
        DivisionInfoData result = extractor.parseDivisionInfo(data);

        Assertions.assertNotNull(result);
        DivisionData div = result.getDivisions().get(0);
        Assertions.assertEquals("gwgt1.joymax.com:15780", div.getHosts().get(0));
    }

    @Test
    public void testParseDivisionInfo_SingleDivisionSingleHost() {
        // Simulated legacy content: Locale byte, DivisionName, some ID, HostCount, IP,
        // terminator
        // Format: "L\0DivisionName\0\0IP\0"
        String simulatedContent = "L\u0000DIV1\u0000\u0000192.168.1.1\u0000";
        byte[] data = simulatedContent.getBytes(StandardCharsets.ISO_8859_1);

        GameInfoExtractor extractor = new GameInfoExtractor();
        DivisionInfoData result = extractor.parseDivisionInfo(data);

        Assertions.assertNotNull(result, "DivisionInfoData should not be null");
        Assertions.assertEquals('L', (char) result.getLocal(), "Locale should match first byte");

        List<DivisionData> divisions = result.getDivisions();
        Assertions.assertNotNull(divisions, "Divisions list should not be null");
        Assertions.assertEquals(1, divisions.size(), "Should have exactly 1 division");

        DivisionData div = divisions.get(0);
        Assertions.assertEquals("DIV1", div.getName(), "Division name should be DIV1");
        Assertions.assertEquals(1, div.getHosts().size(), "Division should have 1 host");
        Assertions.assertEquals("192.168.1.1", div.getHosts().get(0), "Host IP should match");
    }

    @Test
    public void testParseDivisionInfo_MultipleDivisionsMultipleHosts() {
        // Simulate two divisions with multiple hosts each
        String simulatedContent = "E\u0000FirstDiv\u0000\u00001.2.3.4\u00005.6.7.8\u0000\u0000SecondDiv\u0000\u000010.0.0.1\u0000";
        byte[] data = simulatedContent.getBytes(StandardCharsets.ISO_8859_1);

        GameInfoExtractor extractor = new GameInfoExtractor();
        DivisionInfoData result = extractor.parseDivisionInfo(data);

        Assertions.assertNotNull(result);
        List<DivisionData> divisions = result.getDivisions();
        Assertions.assertEquals(2, divisions.size(), "Should have 2 divisions");

        // First division
        DivisionData firstDiv = divisions.stream()
                .filter(d -> "FirstDiv".equals(d.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(firstDiv, "FirstDiv should exist");
        Assertions.assertEquals(2, firstDiv.getHosts().size(), "FirstDiv should have 2 hosts");
        Assertions.assertTrue(firstDiv.getHosts().contains("1.2.3.4"));
        Assertions.assertTrue(firstDiv.getHosts().contains("5.6.7.8"));

        // Second division
        DivisionData secondDiv = divisions.stream()
                .filter(d -> "SecondDiv".equals(d.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(secondDiv, "SecondDiv should exist");
        Assertions.assertEquals(1, secondDiv.getHosts().size(), "SecondDiv should have 1 host");
        Assertions.assertEquals("10.0.0.1", secondDiv.getHosts().get(0));
    }

    @Test
    public void testParseDivisionInfo_EmptyData() {
        byte[] data = new byte[0];

        GameInfoExtractor extractor = new GameInfoExtractor();
        DivisionInfoData result = extractor.parseDivisionInfo(data);

        // Empty data should result in null or empty divisions
        // The method returns null if tokens.length == 0
        Assertions.assertNull(result, "Result should be null for empty data");
    }

    @Test
    public void testParseDivisionInfo_NoIps() {
        // Content with only text, no IPs
        String simulatedContent = "L\u0000SomeText\u0000AnotherText\u0000";
        byte[] data = simulatedContent.getBytes(StandardCharsets.ISO_8859_1);

        GameInfoExtractor extractor = new GameInfoExtractor();
        DivisionInfoData result = extractor.parseDivisionInfo(data);

        Assertions.assertNotNull(result);
        List<DivisionData> divisions = result.getDivisions();
        Assertions.assertTrue(divisions.isEmpty(), "Should have no divisions if no IPs found");
    }

    /**
     * Test Blowfish decryption with key "SILKROAD" (original key).
     * This tests if "SILKROAD" key correctly decrypts the version.
     */
    @Test
    public void testVersionDecryption_WithSilkroadKey() {
        // Create test version data
        String expectedVersion = "12345";
        byte[] versionBytes = expectedVersion.getBytes(StandardCharsets.US_ASCII);

        // Pad to 8-byte block (Blowfish block size)
        byte[] paddedVersion = new byte[8];
        System.arraycopy(versionBytes, 0, paddedVersion, 0, versionBytes.length);

        // Encrypt with "SILKROAD" key
        IBlowfish blowfishEncoder = Blowfish.newInstance("SILKROAD".getBytes());
        byte[] encrypted = blowfishEncoder.encode(0, paddedVersion);

        // Now decrypt and verify
        IBlowfish blowfishDecoder = Blowfish.newInstance("SILKROAD".getBytes());
        byte[] decrypted = blowfishDecoder.decode(0, encrypted);

        String result = new String(decrypted, StandardCharsets.US_ASCII).trim();
        System.out.println("[SILKROAD] Decrypted version: '" + result + "'");

        Assertions.assertEquals(expectedVersion, result, "Decrypted version should match original");
    }

    /**
     * Test Blowfish decryption with key "SILKROADVERSION" (alternative key).
     * This tests if "SILKROADVERSION" key correctly decrypts the version.
     */
    @Test
    public void testVersionDecryption_WithSilkroadVersionKey() {
        // Create test version data
        String expectedVersion = "12345";
        byte[] versionBytes = expectedVersion.getBytes(StandardCharsets.US_ASCII);

        // Pad to 8-byte block (Blowfish block size)
        byte[] paddedVersion = new byte[8];
        System.arraycopy(versionBytes, 0, paddedVersion, 0, versionBytes.length);

        // Encrypt with "SILKROADVERSION" key
        IBlowfish blowfishEncoder = Blowfish.newInstance("SILKROADVERSION".getBytes());
        byte[] encrypted = blowfishEncoder.encode(0, paddedVersion);

        // Now decrypt and verify
        IBlowfish blowfishDecoder = Blowfish.newInstance("SILKROADVERSION".getBytes());
        byte[] decrypted = blowfishDecoder.decode(0, encrypted);

        String result = new String(decrypted, StandardCharsets.US_ASCII).trim();
        System.out.println("[SILKROADVERSION] Decrypted version: '" + result + "'");

        Assertions.assertEquals(expectedVersion, result, "Decrypted version should match original");
    }

    /**
     * Test toInteger parsing with various inputs.
     */
    @Test
    public void testToInteger_ValidNumbers() {
        Assertions.assertEquals(12345, Pk2ExtractorUtils.toInteger("12345"));
        Assertions.assertEquals(0, Pk2ExtractorUtils.toInteger("0"));
        Assertions.assertEquals(999, Pk2ExtractorUtils.toInteger("999"));
    }

    /**
     * Test what happens when parsing version with null bytes.
     */
    @Test
    public void testVersionParsing_WithNullBytes() {
        // Simulate decrypted data that may have trailing null bytes
        byte[] decryptedWithNulls = new byte[] { '1', '2', '3', '4', '5', 0, 0, 0 };

        String versionStr = new String(decryptedWithNulls, StandardCharsets.US_ASCII).trim();
        System.out.println("Version string with nulls (trimmed): '" + versionStr + "' length=" + versionStr.length());

        // Check if there are non-printable chars
        for (int i = 0; i < versionStr.length(); i++) {
            char c = versionStr.charAt(i);
            System.out.println("  char[" + i + "] = '" + c + "' (code=" + (int) c + ")");
        }

        // trim() doesn't remove null bytes, need to strip them
        String cleanVersion = versionStr.replace("\u0000", "").trim();
        System.out.println("Clean version: '" + cleanVersion + "'");

        int version = Integer.parseInt(cleanVersion);
        Assertions.assertEquals(12345, version);
    }

    /**
     * Debug test: Check if decryption produces readable output.
     * This helps diagnose if the key or decryption logic is wrong.
     */
    @Test
    public void testDebugDecryption_PrintBytes() {
        // Simulate some encrypted data (you would replace this with actual SV.T
        // content)
        String testVersion = "67890";
        byte[] original = testVersion.getBytes(StandardCharsets.US_ASCII);
        byte[] padded = new byte[8];
        System.arraycopy(original, 0, padded, 0, original.length);

        // Try both keys
        String[] keys = { "SILKROAD", "SILKROADVERSION" };

        for (String key : keys) {
            IBlowfish bf = Blowfish.newInstance(key.getBytes());
            byte[] encrypted = bf.encode(0, padded);

            System.out.println("\n=== Key: " + key + " ===");
            System.out.println("Original bytes: " + bytesToHex(padded));
            System.out.println("Encrypted bytes: " + bytesToHex(encrypted));

            byte[] decrypted = bf.decode(0, encrypted);
            System.out.println("Decrypted bytes: " + bytesToHex(decrypted));
            System.out.println("Decrypted string: '" + new String(decrypted, StandardCharsets.US_ASCII).trim() + "'");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
