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

import it.unibs.gs.acjt.interfaces.ACJTPrivateKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;


public class ACJTMemberKeyImpl implements ACJTPrivateKey
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2931157374465341045L;
	private BigInteger A, e, x;
	
	public ACJTMemberKeyImpl(BigInteger A, BigInteger e, BigInteger x)
	{
		this.A = A;
		this.e = e;
		this.x = x;
	}
	
	public ACJTMemberKeyImpl(byte[] encoded)
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
		
		this.A = ((DERInteger)s.getObjectAt(0)).getValue();
		this.e = ((DERInteger)s.getObjectAt(1)).getValue();
		this.x = ((DERInteger)s.getObjectAt(2)).getValue();
	}
	
	public BigInteger getA()
	{
		return A;
	}

	public BigInteger getE()
	{
		return e;
	}
	
	public BigInteger getX()
	{
		return x;
	}
	
	public int getSize()
	{
		return A.bitLength() + e.bitLength() + x.bitLength();
	}

	public byte[] getEncoded()
	{
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new DERInteger(A));
		v.add(new DERInteger(e));
		v.add(new DERInteger(x));

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
	
	public String getAlgorithm()
	{
		return "ACJT - Ateniese Camenisch Joye Tsudik Group Signature Scheme";
	}
}
