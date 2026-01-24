package org.sokybot.packetsniffer.packetanalyzer;

public class Utils {

	public static final char[] hexchars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
			'd', 'e', 'f' };

	
	public static boolean isHexString(String str) { 
		return str.matches("^(?:([A-Fa-f0-9]{2})\\s*)+$") ; 
	}

	public static String toHex(byte[] buf, int ofs, int len) {
		StringBuffer sb = new StringBuffer();
		int j = ofs + len;
		for (int i = ofs; i < j; i++) {
			if (i < buf.length) {
				sb.append(hexchars[(buf[i] & 0xF0) >> 4]);
				sb.append(hexchars[buf[i] & 0x0F]);
				sb.append(' ');
			} else {
				sb.append(' ');
				sb.append(' ');
				sb.append(' ');
			}
		}
		return sb.toString();
	}


	public static  String toAscii(byte[] buf, int ofs, int len) {
		StringBuffer sb = new StringBuffer();
		int j = ofs + len;
		for (int i = ofs; i < j; i++) {
			if (i < buf.length) {
				if ((20 <= buf[i]) && (126 >= buf[i])) {
					sb.append((char) buf[i]);
				} else {
					sb.append('.');
				}
			} else {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public static byte[] toByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}
