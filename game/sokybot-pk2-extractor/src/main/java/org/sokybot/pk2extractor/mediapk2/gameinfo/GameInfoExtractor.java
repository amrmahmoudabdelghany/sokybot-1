package org.sokybot.pk2extractor.mediapk2.gameinfo;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.gameinfo.DivisionData;
import org.sokybot.pk2extractor.dto.gameinfo.DivisionInfoData;
import org.sokybot.pk2extractor.dto.gameinfo.GameInfoData;

/**
 * Extractor for game connection and version information.
 * Supports both legacy text-based and modern binary-based divisioninfo.txt
 * formats.
 */
public class GameInfoExtractor implements IExtractor<GameInfoData> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GameInfoExtractor.class);

    private static final String NAME = "Game Info";
    private static final String DIVISION_INFO_REGEX = "(?i).*divisioninfo\\.txt";
    private static final String GATEPORT_PATTERN = "(?i)gate.*port.*\\.txt";

    // Pattern for Host:Port or Host. Supports DNS names and IPs.
    private static final Pattern HOST_PORT_PATTERN = Pattern
            .compile("(?i)^(\\d{1,3}(?:\\.\\d{1,3}){3}|[a-z0-9-]+\\.[a-z0-9\\.-]+|localhost)(?::(\\d+))?$");

    @Override
    public Class<GameInfoData> getDtoClass() {
        return GameInfoData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, ExtractionListener<GameInfoData> listener,
            ExtractionProgressListener progressListener) {

        long startTime = System.currentTimeMillis();

        try {
            if (progressListener != null) {
                progressListener.onStart(NAME, -1);
            }

            // 1. Extract Division Info (Deep Search)
            DivisionInfoData divInfo = null;
            JMXFile divFile = driver.findFirst(DIVISION_INFO_REGEX).orElse(null);

            if (divFile != null) {
                logger.info("Found divisioninfo at: {}, starting extraction...", divFile.getName());

                try (InputStream is = divFile.getInputStream()) {
                    byte[] data = is.readAllBytes();
                    divInfo = parseDivisionInfo(data);
                } catch (Exception e) {
                    logger.error("Error parsing divisioninfo data", e);
                }
            } else {
                logger.warn("divisioninfo.txt not found in PK2!");
            }

            // 2. Extract Version from SV.T file (Blowfish-encrypted)
            int version = 0;
            final int[] port = { 15779 }; // Default port

            // Candidate Blowfish keys for SRO client versions
            String[] candidateKeys = { "SILKROADVERSION", "CYPERONLINE", "SILKROAD" };

            JMXFile svtFile = driver.findFirst("(?i).*SV\\.T").orElse(null);
            if (svtFile != null) {
                logger.info("Found SV.T file at: {}, extracting version...", svtFile.getName());
                try {
                    byte[] encryptedData = org.sokybot.pk2extractor.Pk2ExtractorUtils.firstChunk(svtFile);

                    if (encryptedData != null && encryptedData.length > 0) {
                        boolean solved = false;
                        for (String key : candidateKeys) {
                            try {
                                byte[] attemptData = encryptedData.clone();
                                byte[] decryptedData = org.sokybot.security.Blowfish.newInstance(key.getBytes())
                                        .decode(0, attemptData);

                                String rawVersionStr = new String(decryptedData, StandardCharsets.US_ASCII);
                                Matcher matcher = Pattern.compile("(\\d+)").matcher(rawVersionStr);
                                if (matcher.find()) {
                                    version = Integer.parseInt(matcher.group(1));
                                    logger.info("Successfully matched version {} using key '{}'", version, key);
                                    solved = true;
                                    break;
                                }
                            } catch (Exception e) {
                                logger.debug("Key '{}' failed for SV.T", key);
                            }
                        }

                        if (!solved) {
                            logger.error("Could not decrypt SV.T with any known keys");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Unexpected error during SV.T version extraction", e);
                }
            }

            // Try to extract port from gateport files
            driver.findFirst(GATEPORT_PATTERN).ifPresent(gateFile -> {
                try (InputStream is = gateFile.getInputStream()) {
                    byte[] data = is.readAllBytes();
                    String content = new String(data, StandardCharsets.UTF_8).trim();
                    try (Scanner scanner = new Scanner(content)) {
                        if (scanner.hasNextInt()) {
                            port[0] = scanner.nextInt();
                            logger.info("Extracted port from {}: {}", gateFile.getName(), port[0]);
                            // We use the port from gateport as default, but divisioninfo hosts might
                            // override it
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error reading gateport file", e);
                }
            });

            GameInfoData gameInfo = new GameInfoData();
            gameInfo.setDivisionInfo(divInfo);
            gameInfo.setSilkroadType(null); // Type extraction pending
            gameInfo.setPort(port[0]);
            gameInfo.setVersion(version);

            if (listener != null) {
                listener.onExtracted(gameInfo);
                listener.onComplete(1);
            }

            if (progressListener != null) {
                progressListener.onComplete(NAME, 1, System.currentTimeMillis() - startTime);
            }

        } catch (Exception e) {
            logger.error("Extraction failed", e);
            if (listener != null)
                listener.onError(e);
            if (progressListener != null)
                progressListener.onError(NAME, e);
        }
    }

    protected DivisionInfoData parseDivisionInfo(byte[] data) {
        if (data == null || data.length < 2)
            return null;

        try {
            // Bug 3 Fix: Try binary first as it's more structured.
            // If it produces no divisions or crashes, fall back to text.
            DivisionInfoData divInfo = parseBinary(data);
            if (divInfo != null && divInfo.getDivisions() != null && !divInfo.getDivisions().isEmpty()) {
                return divInfo;
            }
        } catch (Exception e) {
            logger.debug("Binary parse failed, falling back to text", e);
        }

        try {
            return parseText(data);
        } catch (Exception e) {
            logger.error("Text parse failed", e);
            return null;
        }
    }

    private DivisionInfoData parseBinary(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.remaining() < 2)
            return null;

        DivisionInfoData divInfo = new DivisionInfoData();

        divInfo.setLocal(buffer.get());
        int divCount = buffer.get() & 0xFF;

        List<DivisionData> divisions = new ArrayList<>();
        for (int i = 0; i < divCount; i++) {
            // Bug 2 Fix: Safe remaining check for string length
            if (buffer.remaining() < 4)
                break;

            String divName = readString(buffer);
            if (divName == null)
                break;

            // Skip 1 byte if null terminator was not part of the length but exists
            if (buffer.remaining() > 0 && buffer.get(buffer.position()) == 0)
                buffer.get();

            if (buffer.remaining() < 1)
                break;
            int hostCount = buffer.get() & 0xFF;
            List<String> hosts = new ArrayList<>();
            for (int j = 0; j < hostCount; j++) {
                String host = readString(buffer);
                if (host != null && !host.isEmpty()) {
                    hosts.add(host);
                }
                if (buffer.remaining() > 0 && buffer.get(buffer.position()) == 0)
                    buffer.get();
            }

            DivisionData divData = new DivisionData();
            divData.setName(divName);
            divData.setHosts(hosts);
            divisions.add(divData);
        }

        divInfo.setDivisions(divisions);
        return divInfo;
    }

    private String readString(ByteBuffer buffer) {
        if (buffer.remaining() < 4)
            return null; // Return null to indicate underflow
        int len = buffer.getInt();
        if (len < 0 || len > buffer.remaining())
            return null; // Return null to indicate invalid length or underflow

        if (len == 0)
            return "";

        byte[] bytes = new byte[len];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }

    private DivisionInfoData parseText(byte[] data) {
        String content = new String(data, StandardCharsets.ISO_8859_1);

        // Split by null
        String[] tokens = content.split("\u0000");

        DivisionInfoData divInfo = new DivisionInfoData();
        divInfo.setLocal(data.length > 0 ? data[0] : 0);

        List<DivisionData> divisions = new ArrayList<>();
        DivisionData currentDiv = null;

        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty())
                continue;

            Matcher matcher = HOST_PORT_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                if (currentDiv == null) {
                    currentDiv = new DivisionData();
                    currentDiv.setName("Unknown");
                    currentDiv.setHosts(new ArrayList<>());
                    divisions.add(currentDiv);
                }
                currentDiv.getHosts().add(trimmed);
            } else if (trimmed.length() >= 2 && !trimmed.matches("\\d+")) {
                currentDiv = new DivisionData();
                currentDiv.setName(trimmed);
                currentDiv.setHosts(new ArrayList<>());
                divisions.add(currentDiv);
            }
        }

        // Filter out divisions with no hosts
        divisions.removeIf(d -> d.getHosts().isEmpty());

        divInfo.setDivisions(divisions);
        return divInfo;
    }
}
