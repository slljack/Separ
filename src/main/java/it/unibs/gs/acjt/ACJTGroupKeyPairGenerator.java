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

package it.unibs.gs.acjt;

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



public final class ACJTGroupKeyPairGenerator extends KeyPairGeneratorSpi
{
	private ACJTSecurityParameters params;
	private SecureRandom random;

	/**
	 * Initialize KeyPairGeneratorSpi
	 * @param keySize: in bits
	 * @param random
	 */
	public void initialize(int keysize, SecureRandom random)
	{		
		params = new ACJTSecurityParameters(keysize >> 1);
		
		randomSetup(random);
	}
	
	public void initialize(AlgorithmParameterSpec params, SecureRandom random)
		throws InvalidAlgorithmParameterException
	{	
		if (params instanceof ACJTSecurityParameters == false)
		    throw new InvalidAlgorithmParameterException
		    	("Params must be instance of ACJTSecurityParameters");

		this.params = (ACJTSecurityParameters) params;
		
		randomSetup(random);
	}

	public KeyPair generateKeyPair()
	{
		/**
		 * Safe Primes Generation p_, q_ : 512-bit primes 
		 * p = 2 * p_ + 1 : 513-bit prime 
		 * q = 2 * q_ + 1 : 513-bit prime
		 */
		BigInteger p, q, p_, q_, n;
		
		p_ = GSMathCore.getBigSafePrime(params.getLp(), random);
		p = p_.shiftLeft(1).add(BigInteger.ONE);
		
		q_ = GSMathCore.getBigSafePrime(params.getLp(), random);
		q = q_.shiftLeft(1).add(BigInteger.ONE);
		
		n = p.multiply(q);
		
		/**
		 * Choose random elements in QR(n) a, a0, g, h in QR(n) of order p'q'
		 */
		BigInteger a, a0, g, h;
		a = GSMathCore.getQRGenerator(n, p, q, random);
		a0 = GSMathCore.getQRGenerator(n, p, q, random);
		g = GSMathCore.getQRGenerator(n, p, q, random);
		h = GSMathCore.getQRGenerator(n, p, q, random);
 
		/**
		 * Choose a random secret element
		 */
		BigInteger x = GSMathCore.nextBigRandom(p_.multiply(q_), random);
		/**
		 * Set public parameter y
		 */
		BigInteger y = g.modPow(x, n);

		PublicKey publicKey = new ACJTGroupPublicKeyImpl(n, a, a0, g, h, y);
		PrivateKey privateKey = new ACJTGroupPrivateKeyImpl(p_, q_, x);

		KeyPair keyPair = new KeyPair(publicKey, privateKey);

		return keyPair;
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
