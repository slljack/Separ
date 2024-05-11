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

import it.unibs.gs.acjt.interfaces.ACJTPublicKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;


public final class ACJTGroupPublicKeyImpl implements ACJTPublicKey
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7840567709262142139L;

	private BigInteger n, a, a0, g, h, y;

	public ACJTGroupPublicKeyImpl(BigInteger n, BigInteger a,
			BigInteger a0, BigInteger g, BigInteger h,
			BigInteger y)
	{
		this.n = n;
		this.a = a;
		this.a0 = a0;
		this.g = g;
		this.h = h;
		this.y = y;
	}
	
	public ACJTGroupPublicKeyImpl(byte[] encoded)
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
		
		this.n = ((DERInteger)s.getObjectAt(0)).getValue();
		this.a = ((DERInteger)s.getObjectAt(1)).getValue();
		this.a0 = ((DERInteger)s.getObjectAt(2)).getValue();
		this.g = ((DERInteger)s.getObjectAt(3)).getValue();
		this.h = ((DERInteger)s.getObjectAt(4)).getValue();
		this.y = ((DERInteger)s.getObjectAt(5)).getValue();
	}
	

	public BigInteger getN()
	{
		return n;
	}

	public BigInteger getA()
	{
		return a;
	}

	public BigInteger getA0()
	{
		return a0;
	}

	public BigInteger getG()
	{
		return g;
	}

	public BigInteger getH()
	{
		return h;
	}

	public BigInteger getY()
	{
		return y;
	}
	
	public byte[] getEncoded()
	{
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new DERInteger(getN()));
		v.add(new DERInteger(getA()));
		v.add(new DERInteger(getA0()));
		v.add(new DERInteger(getG()));
		v.add(new DERInteger(getH()));
		v.add(new DERInteger(getY()));

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
	
	public int getSize()
	{
		return n.bitLength() + a.bitLength() + a0.bitLength() +
		g.bitLength() + h.bitLength() + y.bitLength();
	}
	
	public String toString()
	{
		return "ACJT Group Public Key (" + getSize() + "bits)" +
				"\nn (" + n.bitLength() + "bit): " + n +
				"\na (" + a.bitLength() + "bit): " + a +
				"\na0 (" + a0.bitLength() + "bit): " + a0 +
				"\ng (" + g.bitLength() + "bit): " + g +
				"\nh (" + h.bitLength() + "bit): " + h +
				"\ny (" + y.bitLength() + "bit): " + y;
	}
	
	public String getAlgorithm()
	{
		return "ACJT - Ateniese Camenisch Joye Tsudik Group Signature Scheme";
	}
	
}
