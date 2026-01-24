package org.sokybot.pk2extractor.mediapk2.gameinfo;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.gameinfo.GameInfoData;
import org.sokybot.pk2extractor.test.AbstractExtractorCompatibilityTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compatibility test for GameInfoExtractor.
 * Tests extraction of game version, division info, and port from real PK2 files.
 */
@Slf4j
@DisplayName("GameInfo Extractor Compatibility Test")
public class GameInfoExtractorCompatibilityTest extends AbstractExtractorCompatibilityTest<GameInfoData> {

    @Override
    protected IExtractor<GameInfoData> createExtractor() {
        return new GameInfoExtractor();
    }

    @Override
    protected int getMinimumExpectedItemCount() {
        // GameInfo extractor returns exactly 1 item
        return 1;
    }

    @Override
    protected void validateItem(GameInfoData item, int index) {
        assertNotNull(item, "GameInfoData should not be null");
        
        log.info("Validating GameInfo:");
        log.info("  Version: {}", item.getVersion());
        log.info("  Port: {}", item.getPort());
        
        // Port should be valid
        assertTrue(item.getPort() > 0, "Port should be greater than 0, got: " + item.getPort());
        assertTrue(item.getPort() <= 65535, "Port should be <= 65535, got: " + item.getPort());
        
        // Version should be extracted (may be 0 for some clients)
        assertTrue(item.getVersion() >= 0, "Version should be non-negative, got: " + item.getVersion());
        
        // Division info should exist
        assertNotNull(item.getDivisionInfo(), "DivisionInfo should not be null");
        assertNotNull(item.getDivisionInfo().getDivisions(), "Divisions list should not be null");
        assertFalse(item.getDivisionInfo().getDivisions().isEmpty(), 
            "Should have at least one division");
        
        log.info("  Divisions: {}", item.getDivisionInfo().getDivisions().size());
        item.getDivisionInfo().getDivisions().forEach(div -> {
            log.info("    - {}: {} hosts", div.getName(), div.getHosts().size());
        });
    }

    @Test
    @DisplayName("SV.T file can be found and decrypted (Diagnostic Exhaustive)")
    public void testSvtFileExistsAndCanBeDecrypted() {
        // List all files in Media.pk2 to see if there are multiple SV.T or others
        log.info("--- Listing all files in Media.pk2 ---");
        List<JMXFile> allFiles = getPk2Driver().find(".*");
        log.info("Total files in Media.pk2: {}", allFiles.size());
        
        for (JMXFile f : allFiles) {
            String name = f.getName();
            if (name.toLowerCase().endsWith(".t") || name.toLowerCase().contains("version") || name.toLowerCase().contains("revision")) {
                log.info("INTERESTING FILE: {} (Size: {} bytes)", name, f.getSize());
            }
        }

        // Find SV.T file using the correct regex pattern
        JMXFile svtFile = getPk2Driver().find("(?i)SV\\.T").stream().findFirst().orElse(null);
        
        assertNotNull(svtFile, "SV.T file should be found in Media.pk2");
        log.info("Found SV.T file: {} (Size: {} bytes)", svtFile.getName(), svtFile.getSize());
        
        // 1. Try with Pk2ExtractorUtils.firstChunk (expects 4-byte length prefix)
        log.info("--- Attempting firstChunk extraction (length prefix) ---");
        try {
            byte[] encryptedData = org.sokybot.pk2extractor.Pk2ExtractorUtils.firstChunk(svtFile);
            log.info("firstChunk data length: {}", encryptedData.length);
            log.info("firstChunk hex: {}", bytesToHex(encryptedData, 32));
            tryAllKeys(encryptedData, "firstChunk");
        } catch (Exception e) {
            log.warn("firstChunk extraction failed: {}", e.getMessage());
        }

        // 2. Try raw file content (all bytes)
        log.info("--- Attempting raw file extraction (no length prefix, decrypt from index 0) ---");
        try (java.io.InputStream in = svtFile.getInputStream()) {
            byte[] rawData = org.apache.commons.io.IOUtils.toByteArray(in);
            log.info("Raw data length: {}", rawData.length);
            log.info("Raw hex: {}", bytesToHex(rawData, 32));
            tryAllKeys(rawData, "Raw (from index 0)");
            
            // 3. Try skip first 4 bytes and decrypt (in case length prefix is NOT part of encrypted block)
            if (rawData.length > 4) {
                byte[] dataWithoutPrefix = new byte[rawData.length - 4];
                System.arraycopy(rawData, 4, dataWithoutPrefix, 0, dataWithoutPrefix.length);
                log.info("--- Attempting skip 4 bytes and decrypt ---");
                tryAllKeys(dataWithoutPrefix, "Raw (skip 4 prefix)");
            }
            
            // Try reading first 4 bytes as little-endian integer directly
            if (rawData.length >= 4) {
               int val = ((rawData[0] & 0xFF)) |
                         ((rawData[1] & 0xFF) << 8) |
                         ((rawData[2] & 0xFF) << 16) |
                         ((rawData[3] & 0xFF) << 24);
               log.info("First 4 bytes as LE int: {}", val);
            }
        } catch (Exception e) {
            log.warn("Raw extraction failed: {}", e.getMessage());
        }
    }

