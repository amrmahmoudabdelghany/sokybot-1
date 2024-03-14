package org.sokybot.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

import java.util.Set;
import java.util.stream.Stream;

public class SilkroadUtils {

	private SilkroadUtils() {
	}

	public static boolean isSilkraodDirectory(File directory) {

		Set<String> fileSet = new HashSet<>();
		fileSet.add("sro_client.exe");
		fileSet.add("data.pk2");
		fileSet.add("map.pk2");
		fileSet.add("media.pk2");
		fileSet.add("music.pk2");
		fileSet.add("particles.pk2");
		fileSet.add("gfxfilemanager.dll");

		Stream.of(directory.list()).map(String::toLowerCase).forEach(fileSet::remove);

		return fileSet.isEmpty();

	}

	public static  void transformValue(byte[] stream, byte[] Dkey, byte keyByte) {

		stream[0] ^= (byte) (stream[0] + Dkey[0] + keyByte);
		stream[1] ^= (byte) (stream[1] + Dkey[1] + keyByte);
		stream[2] ^= (byte) (stream[2] + Dkey[2] + keyByte);
		stream[3] ^= (byte) (stream[3] + Dkey[3] + keyByte);

		stream[4] ^= (byte) (stream[4] + Dkey[0] + keyByte);
		stream[5] ^= (byte) (stream[5] + Dkey[1] + keyByte);
		stream[6] ^= (byte) (stream[6] + Dkey[2] + keyByte);
		stream[7] ^= (byte) (stream[7] + Dkey[3] + keyByte);
	}
	public static boolean isValidSilkroadDirectory(String path) {
		// this game distribution path is valid only if the passed path is extists and
		// is silkroad path
		return (Files.exists(Paths.get(path)) && isSilkraodDirectory(new File(path)));
	}

	public static int getXCoord(float xOffset, short xSector, int offsetDivisor) {
		 float res = ((xSector - 135) * 192 + (xOffset / offsetDivisor)) ; 
		 
		return (int) res ;
	}

	public static int getXCoord(float xOffset, short xSector) {
		return getXCoord(xOffset, xSector, 10);

	}
	
	public static short getAngle(short val) { 
		return (short)(val * 360 / 65536);
	}

	public static int getYCoord(float yOffset, short ySector, int offsetDivisor) {
		return (int) ((ySector - 92) * 192 + (yOffset / offsetDivisor));
	}

	public static int getYCoord(float yOffset, short ySector) {
		return getYCoord(yOffset, ySector, 10);
	}

	
	
	/**
	 * 
	 * 
	 * @param y y coordinate
	 * @param x x coordinate
	 * @return 
	 */
	public static short getSectorYX(float y , float x ) { 
		return (short) ((getSectorY(y) << 8 & 0xffff) | (getSectorX(x) & 0xff)) ; 
  		
	}
	public static short getSectorX(int x) {
		return getSectorX((float)x) ; 
	}
	
	public static short getSectorX(float x ) { 
		return  (short)(Math.floor(x / 192 + 135)) ; 
	}
	
	public static byte getSectorY(int y) {
		return getSectorY((float) y) ; 
	}
	
	public static byte getSectorY(float y) { 
		return (byte) Math.floor(y/192 + 92) ; 
	}
	
	
	
	public static  int getSectorOffset(float v) { 
	
		int res =(int) v % 192 ; 
		
		if(res < 0) { 
			res += 192 ;
		}
		return res ; 
	}
	
	public static int getXOffset(int x , short xSector) { 
		return  ((x - ( (xSector - 135) * 192) ) * 10) ;  
	}
	
	public static int getYOffset(int y , short ySector) { 
		return ((y - ( (ySector - 92) * 192) ) * 10) ; 
	}
	

}
