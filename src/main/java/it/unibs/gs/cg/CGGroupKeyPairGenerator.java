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

package it.unibs.gs.cg;

import it.unibs.gs.GSMathCore;
import it.unibs.gs.interfaces.GSSecurityParameters;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;



public final class CGGroupKeyPairGenerator extends KeyPairGeneratorSpi
{
	private CGSecurityParameters params;
	private SecureRandom random;

	/**
	 * Initialize KeyPairGeneratorSpi
	 * @param keySize: in bits
	 * @param random
	 */
	public void initialize(int keysize, SecureRandom random)
	{		
		params = new CGSecurityParameters(keysize >> 1);
		
		randomSetup(random);
	}
	
	public void initialize(AlgorithmParameterSpec params, SecureRandom random)
		throws InvalidAlgorithmParameterException
	{	
		if (params instanceof CGSecurityParameters == false)
		    throw new InvalidAlgorithmParameterException
		    	("Params must be instance of CGSecurityParameters");

		this.params = (CGSecurityParameters) params;
		
		randomSetup(random);
	}

	public KeyPair generateKeyPair()
	{
		BigInteger p, q, p_, q_, n;
		
		p_ = GSMathCore.getBigSafePrime((params.getln() >> 1) - 1, random);
		p = p_.shiftLeft(1).add(BigInteger.ONE);
		
		q_ = GSMathCore.getBigSafePrime((params.getln() >> 1) - 1, random);
		q = q_.shiftLeft(1).add(BigInteger.ONE);
		
		n = p.multiply(q);
		
		//System.err.println("n : (" + n.bitLength() + "bit) - " + n);
		//System.err.println("p : (" + p.bitLength() + "bit) - " + p);
		//System.err.println("q : (" + q.bitLength() + "bit) - " + q);
		
		/**
		 * Choose random elements in QR(n) a, g, h in QR(n) of order p'q'
		 */
		BigInteger a, g, h;
		a = GSMathCore.getQRElement(n, p, q, random);
		g = GSMathCore.getQRElement(n, p, q, random);
		h = GSMathCore.getQRElement(n, p, q, random);
		
		/**
		 * Choose due prime random element where Q|P-1
		 * Q is lQ-bit
		 * P is lP-bit
		 * See: Schnorr's group on Wikipedia
		 */
		
		BigInteger P, Q, r;

		do
		{
			Q = BigInteger.probablePrime(params.getlQ(), random);
			int i = 0;
			
			do
			{
				i++;
				r = new BigInteger(params.getlP() - params.getlQ(), random);
				P = Q.multiply(r).add(BigInteger.ONE);
			}
			while(!(P.isProbablePrime(GSMathCore.PRIME_CERTAINTY)) && i < 200);
		}
		while(!(P.isProbablePrime(GSMathCore.PRIME_CERTAINTY)) || P.bitLength() != params.getlP());	
		
		//System.err.println("P : (" + P.bitLength() + "bit) - " + P);
		//System.err.println("Q : (" + Q.bitLength() + "bit) - " + Q);

		BigInteger F;
		
		do
		{
			F = GSMathCore.nextBigRandom(P, random).modPow(r, P);
		}
		while(F.equals(BigInteger.ONE));
		
		//System.err.println("F : (" + F.bitLength() + "bit) - " + F);
			
		BigInteger XG, XH;
		XG = GSMathCore.nextBigRandom(Q, random);
		XH = GSMathCore.nextBigRandom(Q, random);
		
		BigInteger G, H;
		G = F.modPow(XG, P);
		H = F.modPow(XH, P);
		
		//System.err.println("G : (" + G.bitLength() + "bit) - " + G);
		//System.err.println("H : (" + H.bitLength() + "bit) - " + H);
		
		BigInteger w, f;
		w = GSMathCore.getQRElement(n, p, q, random);
		f = GSMathCore.getQRElement(n, p, q, random);
		
		PublicKey gpk = new CGGroupPublicKeyImpl(n, a, g, h, Q, P, F, G, H, w, f);
		PrivateKey gsk = new CGGroupPrivateKeyImpl(p, q, XG);

		return new KeyPair(gpk, gsk);	
	}
	
	private void randomSetup(SecureRandom random)
	{
		if (random == null)
		{
			this.random = new SecureRandom();
			this.random.setSeed(System.currentTimeMillis());
		}
		else
			this.random = random;
	}
	
	public GSSecurityParameters getSecurityParameters()
	{
		return params;
	}
}
