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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;


public class ACJTSignatureImpl
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3606380613494472186L;
	private BigInteger c, s1, s2, s3, s4, T1, T2, T3;

	
	public ACJTSignatureImpl (BigInteger c, BigInteger s1,
			BigInteger s2, BigInteger s3, BigInteger s4,
			BigInteger T1, BigInteger T2, BigInteger T3)
	{
		this.c = c;
		this.s1 = s1;
		this.s2 = s2;
		this.s3 = s3;
		this.s4 = s4;
		this.T1 = T1;
		this.T2 = T2;
		this.T3 = T3;
	}
	
	public ACJTSignatureImpl(byte[] encoded)
	{
		ASN1InputStream dIn = new ASN1InputStream(new ByteArrayInputStream(encoded));
		
		DERSequence s = null;
		try
		{
			s = (DERSequence)dIn.readObject();
		}
		catch (IOException e)
		{
			throw new SecurityException("can't decode Certificate object");
		}
		
		this.c = ((DERInteger)s.getObjectAt(0)).getValue();
		this.s1 = ((DERInteger)s.getObjectAt(1)).getValue();
		this.s2 = ((DERInteger)s.getObjectAt(2)).getValue();
		this.s3 = ((DERInteger)s.getObjectAt(3)).getValue();
		this.s4 = ((DERInteger)s.getObjectAt(4)).getValue();
		this.T1 = ((DERInteger)s.getObjectAt(5)).getValue();
		this.T2 = ((DERInteger)s.getObjectAt(6)).getValue();
		this.T3 = ((DERInteger)s.getObjectAt(7)).getValue();
	}
	
	public String toString()
	{
		return "ACJT Signature (" + getSize() + "bits)" +
			"\nc (" + c.bitLength() + "bit): "  + c + 
			"\ns1 (" + s1.bitLength() + "bit): " + s1 +
			"\ns2 (" + s2.bitLength() + "bit): " + s2 +
			"\ns3 (" + s3.bitLength() + "bit): " + s3 +
			"\ns4 (" + s4.bitLength() + "bit): " + s4 +
			"\nT1 (" + T1.bitLength() + "bit): " + T1 +
			"\nT2 (" + T2.bitLength() + "bit): " + T2 +
			"\nT3 (" + T3.bitLength() + "bit): " + T3;
			
	}
	
	public int getSize()
	{
		return c.bitLength() + s1.bitLength() + s2.bitLength() + 
			s3.bitLength() + s4.bitLength() + T1.bitLength() + 
			T2.bitLength() + T3.bitLength();
	}

	public BigInteger getC() 
	{
		return c;
	}

	public BigInteger getS1() 
	{
		return s1;
	}

	public BigInteger getS2() 
	{
		return s2;
	}

	public BigInteger getS3() 
	{
		return s3;
	}

	public BigInteger getS4() 
	{
		return s4;
	}

	public BigInteger getT1() 
	{
		return T1;
	}

	public BigInteger getT2() 
	{
		return T2;
	}

	public BigInteger getT3() 
	{
		return T3;
	}

	public String getAlgorithm()
	{
		return "ACJT - Ateniese Camenisch Joye Tsudik Group Signature Scheme";
	}

	public byte[] getEncoded()
	{	
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new DERInteger(c));
		v.add(new DERInteger(s1));
		v.add(new DERInteger(s2));
		v.add(new DERInteger(s3));
		v.add(new DERInteger(s4));
		v.add(new DERInteger(T1));
		v.add(new DERInteger(T2));
		v.add(new DERInteger(T3));

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

	public String getFormat()
	{
		return "ASN.1/DER Format";
	}
}
