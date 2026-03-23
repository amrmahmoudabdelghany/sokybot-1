package org.sokybot.packetsniffer.packetanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sokybot.network.NetworkPeer;
import org.sokybot.packetsniffer.trafficmonitor.TablePacket;

/**
 * Service for packet analyzer functionality
 * Handles hex viewer display, variable management, and highlighting
 */
public class PacketAnalyzerService {
    
    private final List<TablePacket> packets;
    private final List<PacketVar> variables;
    private String selectedHex = "";
    private int selectedByteCount = 0;
    private int matchCount = 0;
    private int groupLen = 16; // Bytes per line (default)
    
    public PacketAnalyzerService(List<TablePacket> packets) {
        this.packets = new ArrayList<>(packets);
        this.variables = new ArrayList<>();
    }
    
    /**
     * Get formatted packet data for hex viewer
     */
    public Map<String, Object> getPacketData() {
        List<Map<String, Object>> packetRows = new ArrayList<>();

        for (TablePacket packet : packets) {
            try {
                byte[] buffer = packet.getPacket().getPacketReader().readBytes(0);
                if (buffer == null) buffer = new byte[0];
                NetworkPeer source = packet.getPacket().getPacketSource();
                String opcode = String.format("0x%04X", packet.getPacket().getOpcode() & 0xffff);
                String name = packet.getName();

                // Add header row
                Map<String, Object> headerRow = new HashMap<>();
                headerRow.put("type", "header");
                headerRow.put("source", source.toString());
                headerRow.put("opcode", opcode);
                headerRow.put("name", name);
                packetRows.add(headerRow);

                // Add hex data rows
                int i = 0;
                do {
                    int lineNumber = i;
                    String hexLine = Utils.toHex(buffer, i, groupLen);
                    String asciiLine = Utils.toAscii(buffer, i, groupLen);

                    Map<String, Object> dataRow = new HashMap<>();
                    dataRow.put("type", "data");
                    dataRow.put("lineNumber", String.format("%06X", lineNumber));
                    dataRow.put("hex", hexLine);
                    dataRow.put("ascii", asciiLine);
                    dataRow.put("startOffset", i);
                    dataRow.put("endOffset", Math.min(i + groupLen, buffer.length));
                    packetRows.add(dataRow);

                    i += groupLen;
                } while (i < buffer.length);

                // Add empty row separator
                Map<String, Object> separatorRow = new HashMap<>();
                separatorRow.put("type", "separator");
                packetRows.add(separatorRow);
            } catch (Exception e) {
                // Skip one bad packet so the rest of the analyzer still gets data
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("packets", packetRows);
        result.put("groupLen", groupLen);
        result.put("selectedHex", selectedHex);
        result.put("selectedByteCount", selectedByteCount);
        result.put("matchCount", matchCount);
        result.put("variables", convertVariablesToData());
        
        return result;
    }
    
    /**
     * Handle selection of hex bytes
     */
    public Map<String, Object> handleSelectHex(String hex, int startOffset, int endOffset) {
        selectedHex = hex != null ? hex.trim() : "";
        
        if (selectedHex.isEmpty() || !Utils.isHexString(selectedHex)) {
            selectedByteCount = 0;
            matchCount = 0;
        } else {
            // Calculate byte count
            selectedByteCount = selectedHex.replaceAll("\\s+", "").length() / 2;
            
            // Find matches
            matchCount = findMatches(selectedHex);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("selectedHex", selectedHex);
        result.put("selectedByteCount", selectedByteCount);
        result.put("matchCount", matchCount);
        result.put("matches", findMatchPositions(selectedHex));
        
        return result;
    }
    
    /**
     * Define a variable from selected hex
     */
    public Map<String, Object> defineVariable(String varName, String hex, String packetName) {
        if (hex == null || hex.trim().isEmpty() || !Utils.isHexString(hex)) {
            return Map.of("success", false, "error", "Invalid hex string");
        }
        
        PacketVar var = new PacketVar();
        var.setPacket(packetName != null ? packetName : "Unknown");
        var.setVarName(varName != null && !varName.isBlank() ? varName : "Undefined");
        var.setOriginValue(hex.trim());
        String formattedValue = "0x" + hex.replaceAll("\\s+", "");
        var.setFormatedValue(formattedValue);
        var.setComment("");
        
        variables.add(var);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("variables", convertVariablesToData());
        
        return result;
    }
    
    /**
     * Update variable
     */
    public Map<String, Object> updateVariable(int index, String varName, String comment) {
        if (index < 0 || index >= variables.size()) {
            return Map.of("success", false, "error", "Invalid variable index");
        }
        
        PacketVar var = variables.get(index);
        if (varName != null) {
            var.setVarName(varName);
        }
        if (comment != null) {
            var.setComment(comment);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("variables", convertVariablesToData());
        
        return result;
    }
    
    /**
     * Select variable (for highlighting)
     */
    public Map<String, Object> selectVariable(int index) {
        if (index < 0 || index >= variables.size()) {
            return Map.of("success", false, "error", "Invalid variable index");
        }
        
        PacketVar var = variables.get(index);
        return handleSelectHex(var.getOriginValue(), -1, -1);
    }
    
    /**
     * Set bytes per line
     */
    public Map<String, Object> setGroupLen(int groupLen) {
        this.groupLen = Math.max(8, Math.min(32, groupLen)); // Clamp between 8 and 32
        return getPacketData();
    }
    
    private int findMatches(String hex) {
        if (hex == null || hex.trim().isEmpty()) {
            return 0;
        }
        
        String hexPattern = hex.replaceAll("\\s+", "");
        int count = 0;
        
        for (TablePacket packet : packets) {
            byte[] buffer = packet.getPacket().getPacketReader().readBytes(0);
            String packetHex = bytesToHex(buffer);
            
            int index = 0;
            while ((index = packetHex.indexOf(hexPattern, index)) != -1) {
                count++;
                index += hexPattern.length();
            }
        }
        
        return count;
    }
    
    private List<Map<String, Object>> findMatchPositions(String hex) {
        List<Map<String, Object>> positions = new ArrayList<>();
        
        if (hex == null || hex.trim().isEmpty()) {
            return positions;
        }
        
        String hexPattern = hex.replaceAll("\\s+", "");
        int packetOffset = 0;
        
        for (TablePacket packet : packets) {
            byte[] buffer = packet.getPacket().getPacketReader().readBytes(0);
            String packetHex = bytesToHex(buffer);
            
            int index = 0;
            while ((index = packetHex.indexOf(hexPattern, index)) != -1) {
                Map<String, Object> pos = new HashMap<>();
                pos.put("packetIndex", packets.indexOf(packet));
                pos.put("byteOffset", index / 2); // Convert hex chars to bytes
                pos.put("length", hexPattern.length() / 2);
                positions.add(pos);
                index += hexPattern.length();
            }
            
            packetOffset += buffer.length;
        }
        
        return positions;
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Utils.hexchars[(b & 0xF0) >> 4]);
            sb.append(Utils.hexchars[b & 0x0F]);
        }
        return sb.toString();
    }
    
    private List<Map<String, Object>> convertVariablesToData() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            PacketVar var = variables.get(i);
            Map<String, Object> varData = new HashMap<>();
            varData.put("index", i);
            varData.put("packet", var.getPacket());
            varData.put("varName", var.getVarName());
            varData.put("value", var.getFormatedValue());
            varData.put("comment", var.getComment());
            varData.put("originValue", var.getOriginValue());
            result.add(varData);
        }
        return result;
    }
    
    /**
     * Get initial state for UI
     */
    public Map<String, Object> getInitialState() {
        Map<String, Object> state = new HashMap<>();
        state.putAll(getPacketData());
        state.put("selectedHex", selectedHex);
        state.put("selectedByteCount", selectedByteCount);
        state.put("matchCount", matchCount);
        return state;
    }
}
