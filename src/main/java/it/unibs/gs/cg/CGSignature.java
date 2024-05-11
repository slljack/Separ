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


public class CGSignature extends GSSignatureSpi
{
	private ByteArrayOutputStream out;

	private CGGroupPublicKeyImpl gpk = null;
	private CGGroupPrivateKeyImpl gsk = null;
	private CGMemberKeyImpl mk = null;
	private CGSecurityParameters params;
	
	protected void engineInitSign(GSPublicKey gpk, GSPrivateKey mk, 
			GSSecurityParameters params, SecureRandom random)
			throws InvalidKeyException
	{
		if(!(gpk instanceof CGGroupPublicKeyImpl) || !(mk instanceof CGMemberKeyImpl)
				|| !(params instanceof CGSecurityParameters))
			throw new InvalidKeyException("Invalid CG Membership credentials.");
		this.gpk = (CGGroupPublicKeyImpl) gpk;
		this.mk = (CGMemberKeyImpl) mk;
		this.params = (CGSecurityParameters) params;
		
		out = new ByteArrayOutputStream();
		this.random = random;
	}
	
	protected void engineInitVerify(GSPublicKey gpk, GSSecurityParameters params)
			throws InvalidKeyException
	{
		if(!(gpk instanceof CGGroupPublicKeyImpl) || !(params instanceof CGSecurityParameters))
			throw new InvalidKeyException("Invalid CG Membership credentials.");
		this.gpk = (CGGroupPublicKeyImpl) gpk;
		this.params = (CGSecurityParameters) params;
		
		out = new ByteArrayOutputStream();
	}
	
	protected void engineInitOpen(GSPublicKey gpk, GSPrivateKey gsk)
			throws InvalidKeyException
	{
		if(!(gpk instanceof CGGroupPublicKeyImpl) || !(gsk instanceof CGGroupPrivateKeyImpl)
				|| !(params instanceof CGSecurityParameters))
			throw new InvalidKeyException("Invalid CG Membership credentials.");
		this.gpk = (CGGroupPublicKeyImpl) gpk;
		this.gsk = (CGGroupPrivateKeyImpl)gsk;	
		
		out = new ByteArrayOutputStream();	
	}
	
	protected void engineInitRevoke(GSPrivateKey gsk)
		throws InvalidKeyException
	{
		if(!(gsk instanceof CGGroupPrivateKeyImpl))
			throw new InvalidKeyException("Invalid CG Group Private Key.");
		this.gsk = (CGGroupPrivateKeyImpl)gsk;	
		
		out = new ByteArrayOutputStream();	
	}

