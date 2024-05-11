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

package it.unibs.gs.test;

import it.unibs.gs.GSGroupManager;
import it.unibs.gs.GSSignature;
import it.unibs.gs.acjt.ACJTProvider;
import it.unibs.gs.acjt.ACJTSecurityParameters;
import it.unibs.gs.acjt.ACJTSignatureImpl;
import it.unibs.gs.certs.GSGroupCertificate;
import it.unibs.gs.certs.GSMemberCertificate;
import it.unibs.gs.interfaces.GSIdentity;
import it.unibs.gs.interfaces.GSKeyPair;
import it.unibs.gs.interfaces.GSSecurityParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TestACJTGroupManager extends TestCase
{
	private KeyPair kp;
	
	@Before
	public void setUp() throws Exception
	{
		Security.addProvider(new ACJTProvider());
		generateKeyPair();
	}

	@After
	public void tearDown() throws Exception
	{
	}
	
	@Test
	public void testACJTMemberSignVerifyOpen()
	{
		StopWatch signSW = new StopWatch(), verifySW = new StopWatch(), openSW = new StopWatch();
		
		//SETUP
		GSGroupManager manager = null;
		try
		{
			manager = GSGroupManager.getInstance("ACJT");
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GSSecurityParameters params = new ACJTSecurityParameters(
				512, 160, 1.1f, 838, 600, 1102, 840, "SHA-1");
		GSKeyPair keyPair = null;
		try
		{
			keyPair = manager.doSetup(params);
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(keyPair.getPublic());
		System.out.println(keyPair.getPrivate());
		save("ACJT-GPK", keyPair.getPublic().getEncoded());
		save("ACJT-GSK", keyPair.getPrivate().getEncoded());
		
		Calendar calendar = Calendar.getInstance();
		Date startDate = calendar.getTime();
		Calendar calendar2 = Calendar.getInstance();
		calendar2.add(Calendar.YEAR, 1);
		Date expiryDate = calendar2.getTime();
		BigInteger serialNumber = BigInteger.ONE;
		GSGroupCertificate groupCert = manager.buildGroupCertificate(keyPair.getPublic(), 
				"GM", "PERIMETER", startDate, expiryDate, serialNumber, params);
		
		groupCert.addSignature(digitalSign(groupCert.getEncodedForSigning()));
		
		assertTrue(digitalVerify(groupCert.getEncodedForSigning(), groupCert.getSignature()));
		System.out.println(groupCert);
		save("ACJT-GroupCert", groupCert.getEncoded());
		
		//JOIN
		GSMemberCertificate memberCert = manager.joinGroup("member", groupCert, keyPair.getPrivate());
		System.out.println(memberCert);
		save("ACJT-MemberCert", memberCert.getEncoded());
		
		String message = "Diego Ferri - UniBS/DAI-Labor-TU Berlin";
		GSSignature engine = null;
		try
		{
			engine = GSSignature.getInstance("ACJT");
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//SIGN
		signSW.start();
		try
		{
			engine.initSign(groupCert.getGPK(), memberCert.getMemberKey(), groupCert.getParams(), new SecureRandom());
		}
		catch (InvalidKeyException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try
		{
			engine.update(message.getBytes("UTF-8"));
		}
		catch (SignatureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte [] signature = null;
		try
		{
			signature = engine.sign();
		}
		catch (SignatureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		signSW.stop();
		System.out.println("Sign " + signSW);
		
		System.out.println(new ACJTSignatureImpl(signature));
		
		//VERIFY
		verifySW.start();
		try
		{
			engine.initVerify(groupCert.getGPK(), params);
		}
		catch (InvalidKeyException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try
		{
			engine.update(message.getBytes("UTF-8"));
		}
		catch (SignatureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean verified = false;
		try
		{
			verified = engine.verify(signature);
		}
		catch (SignatureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		verifySW.stop();
		System.out.println("Verify " + verifySW);
		assertTrue(verified);
		
		openSW.start();
		try
		{
			engine.initOpen(groupCert.getGPK(), keyPair.getPrivate());
		}
		catch (InvalidKeyException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try
		{
			engine.update(message.getBytes("UTF-8"));
		}
		catch (SignatureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GSIdentity identity = null;
		try
		{
			identity = engine.open(signature);
		}
		catch (SignatureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		openSW.stop();
		System.out.println("Open " + openSW);
	}

	
	public void generateKeyPair()
	{
		//RSA Signature
		KeyPairGenerator keyGen = null;
		try
		{
			keyGen = KeyPairGenerator.getInstance("RSA");
		}
		catch (NoSuchAlgorithmException e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		SecureRandom random = new SecureRandom();
		keyGen.initialize(1024, random);

		this.kp = keyGen.generateKeyPair();
	}
	
	
	public byte [] digitalSign(byte [] message)
	{	
		Signature signEngine = null;
		byte [] signature = null;
		try
		{
			signEngine = Signature.getInstance("SHA1withRSA");
		}
		catch (NoSuchAlgorithmException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try
		{
			signEngine.initSign(kp.getPrivate());
		}
		catch (InvalidKeyException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try
		{
			signEngine.update(message);
			signature = signEngine.sign();
		}
		catch (SignatureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return signature;
	}
	
	public boolean digitalVerify(byte [] message, byte [] signature)
	{
		if (signature == null)
			return false;
			
		Signature signEngine = null;
		
		try
		{
			signEngine = Signature.getInstance("SHA1withRSA");
			signEngine.initVerify(kp.getPublic());
			signEngine.update(message);
			return signEngine.verify(signature);
		}
		catch (NoSuchAlgorithmException nsae)
		{
			return false;
		}
		catch (SignatureException se)
		{
			return false;
		}
		catch (InvalidKeyException ike)
		{
			return false;
		}
	}
	
	public void save(String filename, byte [] bytes)
	{
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(
					new File(filename));
			
			fos.write(bytes);
			
			fos.flush();
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
