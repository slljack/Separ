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
import it.unibs.gs.GSSignatureSpi;
import it.unibs.gs.interfaces.GSIdentity;
import it.unibs.gs.interfaces.GSPrivateKey;
import it.unibs.gs.interfaces.GSPublicKey;
import it.unibs.gs.interfaces.GSRevocationList;
import it.unibs.gs.interfaces.GSSecurityParameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;



public class ACJTSignature extends GSSignatureSpi
{
	private ByteArrayOutputStream out;

	private ACJTGroupPublicKeyImpl gpk = null;
	private ACJTGroupPrivateKeyImpl gsk = null;
	private ACJTMemberKeyImpl mk = null;
	private ACJTSecurityParameters params = null;
	
	protected void engineInitSign(GSPublicKey gpk, GSPrivateKey mk, 
			GSSecurityParameters params, SecureRandom random)
			throws InvalidKeyException
	{
		if (!(gpk instanceof ACJTGroupPublicKeyImpl) || !(mk instanceof ACJTMemberKeyImpl)
				|| !(params instanceof ACJTSecurityParameters))
			throw new InvalidKeyException("Invalid ACJT Membership credentials.");
		this.gpk = (ACJTGroupPublicKeyImpl) gpk;
		this.mk = (ACJTMemberKeyImpl) mk;
		this.params = (ACJTSecurityParameters) params;
		this.random = random;
		
		out = new ByteArrayOutputStream();
	}

	protected void engineInitVerify(GSPublicKey gpk, GSSecurityParameters params)
			throws InvalidKeyException
	{
		if (!(gpk instanceof ACJTGroupPublicKeyImpl) || !(params instanceof ACJTSecurityParameters))
			throw new InvalidKeyException("Invalid ACJT Membership credentials.");
		this.gpk = (ACJTGroupPublicKeyImpl) gpk;
		this.params = (ACJTSecurityParameters) params;
		
		out = new ByteArrayOutputStream();	
	}
	
	protected void engineInitOpen(GSPublicKey gpk, GSPrivateKey gsk)
			throws InvalidKeyException
	{
		if (!(gpk instanceof ACJTGroupPublicKeyImpl) || !(gsk instanceof ACJTGroupPrivateKeyImpl))
			throw new InvalidKeyException("Invalid ACJT Membership credentials.");
		this.gpk = (ACJTGroupPublicKeyImpl) gpk;
		this.gsk = (ACJTGroupPrivateKeyImpl) gsk;
		
		out = new ByteArrayOutputStream();	
	}
	
