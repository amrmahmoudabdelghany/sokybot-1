package org.sokybot.pk2extractor.mediapk2.gameinfo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.gameinfo.DivisionData;
import org.sokybot.pk2extractor.dto.gameinfo.DivisionInfoData;
import org.sokybot.pk2extractor.dto.gameinfo.GameInfoData;
import org.sokybot.pk2extractor.dto.gameinfo.SilkroadTypeData;

public class GameInfoExtractor implements IExtractor<GameInfoData> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GameInfoExtractor.class);

    private static final String NAME = "Game Info";
    private static final String DIVISION_INFO_FILE = "divisioninfo.txt";
    private static final String TYPE_FILE = "type.txt";
    private static final String GATEPORT_PATTERN = "(?i)gate.*port.*\\.txt";

    @Override
    public Class<GameInfoData> getDtoClass() {
        return GameInfoData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, ExtractionListener<GameInfoData> listener, ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }

            // 1. Extract Division Info
            DivisionInfoData divInfo = null;
            JMXFile divFile = driver.find(DIVISION_INFO_FILE).stream().findFirst().orElse(null);
            
            if (divFile != null) {
                logger.info("Found divisioninfo.txt, starting token-based parsing...");
                
                try (InputStream is = divFile.getInputStream()) {
                     byte[] data = is.readAllBytes();
                     divInfo = parseDivisionInfo(data);
                } catch (Exception e) {
                   logger.error("Error parsing divisioninfo.txt tokens", e);
                }
            } else {
                logger.warn("divisioninfo.txt not found in PK2!");
            }

            // 2. Extract Type Info (SilkroadType)
            SilkroadTypeData typeData = null;
            
            GameInfoData gameInfo = new GameInfoData();
            gameInfo.setDivisionInfo(divInfo);
            gameInfo.setSilkroadType(typeData);
            gameInfo.setPort(15779); 
            gameInfo.setVersion(0);
            
            // Emit single result
            if (listener != null) {
                listener.onExtracted(gameInfo);
            }
            
            if (progressListener != null) {
                progressListener.onProgress(NAME, 1, 1, "Game Info Extracted");
            }
            
            if (listener != null) {
                listener.onComplete(1);
            }
            
             if (progressListener != null) {
                progressListener.onComplete(NAME, 1, System.currentTimeMillis() - startTime);
            }

        } catch (Exception e) {
             logger.error("Extraction failed", e);
             if (listener != null) listener.onError(e);
             if (progressListener != null) progressListener.onError(NAME, e);
        }
    }

    /**
     * Parses division info from raw byte data.
     * This method is extracted to allow unit testing without requiring a PK2 driver.
     * @param data The raw bytes from divisioninfo.txt
     * @return Parsed DivisionInfoData
     */
    protected DivisionInfoData parseDivisionInfo(byte[] data) {
        // Use ISO-8859-1 to preserve byte values 1-to-1 in chars
        String content = new String(data, StandardCharsets.ISO_8859_1);
        String[] tokens = content.split("\u0000");
        
        logger.info("Token parse: Found {} tokens.", tokens.length);
        for (int i = 0; i < tokens.length; i++) {
            if (!tokens[i].isEmpty()) {
                logger.debug("Token[{}]: {}", i, tokens[i]);
            }
        }

        if (tokens.length == 0) {
            return null;
        }
        
        DivisionInfoData divInfo = new DivisionInfoData();
        
        // Attempt to get Locale from first byte of first token (legacy behavior)
        byte locale = 0;
        if (!tokens[0].isEmpty()) {
            locale = (byte) tokens[0].charAt(0);
        }
        divInfo.setLocal(locale);
        logger.info("Detected Locale: {}", locale);

        List<DivisionData> divisions = new ArrayList<>();
        
        // Robust Scan: Look for IP addresses
        // Heuristic: IP pattern
        String ipPattern = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$";
        
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.matches(ipPattern)) {
                // Found an IP!
                String host = token;
                String divName = "UnknownType";
                
                // Find the nearest preceding non-empty, non-numeric token
                for (int k = i - 1; k >= 0; k--) {
                    String candidate = tokens[k].trim();
                    if (!candidate.isEmpty() && !candidate.matches("\\d+") && candidate.length() > 1) {
                        divName = candidate;
                        break;
                    }
                }
                
                // Check if we already have this division
                DivisionData existingDiv = null;
                for (DivisionData d : divisions) {
                    if (d.getName().equals(divName)) {
                        existingDiv = d;
                        break;
                    }
                }
                
                if (existingDiv == null) {
                    existingDiv = new DivisionData();
                    existingDiv.setName(divName);
                    existingDiv.setHosts(new ArrayList<>());
                    divisions.add(existingDiv);
                    logger.info("Found New Division: '{}'", divName);
                }
                
                existingDiv.getHosts().add(host);
                logger.debug("Added Host '{}' to Division '{}'", host, divName);
            }
        }
        divInfo.setDivisions(divisions);
        return divInfo;
    }
}
