
package org.sokybot.utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import com.sun.jna.platform.win32.WinNT.HANDLE;

import org.sokybot.IKernel32;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author AMROO
 */
@Slf4j
public class Helper {

	private Helper() {
	}

	public static int getRandomInt() {
		return (int) (Math.random() * (20000) + 1);
	}

	public static int getRandomInt(int min, int max) {
		Random random = new Random();
		return (random.nextInt((max - min)) + min);
	}

	public static String toHexString(byte[] bytes) {
		char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] toBytes(short value) {
		byte ret[] = new byte[2];

		ret[0] = (byte) (value & 0xff);
		ret[1] = (byte) ((value >> 8) & 0xff);

		return ret;
	}

	public static String formatHexDump(byte[] array, int offset, int length) {
		final int width = 16;

		StringBuilder builder = new StringBuilder();

		for (int rowOffset = offset; rowOffset < offset + length; rowOffset += width) {
			builder.append(String.format("%06d:  ", rowOffset));

			for (int index = 0; index < width; index++) {
				if (rowOffset + index < array.length) {
					builder.append(String.format("%02x ", array[rowOffset + index]));
				} else {
					builder.append("   ");
				}
			}

			if (rowOffset < array.length) {
				int asciiWidth = Math.min(width, array.length - rowOffset);
				builder.append("  |  ");
				try {
					builder.append(new String(array, rowOffset, asciiWidth, "UTF-8").replaceAll("\r\n", " ")
							.replaceAll("\n", " "));
				} catch (UnsupportedEncodingException ignored) {
					// If UTF-8 isn't available as an encoding then what can we do?!
				}
			}

			builder.append(String.format("%n"));
		}

		return builder.toString();
	}

	public static Object[] splite(String str, char splitor) {
		List<String> parts = new ArrayList<>();
		char ch;
		String part = "";
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {

			if ((ch = str.charAt(i)) != splitor) {
				part += ch;
			} else {
				parts.add(part);
				part = "";
			}
			if (i == strLen - 1 && str.charAt(strLen - 1) != splitor)
				parts.add(part);
		}
		return parts.toArray();
	}

	public static void shiftTo(String[] arr, String key, int index) {
		if (arr.length == 0 || key == null || index < 0 || index >= arr.length)
			return;

		String temp = arr[index];

		if (!temp.equalsIgnoreCase(key))
			for (int i = 0; i < arr.length; i++)
				if (arr[i].equalsIgnoreCase(key)) {
					arr[index] = arr[i];
					arr[i] = temp;
					break;
				}
	}

	public static byte[] bytesConcat(byte[] x, byte[] y) {
		int length = x.length + y.length;
		byte[] res = new byte[length];
		System.arraycopy(x, 0, res, 0, x.length);
		System.arraycopy(y, 0, res, x.length, y.length);
		return res;
	}

	public static Image fitimage(Image img, int w, int h) {
		BufferedImage resizedimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = resizedimage.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(img, 0, 0, w, h, null);
		g2.dispose();
		return resizedimage;
	}
	
	   public static BufferedImage makeRoundedCorner(BufferedImage image,
	            int cornerRadius) {
	        int w = image.getWidth();
	        int h = image.getHeight();
	        BufferedImage output = new BufferedImage(w, h,
	                BufferedImage.TYPE_INT_ARGB);

	        Graphics2D g2 = output.createGraphics();

	        // This is what we want, but it only does hard-clipping, i.e. aliasing
	        // g2.setClip(new RoundRectangle2D ...)

	        // so instead fake soft-clipping by first drawing the desired clip shape
	        // in fully opaque white with antialiasing enabled...
	        g2.setComposite(AlphaComposite.Src);
	        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                RenderingHints.VALUE_ANTIALIAS_ON);
	        g2.setColor(Color.WHITE);
	        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius,
	                cornerRadius));

	        // ... then compositing the image on top,
	        // using the white shape from above as alpha source
	        g2.setComposite(AlphaComposite.SrcAtop);
	        g2.drawImage(image, 0, 0, null);

	        g2.dispose();

	        return output;
	    }
	


	public static void killProcess(int processId) {
		IKernel32 lib = IKernel32.INSTANCE;

		HANDLE handle = lib.OpenProcess(1, false, processId);
		lib.TerminateProcess(handle, processId);
		lib.CloseHandle(handle);

	}

	public static void main(String args[]) {
		Object[] res = splite("D0340000,ITEM_EU_W_LIGHT_05_BA_B,Triangulum Hard Steel Mail,40,1,54", ',');
		for (int i = 0; i < res.length; i++) {
			System.out.println(res[i]);
		}
	}
}
