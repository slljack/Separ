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

import it.unibs.gs.interfaces.GSSecurityParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNumericString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;


public final class ACJTSecurityParameters implements GSSecurityParameters
{
	private int lp;
	private int k;
	private float epsilon;
	private int lambda2;
	private int lambda1;
	private int gamma2;
	private int gamma1;
	private String hash;

	//BEWARE: 150 digits ~ 512bits 
	
	/**
	 * Interval rules
	 * Lambda2 > 4 * LP //in digits
	 * Lambda1 > EPSILON * (Lambda2 + K) + 2;
	 * Gamma2 > Lambda1 + 2
	 * Gamma1 > EPSILON * (Gamma2 + K) + 2
	 */
	
	public ACJTSecurityParameters()
	{
		/*
		 *	LP = 512
		 *  K = 160
		 *  HASH = SHA-1
		 *  LAMBDA1 = 838
		 *  LAMBDA2 = 600
		 *  GAMMA1 = 1102
		 *  GAMMA2 = 840
		 */
		
		//Standard Parameters
		this(512, 160, 1.1f, 838, 600, 1102, 840, "SHA-1");
	}
	
	public ACJTSecurityParameters(int lp)
	{
		this.lp = lp;
		this.epsilon = 1.1f;
		this.k = 160;
		this.lambda2 = 4 * lp + 1;
		this.lambda1 = Math.round(epsilon * (lambda2 + k)) + 2 + 1;
		this.gamma2 = lambda1 + 2 + 1;
		this.gamma1 = Math.round(epsilon * (gamma2 + k)) + 2 + 1;
	}
	
	public ACJTSecurityParameters(int lp, int k, float epsilon,
			int lambda1, int lambda2, int gamma1, int gamma2, String hash)
	{
		this.lp = lp;
		this.k = k;
		this.epsilon = epsilon;
		this.lambda1 = lambda1;
		this.lambda2 = lambda2;
		this.gamma1 = gamma1;
		this.gamma2 = gamma2;
		this.hash = hash;
	}
	
	public ACJTSecurityParameters(byte[] encoded)
	{
		ASN1InputStream dIn = new ASN1InputStream(new ByteArrayInputStream(encoded));
		
		DERSequence s = null;
		try
		{
			s = (DERSequence)dIn.readObject();
		}
		catch (IOException e)
		{
			throw new SecurityException("can't decode Public Key object");
		}
		
		this.lp = ((DERInteger)s.getObjectAt(0)).getValue().intValue();
		this.k = ((DERInteger)s.getObjectAt(1)).getValue().intValue();
		this.epsilon = Float.parseFloat(((DERNumericString)s.getObjectAt(8)).getString());
		this.lambda1 = ((DERInteger)s.getObjectAt(3)).getValue().intValue();
		this.lambda2 = ((DERInteger)s.getObjectAt(4)).getValue().intValue();
		this.gamma1 = ((DERInteger)s.getObjectAt(5)).getValue().intValue();
		this.gamma2 = ((DERInteger)s.getObjectAt(6)).getValue().intValue();
		this.hash = ((DERIA5String)s.getObjectAt(7)).getString();
		
	}
	
	public int getLp()
	{
		return lp;
	}
	
	public int getK()
	{
		return k;
	}
	
	public float getEpsilon()
	{
		return epsilon;
	}
	
	public int getLambda2()
	{
		return lambda2;
	}
	
	public int getLambda1()
	{
		return lambda1;
	}
	
	public int getGamma2()
	{
		return gamma2;
	}
	
	public int getGamma1()
	{
		return gamma1;
	}
	
	public String getHash()
	{
		return hash;
	}
	
	public byte [] getEncoded()
	{
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new DERInteger(lp));
		v.add(new DERInteger(k));
		v.add(new DERNumericString(String.valueOf(epsilon)));
		v.add(new DERInteger(lambda1));
		v.add(new DERInteger(lambda2));
		v.add(new DERInteger(gamma1));
		v.add(new DERInteger(gamma2));
		v.add(new DERIA5String(hash));

		DERSequence s = new DERSequence(v);
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);
		try
		{
			dOut.writeObject(s);
			dOut.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bOut.toByteArray();
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		
		s.append("\n\tlP: ");
		s.append(lp);
		s.append("\n\tk: ");
		s.append(k);
		s.append("\n\tepsilon: ");
		s.append(epsilon);
		s.append("\n\tlambda1: ");
		s.append(lambda1);
		s.append("\n\tlambda2: ");
		s.append(lambda2);
		s.append("\n\tgamma1: ");
		s.append(gamma1);
		s.append("\n\tgamma2: ");
		s.append(gamma2);
		s.append("\n\tHash: ");
		s.append(hash);
		
		return s.toString();
	}
}
