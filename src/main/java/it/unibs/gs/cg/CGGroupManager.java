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

import it.unibs.gs.GSGroupManagerSpi;
import it.unibs.gs.GSMathCore;
import it.unibs.gs.certs.GSGroupCertificate;
import it.unibs.gs.certs.GSMemberCertificate;
import it.unibs.gs.cg.certs.CGGroupCertificate;
import it.unibs.gs.cg.certs.CGMemberCertificate;
import it.unibs.gs.interfaces.GSIdentity;
import it.unibs.gs.interfaces.GSKeyPair;
import it.unibs.gs.interfaces.GSPrivateKey;
import it.unibs.gs.interfaces.GSPublicKey;
import it.unibs.gs.interfaces.GSSecurityParameters;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;


public class CGGroupManager extends GSGroupManagerSpi
{
	private final static String ALGORITHM = "CG"; 
	private final static float VERSION = 1.0f; 
	
	/*@Override
	public void engineDoAction(String action, Object[] objs)
	{
		// TODO Auto-generated method stub

	}*/

	public GSMemberCertificate engineDoJoin(String subject, GSGroupCertificate groupCert, GSPrivateKey priv)
	{
		CGGroupPublicKeyImpl gpk = (CGGroupPublicKeyImpl) groupCert.getGPK();
		CGSecurityParameters params = (CGSecurityParameters) groupCert.getParams();
		SecureRandom random = new SecureRandom();
		
		BigInteger x;
		x = GSMathCore.nextBigRandom(gpk.getQ(), random);
		
		BigInteger Y;
		Y = gpk.getG().modPow(x, gpk.getP());
		
		BigInteger r, xCommit, s;
		r = GSMathCore.nextBigRandom(gpk.getn(), random);
		xCommit = gpk.getg().modPow(x, gpk.getn()).multiply(gpk.geth().modPow(r, gpk.getn())).mod(gpk.getn());
		s = GSMathCore.nextBigRandom(gpk.getQ(), random);

		BigInteger  e, E;
		do
		{
			e = new BigInteger(params.getle(), random);
			E = BigInteger.ONE.shiftLeft(params.getlE()).add(e);
		}
		while (!E.isProbablePrime(GSMathCore.PRIME_CERTAINTY));
		
		CGGroupPrivateKeyImpl gsk = (CGGroupPrivateKeyImpl) priv;
		BigInteger EinvQR, w_, r_, y;

		EinvQR = E.modInverse(
				gsk.getP().subtract(BigInteger.ONE).shiftRight(1).multiply(gsk.getQ().subtract(BigInteger.ONE).shiftRight(1)));
		w_ = gpk.getw().modPow(EinvQR, gpk.getn());
		r_ = GSMathCore.nextBigRandom(e, random);

		y = (gpk.geta().multiply(xCommit).multiply(
				gpk.geth().modPow(r_, gpk.getn()))).modPow(EinvQR, gpk.getn());
		
		GSPrivateKey memberKey = new CGMemberKeyImpl(w_, x, r.add(r_), y, e, s);
		
		members.add(new GSIdentity(subject, new Object[]{w_, e, s}));
		
		Calendar calendar = Calendar.getInstance();
		Date startDate = calendar.getTime();
		Calendar calendar2 = Calendar.getInstance();
		calendar2.add(Calendar.YEAR, 1);
		Date expiryDate = calendar2.getTime();
		GSMemberCertificate memberCert = new CGMemberCertificate(
				ALGORITHM, VERSION, "GM", subject, startDate, expiryDate, params, memberKey);

		return memberCert;
	}

	public GSKeyPair engineDoSetup(GSSecurityParameters params) throws NoSuchAlgorithmException
	{
		random = new SecureRandom();
		random.setSeed(System.currentTimeMillis());
		members = new Vector<GSIdentity>();
		
		KeyPairGenerator kpg = null;
		
		kpg = KeyPairGenerator.getInstance("CG");
		
		try
		{
			kpg.initialize(params, random);
		}
		catch (InvalidAlgorithmParameterException e)
		{
			e.printStackTrace();
			kpg.initialize(1024);
		}
		
		return new GSKeyPair(kpg.generateKeyPair());
	}

	public GSGroupCertificate engineBuildGroupCertificate(GSPublicKey gpk, String issuer, String subject, 
			Date startDate, Date expiryDate, BigInteger serialNumber, GSSecurityParameters params)
	{		
		GSGroupCertificate groupCert = new CGGroupCertificate(ALGORITHM, VERSION, serialNumber, 
				issuer, subject, startDate, expiryDate, gpk, params);
		
		return groupCert;
		
	}
	
	public GSPublicKey engineDoRevoke(GSPublicKey gpk, GSIdentity identity) throws Exception
	{
		throw new Exception("Unsupported Operation");
	}

	public void engineDoFullRevoke(GSIdentity identity) throws Exception
	{
		throw new Exception("Unsupported Operation");
	}

}
