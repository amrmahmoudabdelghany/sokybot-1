package org.sokybot.pk2extractor.mediapk2.gameinfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2extractor.dto.gameinfo.DivisionInfoData;
import org.sokybot.pk2extractor.dto.gameinfo.DivisionData;

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
    public void testParseDivisionInfo_SingleDivisionSingleHost() {
        // Simulated legacy content: Locale byte, DivisionName, some ID, HostCount, IP, terminator
        // Format: "L\0DivisionName\0\0IP\0"
        String simulatedContent = "L\0DIV1\0\0192.168.1.1\0";
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
        String simulatedContent = "E\0FirstDiv\0\01.2.3.4\05.6.7.8\0\0SecondDiv\0\010.0.0.1\0";
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
        String simulatedContent = "L\0SomeText\0AnotherText\0";
        byte[] data = simulatedContent.getBytes(StandardCharsets.ISO_8859_1);

        GameInfoExtractor extractor = new GameInfoExtractor();
        DivisionInfoData result = extractor.parseDivisionInfo(data);

        Assertions.assertNotNull(result);
        List<DivisionData> divisions = result.getDivisions();
        Assertions.assertTrue(divisions.isEmpty(), "Should have no divisions if no IPs found");
    }
}
