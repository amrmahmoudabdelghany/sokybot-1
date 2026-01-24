package org.sokybot.security;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author SHIKO
 */
public class CountSecurity implements ICountSecurity {
	long mut;
	long mut1;
	long mut2;
	long mut3;
	short byte1Seeds[];

	
	@Override
	public void configur(long seed) {
	 
		if (seed == 0)
			seed = 0x9ABFB3B6;
		int SeedTmp = (int) seed;
		seed = Integer.toUnsignedLong(SeedTmp);

		this.mut = seed;

		GenerateValue(1);

		GenerateValue(2);

		GenerateValue(3);

		GenerateValue(0);

		byte byte1tmp = (byte) ((this.mut & 0xFF) ^ (this.mut3 & 0xFF));
		short byte1 = (short) (byte1tmp & 0xff);

		byte byte2tmp = (byte) ((this.mut1 & 0xFF) ^ (this.mut2 & 0xFF));
		short byte2 = (short) (byte2tmp & 0xff);

		if (byte1 == 0)
			byte1 = 1;

		if (byte2 == 0)
			byte2 = 1;

		this.byte1Seeds = new short[3];

		this.byte1Seeds[0] = (short) (byte1 ^ byte2);

		this.byte1Seeds[1] = byte2;

		this.byte1Seeds[2] = byte1;

	}
  
	private void GenerateValue(int flag) {
		long val = this.mut;
		for (int i = 0; i < 32; i++)
			val = (((((((((((val >> 2) ^ val) >> 2) ^ val) >> 1) ^ val) >> 1) ^ val) >> 1) ^ val) & 1)
					| ((((val & 1) << 31) | (val >> 1)) & 0xFFFFFFFE);
		int valtmp = (int) val;

		this.mut = Integer.toUnsignedLong(valtmp);

		switch (flag) {
		case 1:

			this.mut1 = Integer.toUnsignedLong((int) this.mut);
		case 2:
			this.mut2 = Integer.toUnsignedLong((int) this.mut);
		case 3:
			this.mut3 = Integer.toUnsignedLong((int) this.mut);

		case 0:
			break;
		}

	}

	@Override
	public byte generateCountByte() {
		byte resulttmp = (byte) (byte1Seeds[2] * (~byte1Seeds[0] + byte1Seeds[1]));
		short result = (short) (resulttmp & 0xff);
		result = (short) (result ^ (result >> 4));
		byte1Seeds[0] = (short) result;
		return (byte) (result & 0xff);
	}

}
