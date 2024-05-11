/**
	Group Signature Crypto Library
	Copyright (C) 2010 - Diego Ferri
		
	This file is part of Group Signature Crypto Library.

    Group Signature Crypto Library is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Group Signature Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Group Signature Crypto Library.  If not, see <http://www.gnu.org/licenses/>
*/

package it.unibs.gs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;

public final class GSMathCore
{
	public static final int PRIME_CERTAINTY = 50;
	
	private GSMathCore() {}
	
	public static int getByteLength(BigInteger b) 
	{
		int n = b.bitLength();
		return (n + 7) >> 3;
	}
	
	public static byte[] toByteArray(BigInteger bi, int len) 
	{
		byte[] b = bi.toByteArray();
		int n = b.length;
		if (n == len) 
		    return b;
		
		// BigInteger prefixed a 0x00 byte for 2's complement form, remove it
		if ((n == len + 1) && (b[0] == 0)) 
		{
		    byte[] t = new byte[len];
		    System.arraycopy(b, 1, t, 0, len);
		    return t;
		}
		
		// must be smaller
		assert (n < len);
		byte[] t = new byte[len];
		System.arraycopy(b, 0, t, (len - n), n);
		return t;
	}
	
	/**
	 * BigInteger wrapper
	 * Constructs a randomly generated BigInteger, 
	 * uniformly distributed over the range 0 
	 * to limit, exclusive.
	 * 
	 * @param limit
	 * @return
	 */
	public static BigInteger nextBigRandom(BigInteger limit, SecureRandom random)
	{		
		BigDecimal rD = null;
		BigInteger r = null;
		do
		{
			rD = new BigDecimal(random.nextDouble());
			rD = rD.multiply(new BigDecimal(limit.subtract(BigInteger.ONE)));
			r = rD.toBigInteger();
		} 
		while (r.compareTo(limit) >= 0 || r.compareTo(BigInteger.ZERO) < 0);

		return r;
	}
	
	public static BigInteger getBigSafePrime(int size, SecureRandom random)
	{
		BigInteger p_, p;
		do
		{
			p_ = BigInteger.probablePrime(size, random);
			p = p_.shiftLeft(1).add(BigInteger.ONE);
		} 
		while (!p.isProbablePrime(GSMathCore.PRIME_CERTAINTY));
		
		return p_;
	}
	
	/**
	 * Construct a random generator quadratic residue mod n(=pq)
	 *
	 * @param n safe RSA modules 
	 * @param p safe prime
	 * @param q safe prime
	 * @return
	 */
	public static BigInteger getQRGenerator(BigInteger n, 
			BigInteger p, BigInteger q, SecureRandom random)
	{
		BigInteger qrElement;
		do
		{
			qrElement = nextBigRandom(n, random);
		} 
		while((qrElement.add(BigInteger.ONE).gcd(n)).compareTo(BigInteger.ONE) != 0 ||
				(qrElement.subtract(BigInteger.ONE).gcd(n)).compareTo(BigInteger.ONE) != 0);
		
		return (qrElement.multiply(qrElement)).mod(n);
	}
	
	/**
	 * Construct a random quadratic residue mod n(=pq)
	 *
	 * @param n safe RSA modules 
	 * @param p safe prime
	 * @param q safe prime
	 * see Euler's Criterion on Wikipedia
	 */
	public static BigInteger getQRElement(BigInteger n, 
			BigInteger p, BigInteger q, SecureRandom random)
	{
		BigInteger qrElement;
			
		do
		{
			qrElement = nextBigRandom(n, random);
		} 
		while(!isQRElement(qrElement, p, q));
		
		return qrElement;
	}
		
	/**
	 * Legendre Symbol
	 * @param e
	 * @param p must be prime
	 * @return
	 */
	public static boolean isQR(BigInteger a, BigInteger p)
	{
		/*if ((e.gcd(p)).compareTo(BigInteger.ONE) != 0)
		{
			// a not in Z*_{p}
			return false;
		}*/

		if ((a.modPow((p.subtract(BigInteger.ONE)).shiftRight(1), p))
				.compareTo(BigInteger.ONE) == 0)
			return true;
		else
			return false;
	}
	
	/**
	 * Check if a value is a quadratic residue mod n(=pq)
	 */
	public static boolean isQRElement(BigInteger a, 
			BigInteger p, BigInteger q)
	{
		return isQR(a, p) && isQR(a, q);
	}
	
	public static String byteArrayToString(byte[] b) 
	{
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < b.length; i++) 
		{
			buffer.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
		}
		return buffer.toString();
    }		
}
