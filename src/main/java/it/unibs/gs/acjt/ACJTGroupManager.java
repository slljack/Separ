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

import it.unibs.gs.GSGroupManagerSpi;
import it.unibs.gs.GSMathCore;
import it.unibs.gs.acjt.certs.ACJTGroupCertificate;
import it.unibs.gs.acjt.certs.ACJTMemberCertificate;
import it.unibs.gs.certs.GSGroupCertificate;
import it.unibs.gs.certs.GSMemberCertificate;
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


public class ACJTGroupManager extends GSGroupManagerSpi
{
	private final static String ALGORITHM = "ACJT"; 
	private final static float VERSION = 1.0f; 
	
	/*@Override
	public void engineDoAction(String action, Object[] objs)
	{
		// TODO Auto-generated method stub

	}*/

	protected GSMemberCertificate engineDoJoin(String subject, GSGroupCertificate groupCert, GSPrivateKey priv)
	{
		if (!groupCert.getAlgorithm().equals(ALGORITHM))
			return null;
		
		ACJTGroupPublicKeyImpl gpk = (ACJTGroupPublicKeyImpl) groupCert.getGPK();
		ACJTSecurityParameters params = (ACJTSecurityParameters) groupCert.getParams();
		SecureRandom random = new SecureRandom();
		
		BigInteger lambda1 = BigInteger.ONE.shiftLeft(
				params.getLambda1());
		BigInteger lambda2 = BigInteger.ONE.shiftLeft(
				params.getLambda2());
		/**
		 * Phase 1
		 */
		BigInteger x_;
		x_ = GSMathCore.nextBigRandom(lambda2, random);
	
		BigInteger r_;
		r_ = GSMathCore.nextBigRandom(gpk.getN().pow(2), random);
	
		BigInteger C1 = gpk.getG().modPow(x_, gpk.getN()).multiply(
				gpk.getH().modPow(r_, gpk.getN())).mod(gpk.getN());
		
		/**
		 * Phase 2
		 */
		ACJTGroupPrivateKeyImpl gsk = (ACJTGroupPrivateKeyImpl) priv;		
		BigInteger p = gsk.getP_().shiftLeft(1).add(BigInteger.ONE);
		BigInteger q = gsk.getQ_().shiftLeft(1).add(BigInteger.ONE);
		
	
		BigInteger alpha, beta;
		if(GSMathCore.isQRElement(C1, p, q))
		{
			alpha = GSMathCore.nextBigRandom(lambda2, random);
			beta = GSMathCore.nextBigRandom(lambda2, random);
		}
		else
			return null;
			
		/**
		 * Phase 3
		 */
		BigInteger x = lambda1.add(alpha.multiply(x_).add(
				beta.mod(lambda2)));
	
		BigInteger C2 = gpk.getA().modPow(x, gpk.getN());
	
		/**
		 * Phase 4
		 */
		
		//Create a valid certificate
		BigInteger e, A;
		if(GSMathCore.isQRElement(C2, p, q))
		{
			//A little magic for integral range
			BigInteger gamma1 = BigInteger.ONE.shiftLeft(
					params.getGamma1());
			BigInteger gamma2 = BigInteger.ONE.shiftLeft(
					params.getGamma2());
			BigInteger gamma2Plus = gamma2.shiftLeft(1);
			
			e = new BigInteger(params.getGamma1(), random);			
			//e.add(gamma1).subtract(gamma2);
			
			BigInteger eInverse = e.modInverse(
					gsk.getP_().multiply(gsk.getQ_())); 
			
			A = (C2.multiply(gpk.getA0())).modPow(eInverse, 
					gpk.getN());
			
			//TODO: insert transcript
			
		}
		else
			return null;
			

		GSPrivateKey memberKey = new ACJTMemberKeyImpl(A, e, x);
		
		members.add(new GSIdentity(subject, new Object[]{A, e}));
		
		Calendar calendar = Calendar.getInstance();
		Date startDate = calendar.getTime();
		Calendar calendar2 = Calendar.getInstance();
		calendar2.add(Calendar.YEAR, 1);
		Date expiryDate = calendar2.getTime();
		GSMemberCertificate memberCert = new ACJTMemberCertificate(
				ALGORITHM, VERSION, "GM", subject, startDate, expiryDate, params, memberKey);
		
		return memberCert;
	}

	protected GSKeyPair engineDoSetup(GSSecurityParameters params) throws NoSuchAlgorithmException
	{
		random = new SecureRandom();
		random.setSeed(System.currentTimeMillis());
		members = new Vector<GSIdentity>();
		
		KeyPairGenerator kpg = null;
		
		kpg = KeyPairGenerator.getInstance("ACJT");
		
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

	protected GSGroupCertificate engineBuildGroupCertificate(GSPublicKey gpk, String issuer, String subject, 
			Date startDate, Date expiryDate, BigInteger serialNumber, GSSecurityParameters params)
	{		
		GSGroupCertificate groupCert = new ACJTGroupCertificate(ALGORITHM, VERSION, serialNumber, 
				issuer, subject, startDate, expiryDate, gpk, params);
		
		return groupCert;
		
	}
	
	protected GSPublicKey engineDoRevoke(GSPublicKey gpk, GSIdentity identity) throws Exception
	{
		throw new Exception("Invalid Operation");
	}

	protected void engineDoFullRevoke(GSIdentity identity) throws Exception
	{
		throw new Exception("Invalid Operation");
	}
}
