package com.subsolr.util;

import java.math.BigInteger;

public class FNVHashing {
	private static final BigInteger INIT64 = new BigInteger("cbf29ce484222325",16);
	private static final BigInteger PRIME64 = new BigInteger("100000001b3", 16);
	private static final BigInteger MOD64 = new BigInteger("2").pow(64);

	public static BigInteger fnv1a_64(byte[] data) {
		BigInteger hash = INIT64;

		for (byte b : data) {
			hash = hash.xor(BigInteger.valueOf((int) b & 0xff));
			hash = hash.multiply(PRIME64).mod(MOD64);
		}

		return hash;
	}
}