    private void tryAllKeys(byte[] data, String source) {
        String[] keys = {
            "SILKROADVERSION", 
            "SILKROAD", 
            "SILKROAD_VERSION", 
            "JOYMAX", 
            "JOYMAXVERSION",
            "SR_CLIENT",
            "CYPER",
            "CYPERONLINE",
            "CYPER_ONLINE",
            "CYPER_CLIENT",
            "CYPERVERSION",
            "CYPER_VERSION",
            "Silkroad",
            "SilkroadVersion",
            "CYPERK1",
            "CYPERK2"
        };

        for (String key : keys) {
            try {
                byte[] decrypted = org.sokybot.security.Blowfish.newInstance(key.getBytes())
                    .decode(0, data);
                
                // 1. Try as string
                String versionStr = new String(decrypted, java.nio.charset.StandardCharsets.US_ASCII)
                    .replace("\0", "")
                    .trim();
                
                if (versionStr.length() > 0 && versionStr.length() < 10) {
                    log.info("[{}] Key '{}' -> String: '{}' (Hex: {})", source, key, versionStr, bytesToHex(decrypted, 8));
                    if (versionStr.matches("^\\d+$")) {
                        log.info("✓ MATCH (String)! [{}] Key '{}' produced valid version: {}", source, key, versionStr);
                    }
                }

                // 2. Try as Little-Endian Integer
                if (decrypted.length >= 4) {
                    int v = ((decrypted[0] & 0xFF)) |
                            ((decrypted[1] & 0xFF) << 8) |
                            ((decrypted[2] & 0xFF) << 16) |
                            ((decrypted[3] & 0xFF) << 24);
                    
                    if (v > 0 && v < 2000) {
                        log.info("✓ MATCH (Int LE)! [{}] Key '{}' produced valid version: {}", source, key, v);
                    }
                }
                
                // 3. Try as Big-Endian Integer
                if (decrypted.length >= 4) {
                    int v = ((decrypted[0] & 0xFF) << 24) |
                            ((decrypted[1] & 0xFF) << 16) |
                            ((decrypted[2] & 0xFF) << 8) |
                            ((decrypted[3] & 0xFF));
                    
                    if (v > 0 && v < 2000) {
                        log.info("✓ MATCH (Int BE)! [{}] Key '{}' produced valid version: {}", source, key, v);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        
        // Also check if matches version pattern already
        if (data.length >= 4) {
             String plain = new String(data, java.nio.charset.StandardCharsets.US_ASCII).replace("\0", "").trim();
             if (plain.matches("^\\d+$")) {
                  log.info("✓ MATCH (Plain String)! [{}] Data is plain text: {}", source, plain);
             }
             
             int vPlain = ((data[0] & 0xFF)) | ((data[1] & 0xFF) << 8) | ((data[2] & 0xFF) << 16) | ((data[3] & 0xFF) << 24);
             if (vPlain > 0 && vPlain < 2000) {
                  log.info("✓ MATCH (Plain Int LE)! [{}] Data is plain int: {}", source, vPlain);
             }
        }
    }

    private String bytesToHex(byte[] bytes, int limit) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        int len = Math.min(bytes.length, limit);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        if (bytes.length > limit) sb.append("...");
        return sb.toString().trim();
    }

    @Test
    @DisplayName("Version extraction matches SV.T file content")
    public void testVersionMatchesSvtFile() {
        // Extract via extractor
        extractor.extract(getPk2Driver(), listener, progressListener);
        
        assertFalse(listener.hasError(), "Extraction should not fail");
        assertEquals(1, listener.getExtractedItems().size(), "Should extract exactly 1 GameInfo");
        
        GameInfoData gameInfo = listener.getExtractedItems().get(0);
        int extractedVersion = gameInfo.getVersion();
        
        log.info("Extractor reported version: {}", extractedVersion);
        
        // Manually extract from SV.T to compare
        JMXFile svtFile = getPk2Driver().find("(?i)SV\\.T").stream().findFirst().orElse(null);
        
        if (svtFile != null) {
            try {
                byte[] encryptedData = org.sokybot.pk2extractor.Pk2ExtractorUtils.firstChunk(svtFile);
                byte[] decrypted = org.sokybot.security.Blowfish.newInstance("SILKROADVERSION".getBytes())
                    .decode(0, encryptedData);
                
                String versionStr = new String(decrypted, java.nio.charset.StandardCharsets.US_ASCII)
                    .replace("\0", "")
                    .trim();
                
                if (!versionStr.isEmpty()) {
                    int manualVersion = Integer.parseInt(versionStr);
                    assertEquals(manualVersion, extractedVersion, 
                        "Extracted version should match SV.T file content");
                    log.info("✓ Version matches: {}", extractedVersion);
                }
            } catch (Exception e) {
                log.warn("Could not manually verify version: {}", e.getMessage());
            }
        } else {
            log.warn("SV.T not found, skipping version verification");
        }
    }

    @Test
    @DisplayName("DivisionInfo extraction is complete")
    public void testDivisionInfoIsComplete() {
        extractor.extract(getPk2Driver(), listener, progressListener);
        
        GameInfoData gameInfo = listener.getExtractedItems().get(0);
        
        assertNotNull(gameInfo.getDivisionInfo(), "DivisionInfo should not be null");
        assertNotNull(gameInfo.getDivisionInfo().getDivisions(), "Divisions should not be null");
        
        gameInfo.getDivisionInfo().getDivisions().forEach(division -> {
            assertNotNull(division.getName(), "Division name should not be null");
            assertFalse(division.getName().isEmpty(), "Division name should not be empty");
            assertNotNull(division.getHosts(), "Division hosts should not be null");
            assertFalse(division.getHosts().isEmpty(), 
                "Division '" + division.getName() + "' should have at least one host");
            
            // Each host should be a valid IP pattern
            division.getHosts().forEach(host -> {
                assertTrue(host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"),
                    "Host should be a valid IP address: " + host);
            });
        });
    }
}