	protected byte[] engineSign() throws SignatureException
	{
		BigInteger r_sign = new BigInteger(params.getln() >> 1, random);

		BigInteger R;
		R = GSMathCore.nextBigRandom(gpk.getQ(), random);
		
		BigInteger u;
		u = (gpk.geth().modPow(r_sign, gpk.getn()).multiply(
				mk.gety()).multiply(mk.getw())).mod(gpk.getn());
		
		BigInteger U1, U2, U3;
		U1 = gpk.getF().modPow(R, gpk.getP());
		U2 = gpk.getG().modPow(R.add(mk.getx()), gpk.getP());
		U3 = gpk.getH().modPow(R.add(mk.gete()), gpk.getP());
		
		BigInteger rx, rr, re, RR;
		rx = new BigInteger(params.getlQ() + 
				params.getlc() + params.getls(), random);
		rr = new BigInteger((params.getln() >> 1) +
				params.getlc() + params.getls(), random);
		re = new BigInteger(params.getle() +
				params.getlc() + params.getls(), random);
		RR = GSMathCore.nextBigRandom(gpk.getQ(), random);
		
		BigInteger v;
		v = u.modPow(re, gpk.getn()).multiply(
				gpk.getg().modPow(rx.negate(), gpk.getn())).multiply(
						gpk.geth().modPow(rr, gpk.getn())).mod(gpk.getn());
		
		BigInteger V1, V2, V3, V4;
		
		V1 = gpk.getF().modPow(RR, gpk.getP());
		V2 = gpk.getG().modPow(RR.add(rx), gpk.getP());
		V3 = gpk.getH().modPow(RR.add(re), gpk.getP());
		
		/*System.err.println("SIGN Procedure");
		System.err.println("u : (" + u.bitLength() + "bit) - " + u);
		System.err.println("v : (" + v.bitLength() + "bit) - " + v);
		System.err.println("U1 : (" + U1.bitLength() + "bit) - " + U1);
		System.err.println("U2 : (" + U2.bitLength() + "bit) - " + U2);
		System.err.println("U3 : (" + U3.bitLength() + "bit) - " + U3);
		//System.err.println("U4 : (" + U4.bitLength() + "bit) - " + U4);
		System.err.println("V1 : (" + V1.bitLength() + "bit) - " + V1);
		System.err.println("V2 : (" + V2.bitLength() + "bit) - " + V2);
		System.err.println("V3 : (" + V3.bitLength() + "bit) - " + V3);
		//System.err.println("V4 : (" + V4.bitLength() + "bit) - " + V4);
		System.err.println(out);*/
		
		
		/**
		 * String building
		 * gpk || u || v || U1 || U2 || U3 || U4 || V1 || V2 || V3 || V4 || m
		 */
		ByteArrayOutputStream hashable = null;
		try
		{
			hashable = new ByteArrayOutputStream();
			hashable.write(gpk.getEncoded());
			hashable.write(u.toByteArray());
			hashable.write(v.toByteArray());
			hashable.write(U1.toByteArray());
			hashable.write(U2.toByteArray());
			hashable.write(U3.toByteArray());
			//hashable.write(U4.toByteArray());
			hashable.write(V1.toByteArray());
			hashable.write(V2.toByteArray());
			hashable.write(V3.toByteArray());
			//hashable.write(V4.toByteArray());
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
		//System.err.println(c);
		
		BigInteger E = BigInteger.ONE.shiftLeft(params.getlE()).add(mk.gete());
		BigInteger zx, zr, ze, ZR;
		zx = rx.add(c.multiply(mk.getx()));
		zr = rr.add(c.multiply(mk.getr().add(r_sign.multiply(E))).negate());
		ze = re.add(c.multiply(mk.gete()));
		ZR = RR.add(c.multiply(R)).mod(gpk.getQ());
		
		CGSignatureImpl signature = new CGSignatureImpl(c, u, U1, U2, U3, zx, zr, ze, ZR);
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
		CGSignatureImpl sign = new CGSignatureImpl(sigBytes);
		
		//TODO parameters size verification 
		//System.out.println(sign.getze().bitLength());
		//System.out.println(sign.getzx().bitLength());

		BigInteger v, V1, V2, V3;
		v = (gpk.geta().multiply(gpk.getw())).modPow(sign.getc().negate(), gpk.getn()).multiply(
				gpk.getg().modPow(sign.getzx().negate(), gpk.getn())).multiply(
						gpk.geth().modPow(sign.getzr(), gpk.getn())).multiply(
								sign.getu().modPow(sign.getc().multiply(
										BigInteger.ONE.shiftLeft(params.getlE())).add(sign.getze()), 
										gpk.getn())).mod(gpk.getn());

		V1 = sign.getU1().modPow(sign.getc().negate(), gpk.getP()).multiply(
				gpk.getF().modPow(sign.getZR(), gpk.getP())).mod(gpk.getP());
		V2 = sign.getU2().modPow(sign.getc().negate(), gpk.getP()).multiply(
				gpk.getG().modPow(sign.getZR().add(sign.getzx()), gpk.getP())).mod(gpk.getP());
		V3 = sign.getU3().modPow(sign.getc().negate(), gpk.getP()).multiply(
				gpk.getH().modPow(sign.getZR().add(sign.getze()), gpk.getP())).mod(gpk.getP());
		
		/*System.err.println("VERIFY Procedure");
		System.err.println("u : (" + sign.getu().bitLength() + "bit) - " + sign.getu());
		System.err.println("v : (" + v.bitLength() + "bit) - " + v);
		System.err.println("U1 : (" + sign.getU1().bitLength() + "bit) - " + sign.getU1());
		System.err.println("U2 : (" + sign.getU2().bitLength() + "bit) - " + sign.getU2());
		System.err.println("U3 : (" + sign.getU3().bitLength() + "bit) - " + sign.getU3());
		//System.err.println("U4 : (" + sign.getU4().bitLength() + "bit) - " + sign.getU4());
		System.err.println("V1 : (" + V1.bitLength() + "bit) - " + V1);
		System.err.println("V2 : (" + V2.bitLength() + "bit) - " + V2);
		System.err.println("V3 : (" + V3.bitLength() + "bit) - " + V3);
		//System.err.println("V4 : (" + V4.bitLength() + "bit) - " + V4);
		System.err.println(out);*/
		
		/**
		 * String building
		 * gpk || u || v || U1 || U2 || U3 || U4 || V1 || V2 || V3 || V4 || m
		 */
		ByteArrayOutputStream hashable = null;
		try
		{
			hashable = new ByteArrayOutputStream();
			hashable.write(gpk.getEncoded());
			hashable.write(sign.getu().toByteArray());
			hashable.write(v.toByteArray());
			hashable.write(sign.getU1().toByteArray());
			hashable.write(sign.getU2().toByteArray());
			hashable.write(sign.getU3().toByteArray());
			//hashable.write(sign.getU4().toByteArray());
			hashable.write(V1.toByteArray());
			hashable.write(V2.toByteArray());
			hashable.write(V3.toByteArray());
			//hashable.write(V4.toByteArray());
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
		
		//System.err.println(c);
		
		if (sign.getc().compareTo(c) != 0)
		{
			return false;
		}
		
		return true;
	}
	
	protected GSIdentity engineOpen(byte[] sigBytes) throws SignatureException
	{
		BigInteger id = null;
		engineVerify(sigBytes);
		
		CGSignatureImpl sign = new CGSignatureImpl(sigBytes);
		
		//BigInteger r = gpk.getP().subtract(BigInteger.ONE).divide(gpk.getQ());
		
		id = (sign.getU2().multiply(sign.getU1().modPow(
				gsk.getXG().negate(), gpk.getP()))).mod(gpk.getP());
		
		return null;//identity;
	}

	@Override
	protected void engineInitRevoke(GSPublicKey gpk, GSRevocationList list)
			throws InvalidKeyException 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean engineRevoke(byte[] sigBytes) throws SignatureException 
	{
		// TODO Auto-generated method stub
		return false;
	}
}
