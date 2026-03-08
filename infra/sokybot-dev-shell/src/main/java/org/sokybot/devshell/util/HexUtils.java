package org.sokybot.devshell.util;

/**
 * Shared hex parsing utilities for dev shell commands.
 */
public final class HexUtils {

    private HexUtils() {}

    public static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static int parseOpcode(String s) {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseInt(s.substring(2), 16);
        }
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return Integer.parseInt(s);
        }
    }
}