	protected byte[] engineSign() throws SignatureException
	{
		BigInteger w = new BigInteger(params.getLp() << 1, random);

		BigInteger T1, T2, T3;
		T1 = mk.getA().multiply(gpk.getY().modPow(
				w, gpk.getN())).mod(gpk.getN());
		T2 = gpk.getG().modPow(w, gpk.getN());
		T3 = gpk.getG().modPow(mk.getE(), gpk.getN()).multiply(
				gpk.getH().modPow(w, gpk.getN())).mod(gpk.getN());
		
		BigInteger r1, r2, r3, r4;
		r1 = new BigInteger(Math.round(params.getEpsilon() *
						(params.getGamma2() + params.getK())), random);
		r2 = new BigInteger(Math.round(params.getEpsilon() *
						(params.getLambda2() + params.getK())), random);
		r3 = new BigInteger(Math.round(params.getEpsilon() *
						(params.getLambda1() + (params.getLp() << 1) + 
								params.getK() + 1)), random);
		r4 = new BigInteger(Math.round(params.getEpsilon() *
						((params.getLp() << 1) + params.getK())), 
						random);

		BigInteger d1, d2, d3, d4;
		d1 = T1.modPow(r1, gpk.getN()).multiply((
				gpk.getA().modPow(r2, gpk.getN()).multiply(
						gpk.getY().modPow(r3, gpk.getN()))).
						modInverse(gpk.getN())).mod(gpk.getN());
		d2 = T2.modPow(r1, gpk.getN()).multiply(
				gpk.getG().modPow(r3, gpk.getN()).
				modInverse(gpk.getN())).mod(gpk.getN());
		d3 = gpk.getG().modPow(r4, gpk.getN());
		d4 = gpk.getG().modPow(r1, gpk.getN()).multiply(
				gpk.getH().modPow(r4, gpk.getN())).mod(gpk.getN());
		
		/**
		 * String building
		 * g || h || y || a0 || a || T1 || T2 || T3 || d1 || d2 || d3 || d4 || m
		 */
		ByteArrayOutputStream hashable = null;
		try
		{
			hashable = new ByteArrayOutputStream();
			hashable.write(gpk.getG().toByteArray());
			hashable.write(gpk.getH().toByteArray());
			hashable.write(gpk.getY().toByteArray());
			hashable.write(gpk.getA0().toByteArray());
			hashable.write(T1.toByteArray());
			hashable.write(T2.toByteArray());
			hashable.write(T3.toByteArray());
			hashable.write(d1.toByteArray());
			hashable.write(d2.toByteArray());
			hashable.write(d3.toByteArray());
			hashable.write(d4.toByteArray());
			hashable.write(out.toByteArray());
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//hashing
		MessageDigest md = null;
		try 
		{
			md = MessageDigest.getInstance(params.getHash());
		} 
		catch (NoSuchAlgorithmException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		md.update(hashable.toByteArray());

		byte [] digest = md.digest();
		BigInteger c = new BigInteger(GSMathCore.byteArrayToString(
				digest), 16);
		
		BigInteger s1, s2, s3, s4;
		System.out.println(r1.bitLength());
		System.out.println(r2.bitLength());
		System.out.println(r3.bitLength());
		System.out.println(r4.bitLength());
		System.out.println(mk.getE().bitLength());
		s1 = r1.subtract(c.multiply(mk.getE().subtract(
				BigInteger.ONE.shiftLeft(
						params.getGamma1()))));
		s2 = r2.subtract(c.multiply(mk.getX().subtract(
				BigInteger.ONE.shiftLeft(
						params.getLambda1()))));
		s3 = r3.subtract(c.multiply(mk.getE().multiply(w)));
		s4 = r4.subtract(c.multiply(w));
		
		ACJTSignatureImpl signature = new ACJTSignatureImpl(c, s1, s2, s3, s4, T1, T2, T3);
		return signature.getEncoded();
	}

	protected void engineUpdate(byte b)
	{
		out.write(b);
	}
	
	protected void engineUpdate(byte[] b, int off, int len)
	{
		out.write(b, off, len);
	}

	protected boolean engineVerify(byte[] sigBytes) throws SignatureException
	{
		ACJTSignatureImpl sign = new ACJTSignatureImpl(sigBytes);
		
		BigInteger check1, check2, check3, check4;
		check1 = gpk.getA0().modPow(sign.getC(), gpk.getN()).multiply(
				sign.getT1().modPow(sign.getS1().subtract(
						sign.getC().multiply(BigInteger.ONE.shiftLeft(
								params.getGamma1()))), gpk.getN()).multiply((
				gpk.getA().modPow(sign.getS2().subtract(
						sign.getC().multiply(BigInteger.ONE.shiftLeft(
								params.getLambda1()))), gpk.getN()).multiply(
						gpk.getY().modPow(sign.getS3(), gpk.getN()))).
						modInverse(gpk.getN()))).mod(gpk.getN());
		check2 = sign.getT2().modPow(sign.getS1().subtract(
						sign.getC().multiply(BigInteger.ONE.shiftLeft(
								params.getGamma1()))), gpk.getN()).multiply((
				gpk.getG().modPow(sign.getS3(), gpk.getN())).
						modInverse(gpk.getN())).mod(gpk.getN());
		check3 = sign.getT2().modPow(sign.getC(), gpk.getN()).multiply(
				gpk.getG().modPow(sign.getS4(), gpk.getN())).mod(gpk.getN());
		check4 = sign.getT3().modPow(sign.getC(), gpk.getN()).multiply(
				gpk.getG().modPow(sign.getS1().subtract(
						sign.getC().multiply(BigInteger.ONE.shiftLeft(
								params.getGamma1()))), gpk.getN()).multiply(
						gpk.getH().modPow(sign.getS4(), gpk.getN()))).
						mod(gpk.getN());
	
		/**
		 * String building
		 * g || h || y || a0 || a || T1 || T2 || T3 || check1 || 
		 * check2 || check3 || check4 || m
		 */
		ByteArrayOutputStream hashable = null;
		try
		{
			hashable = new ByteArrayOutputStream();
			hashable.write(gpk.getG().toByteArray());
			hashable.write(gpk.getH().toByteArray());
			hashable.write(gpk.getY().toByteArray());
			hashable.write(gpk.getA0().toByteArray());
			hashable.write(sign.getT1().toByteArray());
			hashable.write(sign.getT2().toByteArray());
			hashable.write(sign.getT3().toByteArray());
			hashable.write(check1.toByteArray());
			hashable.write(check2.toByteArray());
			hashable.write(check3.toByteArray());
			hashable.write(check4.toByteArray());
			hashable.write(out.toByteArray());
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//hashing
		MessageDigest md = null;
		try 
		{
			md = MessageDigest.getInstance(params.getHash());
		} 
		catch (NoSuchAlgorithmException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		md.update(hashable.toByteArray());
	
		byte [] digest = md.digest();
		BigInteger c_ = new BigInteger(GSMathCore.byteArrayToString(
				digest), 16);
		
		if (sign.getC().compareTo(c_) != 0)
		{
			//TO DO: parameters size verification 
			return false;
		}
		
		return true;
	}
	
	protected GSIdentity engineOpen(byte[] sigBytes) throws SignatureException
	{
		BigInteger identity = null;
		engineVerify(sigBytes);
		
		ACJTSignatureImpl sign = new ACJTSignatureImpl(sigBytes);
		
		BigInteger id = sign.getT1().multiply(
				sign.getT2().modPow(gsk.getX(), gpk.getN()).
				modInverse(gpk.getN())).mod(gpk.getN());
			
		//TODO verify opening
		
		return null;//identity;
	}

	protected void engineInitRevoke(GSPublicKey gpk, GSRevocationList list)
			throws InvalidKeyException 
	{
	}

	protected boolean engineRevoke(byte[] sigBytes) throws SignatureException 
	{
		throw new SignatureException("Operation not supported.");
	}
}
